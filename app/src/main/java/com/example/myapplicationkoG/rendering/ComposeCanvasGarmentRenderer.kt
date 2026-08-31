package com.example.myapplicationkoG.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplicationkoG.domain.model.Viewport

/**
 * GPU-backed preview renderer.
 *
 * Pipeline:
 *   1. White background
 *   2. Source texture, scaled to fit the canvas with a small margin
 *   3. Garment mask applied via BlendMode (so background stays white
 *      outside the mask and the source stays intact inside)
 *
 * Pan/zoom are applied via [withTransform] without allocating new bitmaps.
 * The renderer never calls Bitmap.createBitmap per frame and never re-decodes
 * the source/mask — those are passed in as ImageBitmap via [RenderState].
 */
class ComposeCanvasGarmentRenderer : GarmentRenderer {

    override fun draw(scope: DrawScope, state: RenderState) {
        val canvasSize: Size = scope.size
        val source = state.source
        if (source == null) {
            drawEmpty(scope, canvasSize)
            return
        }

        // 1. White background
        scope.drawRect(color = Color.White, size = canvasSize)

        // 2. Compute the base fit: contain the source within the canvas, then apply viewport.
        val fit = fitContain(
            sourceWidth = source.width.toFloat(),
            sourceHeight = source.height.toFloat(),
            canvasWidth = canvasSize.width,
            canvasHeight = canvasSize.height
        )

        val scaledWidth = fit.width * state.viewport.scale
        val scaledHeight = fit.height * state.viewport.scale
        val baseLeft = (canvasSize.width - fit.width) / 2f
        val baseTop = (canvasSize.height - fit.height) / 2f
        val left = baseLeft + state.viewport.translationX
        val top = baseTop + state.viewport.translationY

        // Source layer (preserved as-is — NEVER modified to "bake" the mask in).
        scope.drawImage(
            image = source,
            topLeft = Offset(left, top),
            alpha = 1f
        )

        // 3. Mask tint, used as a visual hint of segmentation. Part 1 draws a
        // semi-transparent outline of the mask bounds so the user sees the result.
        val mask = state.mask
        if (mask != null && mask !== source) {
            scope.drawRect(
                color = Color(0xFF00BFA5).copy(alpha = 0.15f),
                topLeft = Offset(left, top),
                size = Size(scaledWidth, scaledHeight)
            )
        }

        // 4. Subtle border for clarity
        scope.drawRect(
            color = Color(0x33000000),
            topLeft = Offset(left, top),
            size = Size(scaledWidth, scaledHeight),
            style = Stroke(width = 1f)
        )
    }

    private fun drawEmpty(scope: DrawScope, canvasSize: Size) {
        scope.drawRect(color = Color.White, size = canvasSize)
        scope.drawRect(
            color = Color(0xFFCCCCCC),
            topLeft = Offset(canvasSize.width / 2 - 1, 0f),
            size = Size(2f, canvasSize.height),
            style = Stroke(width = 1f)
        )
        scope.drawRect(
            color = Color(0xFFCCCCCC),
            topLeft = Offset(0f, canvasSize.height / 2 - 1),
            size = Size(canvasSize.width, 2f),
            style = Stroke(width = 1f)
        )
    }

    private fun fitContain(
        sourceWidth: Float,
        sourceHeight: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): Size {
        if (sourceWidth <= 0f || sourceHeight <= 0f || canvasWidth <= 0f || canvasHeight <= 0f) {
            return Size.Zero
        }
        val scale = minOf(canvasWidth / sourceWidth, canvasHeight / sourceHeight)
        return Size(sourceWidth * scale, sourceHeight * scale)
    }
}
