package com.example.myapplicationkoG.ui.garmentinput

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplicationkoG.di.ServiceLocator
import com.example.myapplicationkoG.domain.model.ClothingDocument
import com.example.myapplicationkoG.domain.model.GarmentSide
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.domain.model.ImageAsset
import com.example.myapplicationkoG.domain.model.MaskAsset
import com.example.myapplicationkoG.inference.ClothingInferencePipeline
import com.example.myapplicationkoG.storage.ImageCache
import com.example.myapplicationkoG.storage.ProjectPreferences
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
 * Lightweight state holder for [GarmentInputScreen].
 *
 * Flow:
 *  1. User picks FRONT / BACK image via SAF (photo picker).
 *  2. We copy the picked bytes into app private storage (so we don't have
 *     to fight `content://` permission flags later).
 *  3. The on-device pipeline (YOLO + SAM 2.1 + OpenCV) processes the local
 *     file and returns source / mask / 1080x1080 design-space bitmaps.
 *  4. Those bitmaps are persisted to private storage and a [ClothingDocument]
 *     is exposed to the UI so the next screen can pick up the work.
 *
 * No editor logic, no layers, no undo/redo.
 */
class GarmentInputViewModel(app: Application) : AndroidViewModel(app) {

    private val inference: ClothingInferencePipeline =
        ServiceLocator.inferencePipeline(app)
    private val preferences: ProjectPreferences =
        ServiceLocator.projectPreferences(app)

    private val _state = MutableStateFlow(GarmentInputUiState())
    val state: StateFlow<GarmentInputUiState> = _state.asStateFlow()

    private val docId: String = UUID.randomUUID().toString()

    fun onPickedImage(side: GarmentSideId, uri: Uri) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    // 1) Copy the picked Uri into private storage. This sidesteps
                    //    the flaky `content://` open paths on Android 13+.
                    val imported: File = ImageCache.importFromUri(app, uri, "garment_in_${docId}_${side.name.lowercase()}")
                        ?: error("Could not import picked image")

                    // 2) Run inference on the local file (file:// Uri always opens).
                    inference.run(Uri.fromFile(imported))
                }

                // 3) Persist source / mask / design-space bitmaps to private storage
                val app = getApplication<Application>()
                val prefix = "garment_${docId}_${side.name.lowercase()}"

                val sourceFile = ImageCache.exportBitmap(app, result.sourceBitmap, "${prefix}_source.png")
                val maskFile = ImageCache.exportBitmap(app, result.mask, "${prefix}_mask.png")
                val designFile = ImageCache.exportBitmap(app, result.designSpace, "${prefix}_design.png")

                val imageAsset = ImageAsset(
                    id = UUID.randomUUID().toString(),
                    uri = Uri.fromFile(sourceFile).toString(),
                    width = result.sourceBitmap.width,
                    height = result.sourceBitmap.height,
                )
                val maskAsset = MaskAsset(
                    id = UUID.randomUUID().toString(),
                    uri = Uri.fromFile(maskFile).toString(),
                    width = result.mask.width,
                    height = result.mask.height,
                )
                val designSpacePath = designFile.absolutePath

                val finalDoc = _state.updateAndGet { current ->
                    val doc = current.document ?: ClothingDocument(
                        id = docId,
                        front = sideStub(),
                        back = sideStub(),
                    )
                    val sideUpdated = GarmentSide(
                        sourceImage = imageAsset,
                        garmentMask = maskAsset,
                        designSpacePath = designSpacePath,
                    )
                    val newDoc = when (side) {
                        GarmentSideId.FRONT -> doc.copy(front = sideUpdated)
                        GarmentSideId.BACK -> doc.copy(back = sideUpdated)
                    }
                    current.copy(document = newDoc, isLoading = false, errorMessage = null)
                }.document

                if (finalDoc != null) {
                    runCatching { preferences.saveDocument(finalDoc) }
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Failed to process image",
                    )
                }
            }
        }
    }

    private fun sideStub(): GarmentSide {
        // Empty placeholder that will be replaced as soon as a real
        // image is picked for that side.
        val placeholder = ImageAsset(
            id = UUID.randomUUID().toString(),
            uri = "",
            width = 0,
            height = 0,
        )
        return GarmentSide(sourceImage = placeholder)
    }
}

/**
 * UI state for [GarmentInputScreen].
 */
data class GarmentInputUiState(
    val document: ClothingDocument? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

// Helper for MutableStateFlow.update + return new value
private inline fun <T> MutableStateFlow<T>.updateAndGet(function: (T) -> T): T {
    var newValue: T
    do {
        val prev = value
        newValue = function(prev)
    } while (!compareAndSet(prev, newValue))
    return newValue
}