package com.example.myapplicationkoG.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Reads/writes image bytes outside the structured document store.
 * - Originals and masks live as files in app private storage.
 * - Documents (Proto DataStore) only hold URIs and dimensions.
 *
 * The renderer MUST NOT decode images on every frame. It calls
 * [loadBitmap] once and reuses the resulting Bitmap.
 */
object ImageCache {

    private const val DIR_NAME = "garment_assets"

    fun assetDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Copy a SAF/content Uri into app private storage and return the new file path.
     * Returns null if the source cannot be opened.
     */
    fun importFromUri(context: Context, source: Uri, prefix: String): File? {
        return try {
            val ext = guessExtension(context, source)
            val target = File(assetDir(context), "${prefix}_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(source)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return null
            target
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Decode with inSampleSize so we never blow memory on huge photos.
     Returns the dimensions without allocating a full bitmap if [justBounds] is true.
     */
    fun decodeBounds(file: File): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return opts.outWidth to opts.outHeight
    }

    /**
     * Decode at a target longest-edge size to keep preview light.
     */
    fun loadBitmap(file: File, maxEdge: Int = 2048): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val (w, h) = bounds.outWidth to bounds.outHeight
        if (w <= 0 || h <= 0) return null
        var sample = 1
        val longest = maxOf(w, h)
        while (longest / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun guessExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: return "jpg"
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            else -> "jpg"
        }
    }

    /**
     * Save a generated [Bitmap] (e.g. mask or design-space render) to app
     * private storage. Returns the file so the caller can build a Uri.
     */
    fun exportBitmap(context: Context, bitmap: Bitmap, name: String): File {
        val target = File(assetDir(context), name)
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return target
    }
}
