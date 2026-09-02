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
 *   2. Run on-device YOLO + SAM 2.1 → cutout bitmap.
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

    fun onPickedImage(side: GarmentSideId, uri: Uri) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val cutout = withContext(Dispatchers.IO) {
                    val imported = copyToCache(uri)
                        ?: error("Could not import picked image")
                    val result = inference.run(imported)
                    val out = File(cacheDir, "garment_${runId}_${side.name.lowercase()}.png")
                    savePng(result.cutout, out)
                    out
                }
                _state.update { current ->
                    when (side) {
                        GarmentSideId.FRONT -> current.copy(frontCutoutPath = cutout.absolutePath, isLoading = false)
                        GarmentSideId.BACK -> current.copy(backCutoutPath = cutout.absolutePath, isLoading = false)
                    }
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "Failed to process image")
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
}

data class GarmentInputUiState(
    val frontCutoutPath: String? = null,
    val backCutoutPath: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)