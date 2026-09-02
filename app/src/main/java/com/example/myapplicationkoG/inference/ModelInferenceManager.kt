package com.example.myapplicationkoG.inference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.Collections

/**
 * Loads the three ONNX models from assets and exposes three primitives:
 * 1. detectClothingBboxes — YOLOv8
 * 2. encodeImage — SAM 2.1 encoder
 * 3. decodeMask — SAM 2.1 decoder
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
        ortEnv.createSession(getModelFilePath(yoloModelAsset), ORT_OPTIONS)
    }
    private val samEncoderSession: OrtSession by lazy {
        ortEnv.createSession(getModelFilePath(samEncoderAsset), ORT_OPTIONS)
    }
    private val samDecoderSession: OrtSession by lazy {
        ortEnv.createSession(getModelFilePath(samDecoderAsset), ORT_OPTIONS)
    }

    fun close() {
        runCatching { yoloSession.close() }
        runCatching { samEncoderSession.close() }
        runCatching { samDecoderSession.close() }
        synchronized(this) {
            cachedImageEmbedding?.close()
            cachedImageEmbedding = null
        }
    }

    private val yoloInputSize = 640
    private val samInputSize = 1024

    @Volatile private var cachedImageId: Int = 0
    @Volatile private var cachedW: Int = -1
    @Volatile private var cachedH: Int = -1
    @Volatile private var cachedImageEmbedding: OrtSession.Result? = null

    fun detectClothingBboxes(bitmap: Bitmap): List<RectF> {
        val resized = Bitmap.createScaledBitmap(bitmap, yoloInputSize, yoloInputSize, true)
        val input = bitmapToNormalizedFloatBuffer(resized)
        val shape = longArrayOf(1, 3, yoloInputSize.toLong(), yoloInputSize.toLong())
        var tensor: OnnxTensor? = null
        var outputs: OrtSession.Result? = null
        try {
            tensor = OnnxTensor.createTensor(ortEnv, input, shape)
            val inputName = yoloSession.inputNames.firstOrNull() ?: "images"
            outputs = yoloSession.run(Collections.singletonMap(inputName, tensor))
            val firstValue = outputs.get(0).getValue()
            val raw = extractYoloRaw(firstValue)
            if (raw.isEmpty()) return emptyList()
            val dim = raw[0].size
            val nc = dim - 4
            if (nc < 0) return emptyList()
            val scaleX = bitmap.width.toFloat() / yoloInputSize
            val scaleY = bitmap.height.toFloat() / yoloInputSize
            val boxes = mutableListOf<RectF>()
            for (det in raw) {
                if (det.size < 4) continue
                var bestIdx = 0
                var bestScore = Float.NEGATIVE_INFINITY
                for (c in 0 until nc) {
                    if (4 + c >= det.size) break
                    val s = det[4 + c]
                    if (s > bestScore) {
                        bestScore = s
                        bestIdx = c
                    }
                }
                if (bestIdx != CLOTHING_CLASS_ID) continue
                if (bestScore < CONFIDENCE_THRESHOLD) continue
                val cx = det[0] * scaleX
                val cy = det[1] * scaleY
                val w = det[2] * scaleX
                val h = det[3] * scaleY
                val left = (cx - w / 2f).coerceIn(0f, bitmap.width.toFloat())
                val top = (cy - h / 2f).coerceIn(0f, bitmap.height.toFloat())
                val right = (cx + w / 2f).coerceIn(0f, bitmap.width.toFloat())
                val bottom = (cy + h / 2f).coerceIn(0f, bitmap.height.toFloat())
                if (right > left && bottom > top) boxes.add(RectF(left, top, right, bottom))
            }
            // No fallback: caller will show invalid message if empty
            return boxes
        } finally {
            runCatching { tensor?.close() }
            runCatching { outputs?.close() }
            if (resized !== bitmap) runCatching { resized.recycle() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractYoloRaw(firstValue: Any?): Array<FloatArray> {
        return try {
            if (firstValue == null) return emptyArray()
            if (firstValue !is Array<*>) return emptyArray()
            val batch = firstValue as Array<*>
            if (batch.isEmpty()) return emptyArray()
            val first = batch[0] ?: return emptyArray()
            if (first !is Array<*>) return emptyArray()
            val arr2 = first as Array<FloatArray>
            if (arr2.isEmpty()) return emptyArray()
            val d0 = arr2.size
            val d1 = arr2[0].size
            if (d0 <= 20 && d1 >= 100) {
                val numAnchors = d1
                val channels = d0
                Array(numAnchors) { a -> FloatArray(channels) { c -> arr2[c][a] } }
            } else {
                arr2
            }
        } catch (t: Throwable) {
            Log.w("ModelInference", "extractYoloRaw failed: ${t.message}", t)
            emptyArray()
        }
    }

    @Synchronized
    fun encodeImage(bitmap: Bitmap) {
        if (bitmap.generationId == cachedImageId && bitmap.width == cachedW && bitmap.height == cachedH && cachedImageEmbedding != null) return
        cachedImageEmbedding?.let { runCatching { it.close() } }
        cachedImageEmbedding = null
        val resized = Bitmap.createScaledBitmap(bitmap, samInputSize, samInputSize, true)
        var tensor: OnnxTensor? = null
        try {
            val input = bitmapToNormalizedFloatBuffer(resized)
            val shape = longArrayOf(1, 3, samInputSize.toLong(), samInputSize.toLong())
            tensor = OnnxTensor.createTensor(ortEnv, input, shape)
            val inputName = samEncoderSession.inputNames.firstOrNull() ?: "image"
            val outputs = samEncoderSession.run(Collections.singletonMap(inputName, tensor))
            cachedImageId = bitmap.generationId
            cachedW = bitmap.width
            cachedH = bitmap.height
            cachedImageEmbedding = outputs
            // Log all encoder outputs shapes for 16KB fix diagnosis
            for (i in 0 until outputs.size()) {
                val v = outputs.get(i)
                val info = v.info
                val shp = (info as? ai.onnxruntime.TensorInfo)?.shape
                Log.d("ModelInference", "SAM encoder output $i shape=${shp?.joinToString()} name=${try{outputs.iterator().asSequence().toList()[i]}catch(_:Throwable){i}}")
            }
            Log.d("ModelInference", "SAM encoder cached, inputs=${samEncoderSession.inputNames} outputs=${outputs.size()}")
        } finally {
            runCatching { tensor?.close() }
            if (resized !== bitmap) runCatching { resized.recycle() }
        }
    }

    private fun findImageEmbedding(result: OrtSession.Result): OnnxTensor {
        // Prefer exact [1,256,64,64] expected by decoder (Got: Expected 256,64,64)
        for (i in 0 until result.size()) {
            val v = result.get(i) as? OnnxTensor ?: continue
            val info = v.info as? ai.onnxruntime.TensorInfo ?: continue
            val shp = info.shape
            if (shp.size == 4 && shp[1] == 256L && shp[2] == 64L && shp[3] == 64L) {
                Log.d("ModelInference", "Selected encoder output $i as image_embeddings shape=${shp.joinToString()}")
                return v
            }
        }
        // Fallback: any with 256 channels
        for (i in 0 until result.size()) {
            val v = result.get(i) as? OnnxTensor ?: continue
            val info = v.info as? ai.onnxruntime.TensorInfo ?: continue
            val shp = info.shape
            if (shp.size == 4 && shp[1] == 256L) {
                Log.w("ModelInference", "Fallback embedding output $i shape=${shp.joinToString()}")
                return v
            }
        }
        // Last resort: first output
        Log.w("ModelInference", "No 256-channel output found, fallback to get(0)")
        return result.get(0) as OnnxTensor
    }

    private fun findHighResFeat(result: OrtSession.Result, decoderName: String): OnnxTensor? {
        val lower = decoderName.lowercase()
        // Expected shapes for SAM2 Hiera Tiny
        // feats_0: [1,32,256,256], feats_1: [1,64,128,128]
        val want0 = lower.contains("0")
        val want1 = lower.contains("1")
        // Collect candidates
        val candidates = mutableListOf<Pair<Int, LongArray>>()
        for (i in 0 until result.size()) {
            val v = result.get(i) as? OnnxTensor ?: continue
            val info = v.info as? ai.onnxruntime.TensorInfo ?: continue
            val shp = info.shape
            if (shp.size == 4) candidates.add(i to shp)
        }
        // Try exact match
        for ((idx, shp) in candidates) {
            if (want0 && shp[1] == 32L && shp[2] == 256L && shp[3] == 256L) return result.get(idx) as OnnxTensor
            if (want1 && shp[1] == 64L && shp[2] == 128L && shp[3] == 128L) return result.get(idx) as OnnxTensor
        }
        // Fallback by channel
        for ((idx, shp) in candidates) {
            if (want0 && shp[1] == 32L) return result.get(idx) as OnnxTensor
            if (want1 && shp[1] == 64L) return result.get(idx) as OnnxTensor
        }
        // Any high-res (not the 256 embedding)
        for ((idx, shp) in candidates) {
            if (shp[1] != 256L && shp[2] >= 128) return result.get(idx) as OnnxTensor
        }
        return null
    }

    fun decodeMask(bitmap: Bitmap, box: RectF): Bitmap {
        encodeImage(bitmap)
        val embeddingOutputs = synchronized(this) { cachedImageEmbedding }
            ?: error("SAM encoder produced no output")
        val embeddingTensor = findImageEmbedding(embeddingOutputs)

        val scaleX = samInputSize.toFloat() / bitmap.width.toFloat()
        val scaleY = samInputSize.toFloat() / bitmap.height.toFloat()
        val x1 = (box.left * scaleX).coerceIn(0f, samInputSize.toFloat())
        val y1 = (box.top * scaleY).coerceIn(0f, samInputSize.toFloat())
        val x2 = (box.right * scaleX).coerceIn(0f, samInputSize.toFloat())
        val y2 = (box.bottom * scaleY).coerceIn(0f, samInputSize.toFloat())

        val inputNames = samDecoderSession.inputNames.toList()
        Log.d("ModelInference", "decodeMask orig=$box scaled=[$x1,$y1,$x2,$y2] decoderInputs=$inputNames")

        val toClose = mutableListOf<OnnxTensor>()
        var outputs: OrtSession.Result? = null
        try {
            val decoderInputs = mutableMapOf<String, OnnxTensor>()

            fun findName(vararg keywords: String): String? {
                for (n in inputNames) {
                    val lower = n.lowercase()
                    var ok = true
                    for (k in keywords) if (!lower.contains(k)) { ok = false; break }
                    if (ok) return n
                }
                return null
            }

            // image_embeddings
            val embedName = findName("image", "embed") ?: findName("embed") ?: findName("image") ?: inputNames.firstOrNull()
            if (embedName != null) decoderInputs[embedName] = embeddingTensor

            // point_coords  -> [1,2,2]
            val pointCoordsName = findName("point", "coord") ?: findName("coord")
            if (pointCoordsName != null) {
                val data = floatArrayOf(x1, y1, x2, y2)
                val t = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(data), longArrayOf(1, 2, 2))
                toClose.add(t)
                decoderInputs[pointCoordsName] = t
            }

            // point_labels -> [1,2] with 2,3
            val pointLabelsName = findName("point", "label") ?: findName("label")
            if (pointLabelsName != null) {
                val data = floatArrayOf(2f, 3f)
                val t = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(data), longArrayOf(1, 2))
                toClose.add(t)
                decoderInputs[pointLabelsName] = t
            }

            // box fallback if no point
            val boxName = findName("box")
            if (boxName != null && !decoderInputs.containsKey(boxName)) {
                val data = floatArrayOf(x1, y1, x2, y2)
                val t = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(data), longArrayOf(1, 1, 4))
                toClose.add(t)
                decoderInputs[boxName] = t
            }

            // mask_input -> [1,1,256,256] zeros
            var maskInputName: String? = null
            for (n in inputNames) {
                val l = n.lowercase()
                if (l.contains("mask") && !l.contains("has")) { maskInputName = n; break }
            }
            if (maskInputName != null && !decoderInputs.containsKey(maskInputName)) {
                val shape = longArrayOf(1, 1, 256, 256)
                val buf = FloatBuffer.allocate(256 * 256)
                val t = OnnxTensor.createTensor(ortEnv, buf, shape)
                toClose.add(t)
                decoderInputs[maskInputName] = t
            }

            // has_mask_input -> [1] 0
            val hasMaskName = findName("has", "mask")
            if (hasMaskName != null) {
                val buf = FloatBuffer.wrap(floatArrayOf(0f))
                val t = OnnxTensor.createTensor(ortEnv, buf, longArrayOf(1))
                toClose.add(t)
                decoderInputs[hasMaskName] = t
            }

            // orig_im_size -> [2] height,width
            var origName: String? = null
            for (n in inputNames) {
                val l = n.lowercase()
                if (l.contains("orig") && l.contains("size")) { origName = n; break }
            }
            if (origName == null) {
                for (n in inputNames) if (n.lowercase().contains("im") && n.lowercase().contains("size")) { origName = n; break }
            }
            if (origName != null && !decoderInputs.containsKey(origName)) {
                // try Long first, fallback to Float
                try {
                    val buf = LongBuffer.allocate(2)
                    buf.put(bitmap.height.toLong())
                    buf.put(bitmap.width.toLong())
                    buf.rewind()
                    val t = OnnxTensor.createTensor(ortEnv, buf, longArrayOf(2))
                    toClose.add(t)
                    decoderInputs[origName] = t
                } catch (_: Throwable) {
                    val buf = FloatBuffer.wrap(floatArrayOf(bitmap.height.toFloat(), bitmap.width.toFloat()))
                    val t = OnnxTensor.createTensor(ortEnv, buf, longArrayOf(2))
                    toClose.add(t)
                    decoderInputs[origName] = t
                }
            }

            // SAM2 high_res_feats: decoder expects encoder's multi-scale features
            // high_res_feats_0 -> [1,32,256,256], high_res_feats_1 -> [1,64,128,128]
            for (n in inputNames) {
                val lower = n.lowercase()
                if (lower.contains("high") && lower.contains("res") && !decoderInputs.containsKey(n)) {
                    val feat = findHighResFeat(embeddingOutputs, n)
                    if (feat != null) {
                        decoderInputs[n] = feat
                        Log.d("ModelInference", "Mapped $n to encoder feat shape=${(feat.info as? ai.onnxruntime.TensorInfo)?.shape?.joinToString()}")
                    } else {
                        Log.w("ModelInference", "No matching encoder feat for $n, will dummy")
                    }
                }
            }

            // fill any remaining unmapped inputs with zeros to avoid missing input error
            for (n in inputNames) {
                if (!decoderInputs.containsKey(n)) {
                    val lower = n.lowercase()
                    // Special handling for high_res still unmapped: create 4D dummy with correct rank
                    if (lower.contains("high") && lower.contains("res")) {
                        val shape = if (lower.contains("0")) longArrayOf(1, 32, 256, 256) else longArrayOf(1, 64, 128, 128)
                        val sz = (shape[1] * shape[2] * shape[3]).toInt()
                        val buf = FloatBuffer.allocate(sz)
                        val t = OnnxTensor.createTensor(ortEnv, buf, shape)
                        toClose.add(t)
                        decoderInputs[n] = t
                        Log.w("ModelInference", "HighRes $n not found in encoder, using zero dummy shape=${shape.joinToString()}")
                    } else {
                        Log.w("ModelInference", "Unmapped decoder input $n -> dummy zero")
                        val buf = FloatBuffer.wrap(floatArrayOf(0f))
                        val t = OnnxTensor.createTensor(ortEnv, buf, longArrayOf(1))
                        toClose.add(t)
                        decoderInputs[n] = t
                    }
                }
            }

            Log.d("ModelInference", "Decoder inputs final keys=${decoderInputs.keys}")
            outputs = samDecoderSession.run(decoderInputs)

            // Find mask: look for 4D tensor with H,W >=64
            var maskValue: Any? = null
            for (i in 0 until outputs.size()) {
                val v = outputs.get(i)
                val info = v.info
                if (info is ai.onnxruntime.TensorInfo) {
                    val shp = info.shape
                    if (shp.size == 4 && shp[2] >= 64 && shp[3] >= 64) {
                        maskValue = v.getValue()
                        break
                    }
                }
            }
            if (maskValue == null) maskValue = outputs.get(0).getValue()

            val lowResMask: Array<FloatArray> = when (maskValue) {
                is Array<*> -> {
                    val arr = maskValue as Array<*>
                    // try [1][1][H][W] -> [H][W]
                    try {
                        if (arr.isNotEmpty() && arr[0] is Array<*>) {
                            val a1 = arr[0] as Array<*>
                            if (a1.isNotEmpty() && a1[0] is Array<*>) {
                                val a2 = a1[0] as Array<FloatArray>
                                a2
                            } else if (a1.isNotEmpty() && a1[0] is FloatArray) {
                                a1 as Array<FloatArray>
                            } else {
                                arr as Array<FloatArray>
                            }
                        } else {
                            arr as Array<FloatArray>
                        }
                    } catch (e: Throwable) {
                        @Suppress("UNCHECKED_CAST")
                        (arr[0] as Array<Array<FloatArray>>)[0] as Array<FloatArray>
                    }
                }
                else -> error("Unexpected mask type ${maskValue?.javaClass}")
            }
            return resampleMaskToBitmap(lowResMask, bitmap.width, bitmap.height)
        } finally {
            runCatching { outputs?.close() }
            for (t in toClose) runCatching { t.close() }
        }
    }

    /**
     * Copy ONNX from assets to internal storage and return file path.
     * Using file path instead of byte[] avoids `134MB` heap allocation that
     * triggers `Failed to allocate a 134261328 byte allocation` OOM
     * (screenshot). ONNX Runtime can mmap the file directly.
     */
    @Synchronized
    private fun getModelFilePath(assetName: String): String {
        val outFile = java.io.File(appContext.filesDir, "onnx_models/$assetName")
        // Reuse if already copied and size looks valid (>1MB)
        if (outFile.exists() && outFile.length() > 1024 * 1024) {
            return outFile.absolutePath
        }
        outFile.parentFile?.mkdirs()
        val tmpFile = java.io.File(outFile.absolutePath + ".tmp")
        try {
            appContext.assets.open(assetName).use { input ->
                tmpFile.outputStream().use { out ->
                    // 8KB buffer to keep memory low
                    input.copyTo(out, bufferSize = 8192)
                }
            }
            if (tmpFile.length() == 0L) error("Failed to copy $assetName, tmp empty")
            if (outFile.exists()) outFile.delete()
            if (!tmpFile.renameTo(outFile)) {
                // fallback copy if rename fails (cross-device)
                tmpFile.copyTo(outFile, overwrite = true)
                tmpFile.delete()
            }
        } catch (t: Throwable) {
            tmpFile.delete()
            // fallback to old byte[] method if file copy fails (will OOM on large file but better than crash)
            Log.e("ModelInference", "getModelFilePath copy failed for $assetName: ${t.message}", t)
            if (outFile.exists() && outFile.length() > 0) return outFile.absolutePath
            throw t
        }
        Log.d("ModelInference", "Model $assetName copied to ${outFile.absolutePath} size=${outFile.length()}")
        return outFile.absolutePath
    }

    @Suppress("unused")
    private fun readAsset(name: String): ByteArray =
        appContext.assets.open(name).use { it.readBytes() }

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
                val v = (1 - dy) * ((1 - dx) * v00 + dx * v01) + dy * ((1 - dx) * v10 + dx * v11)
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
            setIntraOpNumThreads(Runtime.getRuntime().availableProcessors())
        }
    }
}
