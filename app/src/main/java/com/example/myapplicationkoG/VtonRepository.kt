package com.example.myapplicationkoG

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object VtonConfig {
    const val BASE_URL = "https://yisol-idm-vton.hf.space"
    const val API = "tryon"
    val TOKEN: String get() = BuildConfig.HF_TOKEN
    val TOKEN2: String get() = BuildConfig.HF_TOKEN2
    fun tokens(): List<String> = listOfNotNull(
        TOKEN.takeIf { it.isNotBlank() },
        TOKEN2.takeIf { it.isNotBlank() }
    )
}

class VtonRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private fun auth(url: String, token: String = VtonConfig.TOKEN): Request.Builder {
        val b = Request.Builder().url(url)
        if (token.isNotBlank()) {
            b.header("Authorization", "Bearer $token")
        }
        return b
    }

    private suspend fun uploadImage(file: File, token: String = VtonConfig.TOKEN): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "files", file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
            .build()
        val req = auth("${VtonConfig.BASE_URL}/upload?upload_id=${UUID.randomUUID().toString().replace("-", "")}", token)
            .post(body)
            .build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: error("Empty upload response")
        if (!resp.isSuccessful) {
            if (resp.code == 429 || respBody.contains("quota", ignoreCase = true)) {
                error("QUOTA_EXCEEDED")
            }
            error("Upload failed (${resp.code}): $respBody")
        }
        json.parseToJsonElement(respBody).jsonArray.first().jsonPrimitive.content
    }

    private fun padToRatio(file: File, ratioW: Int = 3, ratioH: Int = 4): File {
        val src = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            ?: return file
        val targetRatio = ratioW.toFloat() / ratioH
        val srcRatio = src.width.toFloat() / src.height
        if (kotlin.math.abs(srcRatio - targetRatio) < 0.01f) return file
        val (dw, dh) = if (srcRatio > targetRatio) {
            src.width to (src.width / targetRatio).toInt()
        } else {
            (src.height * targetRatio).toInt() to src.height
        }
        val out = android.graphics.Bitmap.createBitmap(dw, dh, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(
            src,
            null,
            android.graphics.Rect((dw - src.width) / 2, (dh - src.height) / 2, (dw + src.width) / 2, (dh + src.height) / 2),
            android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        )
        if (out !== src) runCatching { if (!src.isRecycled) src.recycle() }
        val tmp = File.createTempFile("vton_pad_", ".jpg")
        tmp.outputStream().use { o ->
            out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, o)
        }
        runCatching { if (!out.isRecycled) out.recycle() }
        return tmp
    }

    suspend fun runTryOn(
        personFile: File,
        garmentFile: File,
        prompt: String = "a photo of a garment"
    ): File {
        var lastError: Exception? = null
        for (token in VtonConfig.tokens().ifEmpty { listOf("") }) {
            try {
                return runTryOnWithToken(personFile, garmentFile, prompt, token)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("QUOTA_EXCEEDED") || msg.contains("quota", ignoreCase = true) || msg.contains("429")) {
                    lastError = e
                    continue
                }
                throw e
            }
        }
        throw lastError ?: error("Try-on failed: no token available")
    }

    private suspend fun runTryOnWithToken(
        personFile: File,
        garmentFile: File,
        prompt: String,
        token: String
    ): File = withContext(Dispatchers.IO) {
        val personPath = uploadImage(padToRatio(personFile), token)
        val garmentPath = uploadImage(padToRatio(garmentFile), token)

        fun fileData(path: String) = buildJsonObject {
            put("path", path)
        }
        val sessionHash = UUID.randomUUID().toString().replace("-", "")
        val payload = buildJsonObject {
            put("fn_index", 2)
            putJsonArray("data") {
                add(buildJsonObject {
                    put("background", fileData(personPath))
                    putJsonArray("layers") {}
                    put("composite", fileData(personPath))
                })
                add(fileData(garmentPath))
                add(JsonPrimitive(prompt))
                add(JsonPrimitive(true))
                add(JsonPrimitive(false))
                add(JsonPrimitive(30))
                add(JsonPrimitive(Random.nextInt(0, Int.MAX_VALUE)))
            }
            put("session_hash", sessionHash)
        }.toString()

        val joinReq = auth("${VtonConfig.BASE_URL}/queue/join", token)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .build()
        val joinResp = client.newCall(joinReq).execute()
        val joinBody = joinResp.body?.string() ?: error("Empty join response")
        if (!joinResp.isSuccessful) {
            if (joinResp.code == 429 || joinBody.contains("quota", ignoreCase = true)) error("QUOTA_EXCEEDED")
            error("Queue join failed (${joinResp.code}): $joinBody")
        }

        val streamReq = auth("${VtonConfig.BASE_URL}/queue/data?session_hash=$sessionHash", token)
            .header("Accept", "text/event-stream")
            .get()
            .build()
        val streamResp = client.newCall(streamReq).execute()
        if (!streamResp.isSuccessful) error("Stream failed (${streamResp.code})")
        var resultUrl: String? = null
        var lastError: String? = null
        val deadline = System.currentTimeMillis() + 8 * 60 * 1000L
        streamResp.body?.source()?.use { source ->
            while (true) {
                if (System.currentTimeMillis() > deadline) error("Timed out waiting for try-on result")
                val line = try {
                    source.readUtf8Line() ?: break
                } catch (_: Exception) {
                    break
                }
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data.isEmpty()) continue
                    try {
                    val event = json.parseToJsonElement(data).jsonObject
                    when (event["msg"]?.jsonPrimitive?.content) {
                        "process_completed" -> {
                            val output = event["output"]?.jsonObject
                                ?: error("No output in completed event")
                            val errField = output["error"]
                            if (errField != null && errField.toString() != "null") {
                                error("Try-on failed: $errField")
                            }
                            val dataArr = output["data"]?.jsonArray
                                ?: error("No data in completed event")
                            val first = dataArr.firstOrNull()
                            resultUrl = when (first) {
                                is JsonObject -> fileUrl(first)
                                else -> {
                                    val raw = first.toString().trim('"')
                                    if (raw.isBlank() || raw == "null") error("Try-on failed: $data")
                                    if (raw.startsWith("http")) raw
                                    else "${VtonConfig.BASE_URL}/file=$raw"
                                }
                            }
                            break
                        }
                        "error", "unexpected_error" -> error("Try-on failed: $data")
                        else -> Unit
                    }
                } catch (e: Exception) {
                    if (e.message?.startsWith("Try-on failed") == true) throw e
                    lastError = data
                }
            }
        }
        val url = resultUrl ?: error("Try-on failed: ${lastError ?: "no result"}")
        val dlReq = Request.Builder().url(url).get().build()
        val dlResp = client.newCall(dlReq).execute()
        val bytes = dlResp.body?.bytes() ?: error("Empty result image")
        val outFile = File.createTempFile("vton_result_", ".png")
        outFile.writeBytes(bytes)
        outFile
    }

    private fun fileUrl(obj: JsonObject): String {
        val raw = obj["url"]?.jsonPrimitive?.content
            ?: obj["path"]?.jsonPrimitive?.content
            ?: error("No file url")
        return when {
            raw.startsWith("http") -> raw
            raw.startsWith("/") -> VtonConfig.BASE_URL + raw
            else -> "${VtonConfig.BASE_URL}/gradio_api/file=$raw"
        }
    }

    suspend fun saveToGallery(context: Context, file: File, name: String): String =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AltThread")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: error("Could not create gallery entry")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: error("Could not write gallery file")
            uri.toString()
        }
}
