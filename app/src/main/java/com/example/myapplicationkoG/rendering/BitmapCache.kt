package com.example.myapplicationkoG.rendering

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.myapplicationkoG.domain.model.GarmentSide
import com.example.myapplicationkoG.domain.model.ImageAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Decodes source / mask ONCE and caches the result so the renderer never
 * re-decodes or re-uploads textures during pan/zoom.
 *
 * Decode happens on Dispatchers.IO. Results are exposed as Compose [ImageBitmap]
 * so the canvas can draw them directly without copying.
 */
class BitmapCache {

    @Volatile private var lastKey: String? = null
    @Volatile private var lastSource: ImageBitmap? = null
    @Volatile private var lastMask: ImageBitmap? = null

    suspend fun loadFor(side: GarmentSide): Pair<ImageBitmap?, ImageBitmap?> = withContext(Dispatchers.IO) {
        val key = "${side.sourceImage.uri}|${side.garmentMask?.uri}"
        if (key == lastKey) return@withContext lastSource to lastMask
        val src = decodeAsset(side.sourceImage, maxEdge = 2048)
        val mask = side.garmentMask?.let { decodeAsset(it, maxEdge = 2048) }
        lastKey = key
        lastSource = src
        lastMask = mask
        src to mask
    }

    /** Snapshot of the most recently decoded bitmaps. Useful for synchronous reads in draw. */
    fun snapshot(): Pair<ImageBitmap?, ImageBitmap?> = lastSource to lastMask

    fun invalidate() {
        lastKey = null
        lastSource = null
        lastMask = null
    }

    private fun decodeAsset(asset: ImageAsset, maxEdge: Int): ImageBitmap? = try {
        decodeFile(asset.uri, maxEdge)?.asImageBitmap()
    } catch (_: Throwable) { null }

    private fun decodeFile(uri: String, maxEdge: Int): Bitmap? {
        if (uri.isBlank()) return null
        val path: String = if (uri.startsWith("file://") || uri.startsWith("content://")) {
            Uri.parse(uri).path ?: return null
        } else uri
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        val longest = maxOf(w, h)
        while (longest / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }
}
