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
import com.example.myapplicationkoG.inference.InferenceResult
import com.example.myapplicationkoG.storage.ImageCache
import com.example.myapplicationkoG.storage.ProjectPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Lightweight state holder for [GarmentInputScreen].
 *
 * Responsibilities:
 *  - Pick FRONT / BACK images from a SAF Uri.
 *  - Run YOLOv8 → SAM 2.1 → OpenCV on each picked image.
 *  - Persist originals, masks and 1080x1080 design-space bitmaps to
 *    app private storage via [ImageCache].
 *  - Expose a [ClothingDocument] (with `front` / `back` populated)
 *    so the next screen can pick up the work.
 *
 * No editor logic, no layers, no undo/redo. That will be rebuilt in
 * a follow-up.
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
                val result = inference.run(uri)

        // Persist source / mask / design-space bitmaps to private storage
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

        _state.update { current ->
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
        }
        // Persist best-effort; UI does not block on this.
        val finalDoc = _state.value.document
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