package com.example.myapplicationkoG.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.File

/**
 * Result of the on-device cutout pipeline for a single garment photo.
 * Only the final cutout bitmap matters for now.
 */
data class InferenceResult(
    val cutout: Bitmap,
)

/**
 * Pipeline:
 *   photo file → decode → YOLO bbox → SAM mask → cut out + center on 1080x1080
 *
 * Runs on Dispatchers.IO. No deskew, no contour math — just the AI
 * cutout blended onto a white canvas.
 */
class ClothingInferencePipeline(context: Context) {

    private val appContext = context.applicationContext
    private val models = ModelInferenceManager(appContext)

    suspend fun run(file: File): InferenceResult {
        val sourceBitmap = decodeScaled(file, maxEdge = 1280)
        val bboxes = models.detectClothingBboxes(sourceBitmap)
        val bestBox = bboxes.maxByOrNull { it.width() * it.height() }
            ?: error("No clothing detected — try a clearer photo with the garment filling most of the frame.")
        val mask = models.decodeMask(sourceBitmap, bestBox)
        val cutout = cutoutOntoCanvas(sourceBitmap, mask, canvasSize = 1080)
        return InferenceResult(cutout = cutout)
    }

    private fun decodeScaled(file: File, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val (w, h) = bounds.outWidth to bounds.outHeight
        if (w <= 0 || h <= 0) error("Could not decode image: ${file.absolutePath}")

        var sample = 1
        while (w / sample > maxEdge || h / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: error("Could not decode image: ${file.absolutePath}")
        val longEdge = maxOf(bmp.width, bmp.height)
        return if (longEdge > maxEdge) {
            val scale = maxEdge.toFloat() / longEdge
            Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).toInt(),
                (bmp.height * scale).toInt(),
                true,
            )
        } else bmp
    }

    /**
     * Use the SAM mask to cut the garment out of the original photo and
     * center-fit it on a 1080x1080 white canvas.
     */
    private fun cutoutOntoCanvas(
        source: Bitmap,
        maskArgb: Bitmap,
        canvasSize: Int,
    ): Bitmap {
        // 1) Make a transparent-background cutout of the source bitmap
        //    using the SAM mask as the alpha channel.
        val srcW = source.width
        val srcH = source.height
        val maskResized = if (maskArgb.width != srcW || maskArgb.height != srcH) {
            Bitmap.createScaledBitmap(maskArgb, srcW, srcH, true)
        } else maskArgb

        val cut = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888)
        val srcPx = IntArray(srcW * srcH)
        val maskPx = IntArray(srcW * srcH)
        source.getPixels(srcPx, 0, srcW, 0, 0, srcW, srcH)
        maskResized.getPixels(maskPx, 0, srcW, 0, 0, srcW, srcH)
        for (i in srcPx.indices) {
            val alpha = (maskPx[i] ushr 24) and 0xFF
            srcPx[i] = (alpha shl 24) or (srcPx[i] and 0x00FFFFFF)
        }
        cut.setPixels(srcPx, 0, srcW, 0, 0, srcW, srcH)

        // 2) Center-fit the cutout on a 1080x1080 white canvas via OpenCV.
        val srcMat = bitmapToBgrMat(cut)
        val canvasMat = Mat(canvasSize, canvasSize, CvType.CV_8UC3, Scalar.all(255.0))
        try {
            val ratio = minOf(
                canvasSize.toDouble() / srcMat.cols(),
                canvasSize.toDouble() / srcMat.rows(),
            ).coerceAtLeast(1e-6)
            val newW = (srcMat.cols() * ratio).toInt().coerceAtLeast(1)
            val newH = (srcMat.rows() * ratio).toInt().coerceAtLeast(1)
            val fitted = Mat()
            Imgproc.resize(srcMat, fitted, org.opencv.core.Size(newW.toDouble(), newH.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
            try {
                val x = (canvasSize - newW) / 2
                val y = (canvasSize - newH) / 2
                fitted.copyTo(canvasMat.submat(y, y + newH, x, x + newW))
            } finally {
                fitted.release()
            }
            return bgrMatToBitmap(canvasMat)
        } finally {
            srcMat.release()
            canvasMat.release()
            if (maskResized !== maskArgb) maskResized.recycle()
        }
    }

    private fun bitmapToBgrMat(bitmap: Bitmap): Mat {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val mat = Mat(h, w, CvType.CV_8UC3)
        val data = ByteArray(w * h * 3)
        var i = 0
        for (p in pixels) {
            data[i++] = ((p shr 16) and 0xFF).toByte() // B
            data[i++] = ((p shr 8) and 0xFF).toByte()  // G
            data[i++] = (p and 0xFF).toByte()          // R
        }
        mat.put(0, 0, data)
        return mat
    }

    private fun bgrMatToBitmap(mat: Mat): Bitmap {
        val w = mat.cols()
        val h = mat.rows()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val data = ByteArray(w * h * 3)
        mat.get(0, 0, data)
        val pixels = IntArray(w * h)
        var i = 0
        for (p in pixels.indices) {
            val b = data[i++].toInt() and 0xFF
            val g = data[i++].toInt() and 0xFF
            val r = data[i++].toInt() and 0xFF
            pixels[p] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}