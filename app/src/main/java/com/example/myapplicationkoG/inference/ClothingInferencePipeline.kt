package com.example.myapplicationkoG.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import com.example.myapplicationkoG.domain.model.GarmentSide
import com.example.myapplicationkoG.domain.model.ImageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Single entry point used by [GarmentInputViewModel] to turn a user
 * photo into a fully processed `GarmentSide` ready for the editor.
 *
 * Pipeline:
 *   photo (Uri) → decode → downscale → YOLO bbox → SAM mask
 *   → OpenCV deskew + center on 1080x1080 → GarmentSide
 */
class ClothingInferencePipeline(context: Context) {

    private val appContext = context.applicationContext
    private val models = ModelInferenceManager(appContext)

    suspend fun run(uri: Uri): GarmentSide = withContext(Dispatchers.IO) {
        val sourceBitmap = decodeScaled(uri, maxEdge = 1280)
        val bboxes = models.detectClothingBboxes(sourceBitmap)
        val bestBox = bboxes.maxBy { it.width() * it.height() }
        val mask = models.decodeMask(sourceBitmap, bestBox)
        val designSpace = OpenCVPostProcessor.process(sourceBitmap, mask)

        GarmentSide(
            sourceImage = ImageRef(uri = uri.toString()),
            designSpace = designSpace, // kept in memory; persisted later as PNG path
            designSpacePath = null,
            mask = mask,
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
