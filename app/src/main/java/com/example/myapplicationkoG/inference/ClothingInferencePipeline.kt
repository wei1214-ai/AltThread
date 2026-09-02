package com.example.myapplicationkoG.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Result of running the on-device AI inference pipeline on a single
 * garment photo.
 *
 * @param sourceUri The Uri passed in by the caller.
 * @param sourceBitmap Decoded, downscaled bitmap used for inference.
 * @param mask Binary (single-channel) mask produced by SAM 2.1 decoder.
 *   Pixel 0 = background, 255 = clothing. Same dimensions as [sourceBitmap].
 * @param designSpace The garment after OpenCV deskew, rotation, and center-
 *   placing on a 1080x1080 pure-white canvas — what the user sees first in
 *   the editor.
 */
data class InferenceResult(
    val sourceUri: Uri,
    val sourceBitmap: Bitmap,
    val mask: Bitmap,
    val designSpace: Bitmap,
)

/**
 * Single entry point used by the Editor (via [EditorViewModel]) to turn a
 * user photo into a fully processed side ready for the Design Space.
 *
 * Pipeline:
 *   photo (Uri) → decode → downscale → YOLO bbox → SAM mask
 *   → OpenCV deskew + center on 1080x1080 → InferenceResult
 */
class ClothingInferencePipeline(context: Context) {

    private val appContext = context.applicationContext
    private val models = ModelInferenceManager(appContext)

    suspend fun run(uri: Uri): InferenceResult = withContext(Dispatchers.IO) {
        val sourceBitmap = decodeScaled(uri, maxEdge = 1280)
        val bboxes = models.detectClothingBboxes(sourceBitmap)
        val bestBox = bboxes.maxByOrNull { it.width() * it.height() }
            ?: error("No clothing detected by YOLO — try a clearer photo with the garment filling most of the frame.")
        val mask = models.decodeMask(sourceBitmap, bestBox)
        val designSpace = OpenCVPostProcessor.process(sourceBitmap, mask)

        InferenceResult(
            sourceUri = uri,
            sourceBitmap = sourceBitmap,
            mask = mask,
            designSpace = designSpace,
        )
    }

    private fun decodeScaled(uri: Uri, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: error("Could not open image at $uri")

        val (w, h) = bounds.outWidth to bounds.outHeight
        var sample = 1
        while (w / sample > maxEdge || h / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = appContext.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: error("Could not decode image at $uri")
        val longEdge = max(bmp.width, bmp.height)
        return if (longEdge > maxEdge) {
            val scale = maxEdge.toFloat() / longEdge
            bmp.scale((bmp.width * scale).toInt(), (bmp.height * scale).toInt())
        } else bmp
    }
}
