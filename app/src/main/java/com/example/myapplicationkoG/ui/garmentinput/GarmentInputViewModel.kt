package com.example.myapplicationkoG.ui.garmentinput

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplicationkoG.di.ServiceLocator
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.inference.ClothingInferencePipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * State holder for [GarmentInputScreen].
 *
 * Flow per side (FRONT then BACK):
 *   1. Copy the picked Uri into app private storage (file:// is stable).
 *   2. Run on-device cloth U2NET → cutout bitmap.
 *   3. Save the cutout as a PNG so the next screen can pick it up.
 *
 * UI just shows the final cutout preview for each side.
 */
class GarmentInputViewModel(app: Application) : AndroidViewModel(app) {

    private val inference: ClothingInferencePipeline =
        ServiceLocator.inferencePipeline(app)
    private val cacheDir: File = File(app.filesDir, "garment_assets").apply { mkdirs() }

    private val _state = MutableStateFlow(GarmentInputUiState())
    val state: StateFlow<GarmentInputUiState> = _state.asStateFlow()

    private val runId: String = UUID.randomUUID().toString()

    var openDesignId: String? = null
    var openDesignName: String = ""

    fun openSavedDesign(id: String, name: String, front: File, back: File) {
        openDesignId = id
        openDesignName = name
        loadDesignPaths(front, back)
    }

    fun loadDesignPaths(front: File, back: File) {
        _state.update {
            it.copy(
                frontCutoutPath = front.absolutePath,
                backCutoutPath = back.absolutePath,
                frontError = null,
                backError = null,
                isLoading = false,
                loadingSide = null
            )
        }
    }

    fun clearAll() {
        // Delete cached files and reset state so re-entering Design Space is clean
        _state.value.frontCutoutPath?.let { runCatching { File(it).delete() } }
        _state.value.backCutoutPath?.let { runCatching { File(it).delete() } }
        // Keep cacheDir but clear state
        _state.value = GarmentInputUiState()
        openDesignId = null
        openDesignName = ""
    }

    fun onPickedImage(side: GarmentSideId, uri: Uri) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update {
                when (side) {
                    GarmentSideId.FRONT -> it.copy(isLoading = true, loadingSide = GarmentSideId.FRONT, frontError = null)
                    GarmentSideId.BACK -> it.copy(isLoading = true, loadingSide = GarmentSideId.BACK, backError = null)
                }
            }
            try {
                // Remember old path to delete after new one succeeds (avoid Coil cache showing old image)
                val oldPath = when (side) {
                    GarmentSideId.FRONT -> _state.value.frontCutoutPath
                    GarmentSideId.BACK -> _state.value.backCutoutPath
                }
                val cutout = withContext(Dispatchers.IO) {
                    val imported = copyToCache(uri)
                        ?: error("Could not import picked image")
                    try {
                        // Same bytes = same cutout, reuse cache and skip inference.
                        // v2 key: pipeline fixes must re-run instead of reusing v1 results.
                        val out = File(cacheDir, "cutout2_${sha256(imported)}.png")
                        if (!(out.exists() && out.length() > 0L)) {
                            val result = inference.run(imported)
                            try {
                                savePng(result.cutout, out)
                            } finally {
                                // pipeline returns a fresh bitmap, recycle after saving to avoid OOM
                                runCatching { if (!result.cutout.isRecycled) result.cutout.recycle() }
                            }
                        }
                        // Delete old cutout file for this side
                        if (oldPath != null && oldPath != out.absolutePath) {
                            runCatching { File(oldPath).delete() }
                        }
                        out
                    } finally {
                        // clean up imported temp file
                        runCatching { imported.delete() }
                    }
                }
                _state.update { current ->
                    when (side) {
                        GarmentSideId.FRONT -> current.copy(frontCutoutPath = cutout.absolutePath, frontError = null, isLoading = false, loadingSide = null)
                        GarmentSideId.BACK -> current.copy(backCutoutPath = cutout.absolutePath, backError = null, isLoading = false, loadingSide = null)
                    }
                }
            } catch (t: Throwable) {
                val msg = t.message ?: "Failed to process image"
                _state.update {
                    when (side) {
                        GarmentSideId.FRONT -> it.copy(isLoading = false, loadingSide = null, frontError = msg)
                        GarmentSideId.BACK -> it.copy(isLoading = false, loadingSide = null, backError = msg)
                    }
                }
            }
        }
    }

    private fun copyToCache(uri: Uri): File? {
        val target = File(cacheDir, "in_${runId}_${System.currentTimeMillis()}.jpg")
        return try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            if (target.length() > 0L) target else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun savePng(bitmap: Bitmap, file: File) {
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { ins ->
            val buf = ByteArray(8192)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

data class GarmentInputUiState(
    val frontCutoutPath: String? = null,
    val backCutoutPath: String? = null,
    val frontError: String? = null,
    val backError: String? = null,
    val isLoading: Boolean = false,
    val loadingSide: GarmentSideId? = null,
)