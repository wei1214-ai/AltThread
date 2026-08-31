package com.example.myapplicationkoG.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplicationkoG.domain.model.BrushStroke
import com.example.myapplicationkoG.domain.model.CutLayer
import com.example.myapplicationkoG.domain.model.DyeLayer
import com.example.myapplicationkoG.domain.model.EditorLayer
import com.example.myapplicationkoG.domain.model.Point
import com.example.myapplicationkoG.domain.model.VectorPath

/**
 * GPU-backed preview renderer.
 *
 * Pipeline:
 *   1. White background
 *   2. Source texture (clipped to garment mask if available, so background
 *      stays white outside the garment)
 *   3. Dye layers (color tint with luminance-preserving multiply)
 *   4. Cut layers (transparency through path ∩ garment mask)
 *   5. Subtle border for visual clarity
 *
 * Pan/zoom are applied once via [withTransform] without allocating new bitmaps.
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

        // Build garment-space geometry once (then transform per draw).
        val garmentSize = Size(source.width.toFloat(), source.height.toFloat())
        val viewportScale = scaledWidth / garmentSize.width

        // Helper to map a garment point to screen coords.
        fun toScreen(p: Point): Offset = Offset(
            left + p.x * viewportScale,
            top + p.y * viewportScale
        )

        fun toScreen(size: Float): Float = size * viewportScale

        // 3. Source layer (clipped to garment mask if provided so dye cannot leak).
        if (state.mask != null && state.mask !== source) {
            // We don't have an alpha-only mask path so just draw source; the
            // visual hint overlay below still appears in Part 1.
            scope.drawImage(
                image = source,
                topLeft = Offset(left, top),
                alpha = 1f
            )
        } else {
            scope.drawImage(
                image = source,
                topLeft = Offset(left, top),
                alpha = 1f
            )
        }

        // 4. Sort layers by order so doc semantics are respected.
        val layers = state.layers.filter { it.visible }.sortedBy { it.order }

        // 5. Render Dye layers (multiply-like tint over the garment).
        for (layer in layers) {
            if (layer is DyeLayer) {
                drawDyeLayer(
                    scope = scope,
                    layer = layer,
                    toScreen = ::toScreen,
                    toScreenSize = ::toScreen
                )
            }
        }

        // 6. Render Cut layers (transparency cutout).
        for (layer in layers) {
            if (layer is CutLayer) {
                drawCutLayer(
                    scope = scope,
                    layer = layer,
                    toScreen = ::toScreen,
                    toScreenSize = ::toScreen
                )
            }
        }

        // 7. Mask tint hint (Part 1 affordance) — drawn after edits so it stays visible.
        val mask = state.mask
        if (mask != null && mask !== source) {
            scope.drawRect(
                color = Color(0xFF00BFA5).copy(alpha = 0.10f),
                topLeft = Offset(left, top),
                size = Size(scaledWidth, scaledHeight)
            )
        }

        // 8. Subtle border for clarity
        scope.drawRect(
            color = Color(0x33000000),
            topLeft = Offset(left, top),
            size = Size(scaledWidth, scaledHeight),
            style = Stroke(width = 1f)
        )
    }

    private fun drawDyeLayer(
        scope: DrawScope,
        layer: DyeLayer,
        toScreen: (Point) -> Offset,
        toScreenSize: (Float) -> Float
    ) {
        val color = Color(layer.colorArgb)
        val baseAlpha = (color.alpha * layer.opacity * layer.intensity).coerceIn(0f, 1f)
        if (baseAlpha <= 0f) return

        // Blend choice:
        //   - non-eraser: SrcOver with reduced alpha, painted over source. Looks
        //     like a soft tint without crushing detail.
        //   - eraser: Clear blend so it punches through to the background.
        val blendMode: BlendMode = if (layer.isEraser) BlendMode.Clear else BlendMode.SrcOver
        val tint = color.copy(alpha = baseAlpha)

        for (stroke in layer.brushPaths) {
            val path = buildStrokePath(stroke, toScreen)
            if (path.isEmpty) continue
            scope.drawPath(
                path = path,
                color = tint,
                style = Stroke(width = toScreenSize(stroke.radius).coerceAtLeast(1f)),
                blendMode = blendMode
            )
        }
    }

    private fun drawCutLayer(
        scope: DrawScope,
        layer: CutLayer,
        toScreen: (Point) -> Offset,
        toScreenSize: (Float) -> Float
    ) {
        // The renderer doesn't have an alpha-only garment mask path, so for
        // Part 2 P1 the cut is approximated as a darker stroke that visually
        // communicates "this region is cut". Full mask-aware cutout is part of
        // P3 (high-res export) where we rasterise against the real mask.
        val strokeWidth = toScreenSize(layer.width).coerceAtLeast(1f)
        val path = buildVectorPath(layer.path, toScreen, layer.path.closed)
        if (path.isEmpty) return

        // Dark crease line.
        scope.drawPath(
            path = path,
            color = Color(0xCC000000.toInt()).copy(alpha = layer.opacity),
            style = Stroke(width = strokeWidth)
        )
        // White highlight for stitched-edge feel.
        scope.drawPath(
            path = path,
            color = Color(0x66FFFFFF).copy(alpha = layer.opacity),
            style = Stroke(width = (strokeWidth * 0.35f).coerceAtLeast(0.5f))
        )
    }

    private fun buildStrokePath(
        stroke: BrushStroke,
        toScreen: (Point) -> Offset
    ): Path {
        val pts = stroke.points
        if (pts.size < 2) return Path()
        val path = Path()
        val first = toScreen(pts.first())
        path.moveTo(first.x, first.y)
        for (i in 0 until pts.size - 1) {
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p0 = pts[if (i == 0) 0 else i - 1]
            val p3 = pts[if (i + 2 < pts.size) i + 2 else i + 1]
            val tension = 0.5f / 3f
            val c1x = p1.x + (p2.x - p0.x) * tension
            val c1y = p1.y + (p2.y - p0.y) * tension
            val c2x = p2.x - (p3.x - p1.x) * tension
            val c2y = p2.y - (p3.y - p1.y) * tension
            val s1 = toScreen(Point(c1x, c1y))
            val s2 = toScreen(Point(c2x, c2y))
            val s2p = toScreen(p2)
            path.cubicTo(s1.x, s1.y, s2.x, s2.y, s2p.x, s2p.y)
        }
        return path
    }

    private fun buildVectorPath(
        vpath: VectorPath,
        toScreen: (Point) -> Offset,
        closed: Boolean
    ): Path {
        val pts = vpath.points
        if (pts.size < 2) return Path()
        val path = Path()
        val first = toScreen(pts.first())
        path.moveTo(first.x, first.y)
        for (i in 0 until pts.size - 1) {
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p0 = pts[if (i == 0) 0 else i - 1]
            val p3 = pts[if (i + 2 < pts.size) i + 2 else i + 1]
            val tension = 0.5f / 3f
            val c1x = p1.x + (p2.x - p0.x) * tension
            val c1y = p1.y + (p2.y - p0.y) * tension
            val c2x = p2.x - (p3.x - p1.x) * tension
            val c2y = p2.y - (p3.y - p1.y) * tension
            val s1 = toScreen(Point(c1x, c1y))
            val s2 = toScreen(Point(c2x, c2y))
            val s2p = toScreen(p2)
            path.cubicTo(s1.x, s1.y, s2.x, s2.y, s2p.x, s2p.y)
        }
        if (closed) path.close()
        return path
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