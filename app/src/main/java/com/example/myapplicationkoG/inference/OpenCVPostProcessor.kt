package com.example.myapplicationkoG.inference

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.RotatedRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * OpenCV-based post-processing for the SAM mask:
 *
 *  1. Find the largest connected contour of the clothing.
 *  2. Run [Imgproc.minAreaRect] to recover the rotation angle and bbox.
 *  3. Rotate the original image by -angle to make the garment upright.
 *  4. Re-find the largest contour in the rotated mask.
 *  5. Crop the rotated garment and center it on a 1080x1080 white canvas,
 *     preserving aspect ratio.
 *
 * Resulting bitmap is what the user sees as the "Design Space" garment.
 */
object OpenCVPostProcessor {

    val canvasSize = 1080
    private val canvasBg = Scalar.all(255.0)

    init {
        if (!OpenCVLoader.initLocal()) {
            // initLocal returns false in tests; the [process] call will still
            // work because the native lib is loaded the first time you call
            // any OpenCV function.
        }
    }

    /**
     * @param original  the photo the user took
     * @param mask      single-channel ARGB bitmap from [ModelInferenceManager.decodeMask]
     */
    fun process(original: Bitmap, mask: Bitmap): Bitmap {
        val originalMat = bitmapToMat(original)
        val maskMat = bitmapToGrayMask(mask)
        try {
            val rotated = deskewGarment(originalMat, maskMat)
            val rotatedMask = deskewGarment(maskMat, maskMat) // same affine

            val largest = largestContour(rotatedMask)
            val cropRect = if (largest != null) {
                contourBoundingRect(largest)
            } else {
                org.opencv.core.Rect(0, 0, rotated.cols(), rotated.rows())
            }
            val cropped = Mat(rotated, cropRect)

            val canvas = Mat(canvasSize, canvasSize, CvType.CV_8UC3, canvasBg)
            val fitted = fitInto(cropped, canvasSize)
            fitted.copyTo(canvas.submat(centerROI(canvas, fitted)))
            fitted.release()
            cropped.release()
            return matToBitmap(canvas)
        } finally {
            originalMat.release()
            maskMat.release()
        }
    }

    // -------------------------------------------------------------------- //
    // Rotation
    // -------------------------------------------------------------------- //

    private fun deskewGarment(image: Mat, mask: Mat): Mat {
        val largestMatOfPoint = largestContour(mask) ?: return image.clone()
        val largest2f = org.opencv.core.MatOfPoint2f().also { largestMatOfPoint.convertTo(it, CvType.CV_32F) }
        val rect = Imgproc.minAreaRect(largest2f)
        val angle = normaliseAngle(rect.angle)
        largest2f.release()

        val w = if (angle < 45.0) rect.size.width else rect.size.height
        val h = if (angle < 45.0) rect.size.height else rect.size.width

        val center = Point(image.cols() / 2.0, image.rows() / 2.0)
        val rotMat = Imgproc.getRotationMatrix2D(center, angle, 1.0)

        val rotated = Mat(image.size(), image.type())
        Imgproc.warpAffine(
            image, rotated, rotMat, image.size(),
            Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(255.0)
        )
        rotMat.release()
        largestMatOfPoint.release()
        return rotated
    }

    private fun normaliseAngle(raw: Double): Double {
        var a = raw
        if (a < -45.0) a += 90.0
        if (a > 45.0) a -= 90.0
        return a
    }

    // -------------------------------------------------------------------- //
    // Contour helpers
    // -------------------------------------------------------------------- //

    private fun largestContour(mask: Mat): MatOfPoint? {
        val binary = Mat()
        Imgproc.threshold(mask, binary, 127.0, 255.0, Imgproc.THRESH_BINARY)
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            binary, contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )
        val best = contours.maxByOrNull { Imgproc.contourArea(it) }
        binary.release()
        return best
    }

    private fun contourBoundingRect(contour: MatOfPoint): org.opencv.core.Rect {
        val box = Imgproc.boundingRect(contour)
        // Pad the box by 4% so the garment isn't glued to the canvas edge.
        val padX = (box.width * 0.04).toInt()
        val padY = (box.height * 0.04).toInt()
        val x = (box.x - padX).coerceAtLeast(0)
        val y = (box.y - padY).coerceAtLeast(0)
        val w = (box.width + padX * 2).coerceAtMost(maskMatWidth() - x)
        val h = (box.height + padY * 2).coerceAtMost(maskMatHeight() - y)
        return org.opencv.core.Rect(x, y, w, h)
    }

    private fun maskMatWidth() = 0
    private fun maskMatHeight() = 0

    // -------------------------------------------------------------------- //
    // Fit into 1080x1080
    // -------------------------------------------------------------------- //

    private fun fitInto(src: Mat, side: Int): Mat {
        val ratio = minOf(
            side.toDouble() / src.cols(),
            side.toDouble() / src.rows()
        )
        val newW = (src.cols() * ratio).toInt().coerceAtLeast(1)
        val newH = (src.rows() * ratio).toInt().coerceAtLeast(1)
        val out = Mat()
        Imgproc.resize(src, out, Size(newW.toDouble(), newH.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
        return out
    }

    private fun centerROI(canvas: Mat, fitted: Mat): org.opencv.core.Rect {
        val x = (canvas.cols() - fitted.cols()) / 2
        val y = (canvas.rows() - fitted.rows()) / 2
        return org.opencv.core.Rect(x, y, fitted.cols(), fitted.rows())
    }

    // -------------------------------------------------------------------- //
    // Bitmap <-> Mat
    // -------------------------------------------------------------------- //

    private fun bitmapToMat(bitmap: Bitmap): Mat {
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

    private fun bitmapToGrayMask(bitmap: Bitmap): Mat {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val mat = Mat(h, w, CvType.CV_8UC1)
        val data = ByteArray(w * h)
        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            data[i] = a.toByte()
        }
        mat.put(0, 0, data)
        return mat
    }

    private fun matToBitmap(mat: Mat): Bitmap {
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
