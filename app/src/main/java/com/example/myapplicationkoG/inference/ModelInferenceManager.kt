package com.example.myapplicationkoG.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/**
 * Loads the three ONNX models from assets and exposes three primitives:
 *
 *  1. [detectClothingBboxes] — YOLOv8 detector; returns bounding boxes for
 *     class id 2 (clothing).
 *  2. [encodeImage] — SAM 2.1 image encoder; returns an embedding + mask
 *     input that the decoder needs.
 *  3. [decodeMask] — SAM 2.1 prompt decoder; given one bounding box, returns
 *     a single-channel mask Bitmap at the original image resolution.
 *
 * All three sessions share a single [OrtEnvironment] which is initialised
 * lazily. The manager is process-singleton: open once from
 * [ClothingInferencePipeline], reuse forever.
 */
class ModelInferenceManager(
    context: Context,
    private val yoloModelAsset: String = "yolo_clothing.onnx",
    private val samEncoderAsset: String = "sam2_hiera_tiny.encoder.onnx",
    private val samDecoderAsset: String = "sam2_hiera_tiny.decoder.onnx",
) {
    private val appContext = context.applicationContext

    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    private val yoloSession: OrtSession by lazy {
        ortEnv.createSession(readAsset(yoloModelAsset), ORT_OPTIONS)
    }
    private val samEncoderSession: OrtSession by lazy {
        ortEnv.createSession(readAsset(samEncoderAsset), ORT_OPTIONS)
    }
    private val samDecoderSession: OrtSession by lazy {
        ortEnv.createSession(readAsset(samDecoderAsset), ORT_OPTIONS)
    }

    fun close() {
        runCatching { yoloSession.close() }
        runCatching { samEncoderSession.close() }
        runCatching { samDecoderSession.close() }
    }

    // -------------------------------------------------------------------- //
    // 1. YOLOv8 detection
    // -------------------------------------------------------------------- //

    /** YOLOv8 input size we resize every image to before detection. */
    private val yoloInputSize = 640

    /**
     * Run YOLOv8 on [bitmap] and return every detected clothing box in the
     * bitmap's native pixel coordinates. Confidence threshold = 0.35.
     */
    fun detectClothingBboxes(bitmap: Bitmap): List<RectF> {
        val resized = Bitmap.createScaledBitmap(bitmap, yoloInputSize, yoloInputSize, true)
        val input = bitmapToNormalizedFloatBuffer(resized) // CHW, 1x3xHxW
        val shape = longArrayOf(1, 3, yoloInputSize.toLong(), yoloInputSize.toLong())
        val tensor = OnnxTensor.createTensor(ortEnv, input, shape)

        val outputs = yoloSession.run(mapOf(yoloSession.inputNames.first() to tensor))
        val raw = (outputs.first().value as Array<*>).first() as Array<FloatArray>
        // YOLOv8 export shape: [1, 4+nc, num_anchors] → transpose to [num_anchors, 4+nc]
        val numAnchors = raw[0].size
        val nc = raw.size - 4
        val transposed = Array(numAnchors) { a -> FloatArray(4 + nc) { c -> raw[c][a] } }

        val scaleX = bitmap.width.toFloat() / yoloInputSize
        val scaleY = bitmap.height.toFloat() / yoloInputSize
        val boxes = mutableListOf<RectF>()
        for (det in transposed) {
            val bestClass = det.drop(4).withIndex().maxBy { it.value }
            if (bestClass.index != CLOTHING_CLASS_ID) continue
            if (bestClass.value < CONFIDENCE_THRESHOLD) continue
            val cx = det[0] * scaleX
            val cy = det[1] * scaleY
            val w = det[2] * scaleX
            val h = det[3] * scaleY
            boxes += RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        }
        if (boxes.isEmpty()) {
            // Fall back to the whole image so SAM still has a prompt.
            boxes += RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        }
        return boxes
    }

    // -------------------------------------------------------------------- //
    // 2. SAM 2.1 encoder
    // -------------------------------------------------------------------- //

    /** SAM 2.1 Hiera-Tiny expects 1024x1024 RGB input. */
    private val samInputSize = 1024

    /** Cached image embedding; invalidated when the image changes. */
    @Volatile private var cachedImageId: Int = 0
    @Volatile private var cachedImageEmbedding: OrtSession.Result? = null

    /**
     * Encode [bitmap] into the SAM image embedding. The embedding is
     * cached on the manager so the decoder can be called multiple times
     * with different boxes without re-running the encoder.
     */
    fun encodeImage(bitmap: Bitmap) {
        if (bitmap.generationId == cachedImageId && cachedImageEmbedding != null) return
        val resized = Bitmap.createScaledBitmap(bitmap, samInputSize, samInputSize, true)
        val input = bitmapToNormalizedFloatBuffer(resized)
        val shape = longArrayOf(1, 3, samInputSize.toLong(), samInputSize.toLong())
        val tensor = OnnxTensor.createTensor(ortEnv, input, shape)
        val outputs = samEncoderSession.run(
            mapOf(samEncoderSession.inputNames.first() to tensor)
        )
        cachedImageId = bitmap.generationId
        cachedImageEmbedding = outputs
    }

    // -------------------------------------------------------------------- //
    // 3. SAM 2.1 decoder
    // -------------------------------------------------------------------- //

    /**
     * Run the SAM decoder with [box] (in original-image coordinates) and
     * return a single-channel mask bitmap at the original image's
     * resolution. White = clothing, black = background.
     */
    fun decodeMask(bitmap: Bitmap, box: RectF): Bitmap {
        encodeImage(bitmap)
        val embeddingOutputs = cachedImageEmbedding
            ?: error("SAM encoder produced no output")

        // Box prompt: [1, 1, 4] (xyxy) in pixels relative to the 1024x1024 input.
        val scale = samInputSize.toFloat() / maxOf(bitmap.width, bitmap.height)
        val boxXyxy = floatArrayOf(
            box.left * scale,
            box.top * scale,
            box.right * scale,
            box.bottom * scale,
        )
        val boxShape = longArrayOf(1, 1, 4)
        val boxTensor = OnnxTensor.createTensor(
            ortEnv, FloatBuffer.wrap(boxXyxy), boxShape
        )

        // We also feed a no-op point prompt (origin) so the decoder
        // signature matches the standard SAM 2.1 export.
        val pointCoords = floatArrayOf(0f, 0f, 0f)
        val pointShape = longArrayOf(1, 1, 3)
        val pointTensor = OnnxTensor.createTensor(
            ortEnv, FloatBuffer.wrap(pointCoords), pointShape
        )
        val pointLabels = floatArrayOf(-1f) // -1 = pad, i.e. "no point"
        val labelTensor = OnnxTensor.createTensor(
            ortEnv, FloatBuffer.wrap(pointLabels), longArrayOf(1, 1)
        )

        val maskShape = longArrayOf(1, 1, samInputSize.toLong(), samInputSize.toLong())
        val maskTensor = OnnxTensor.createTensor(ortEnv, FloatBuffer.allocate(0), maskShape)

        val inputs = buildMap<String, OnnxTensor> {
            put(samDecoderSession.inputNames.first(), embeddingOutputs.first() as OnnxTensor)
            // Other input names vary by export; the next two slots are
            // the standard "point_coords" / "point_labels" pair.
            val names: List<String> = samDecoderSession.inputNames.toList()
            if (names.size > 1) put(names[1], pointTensor)
            if (names.size > 2) put(names[2], labelTensor)
            if (names.size > 3) put(names[3], boxTensor)
        }
        val outputs = samDecoderSession.run(inputs)
        val lowResMask = outputs.first().value as Array<Array<Array<FloatArray>>> // [1,1,H,W]
        return resampleMaskToBitmap(lowResMask[0][0], bitmap.width, bitmap.height)
    }

    // -------------------------------------------------------------------- //
    // Helpers
    // -------------------------------------------------------------------- //

    private fun readAsset(name: String): ByteArray =
        appContext.assets.open(name).use { it.readBytes() }

    /**
     * Convert a Bitmap into a CHW float tensor normalised to the range the
     * YOLO / SAM exports expect (mean 0, std 1; YOLO uses 0..1, SAM uses
     * the same mean/std). We deliberately use 0..1 here — both
     * Ultralytics YOLOv8 and SAM 2.1 hiera tiny accept that range when
     * exported with the default preprocessing.
     */
    private fun bitmapToNormalizedFloatBuffer(bitmap: Bitmap): FloatBuffer {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = FloatBuffer.allocate(3 * w * h)
        val r = FloatArray(w * h)
        val g = FloatArray(w * h)
        val b = FloatArray(w * h)
        for (i in pixels.indices) {
            val c = pixels[i]
            r[i] = ((c shr 16) and 0xFF) / 255f
            g[i] = ((c shr 8) and 0xFF) / 255f
            b[i] = (c and 0xFF) / 255f
        }
        out.put(r); out.put(g); out.put(b)
        out.rewind()
        return out
    }

    /**
     * Bilinearly resample a low-resolution HxW float mask into a
     * single-channel ARGB bitmap at the target resolution.
     */
    private fun resampleMaskToBitmap(
        mask: Array<FloatArray>,
        outW: Int,
        outH: Int,
    ): Bitmap {
        val srcH = mask.size
        val srcW = mask[0].size
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(outW * outH)
        for (y in 0 until outH) {
            val sy = y.toFloat() * srcH / outH
            val y0 = sy.toInt().coerceIn(0, srcH - 1)
            val y1 = (y0 + 1).coerceAtMost(srcH - 1)
            val dy = sy - y0
            for (x in 0 until outW) {
                val sx = x.toFloat() * srcW / outW
                val x0 = sx.toInt().coerceIn(0, srcW - 1)
                val x1 = (x0 + 1).coerceAtMost(srcW - 1)
                val dx = sx - x0
                val v00 = mask[y0][x0]
                val v01 = mask[y0][x1]
                val v10 = mask[y1][x0]
                val v11 = mask[y1][x1]
                val v = (1 - dy) * ((1 - dx) * v00 + dx * v01) +
                    dy * ((1 - dx) * v10 + dx * v11)
                val on = if (v > 0f) 0xFF else 0x00
                pixels[y * outW + x] = (on shl 24) or 0x00FFFFFF
            }
        }
        out.setPixels(pixels, 0, outW, 0, 0, outW, outH)
        return out
    }

    private companion object {
        const val CLOTHING_CLASS_ID = 2
        const val CONFIDENCE_THRESHOLD = 0.35f
        val ORT_OPTIONS = OrtSession.SessionOptions().apply {
            // Use all CPU cores; switch to NNAPI EP later if you add an
            // `OrtEnvironment.getEnvironment().createSession(... NNAPI)` overload.
            setIntraOpNumThreads(Runtime.getRuntime().availableProcessors())
        }
    }
}
