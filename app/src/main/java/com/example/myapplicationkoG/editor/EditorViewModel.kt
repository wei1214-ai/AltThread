package com.example.myapplicationkoG.editor

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
import com.example.myapplicationkoG.domain.model.Viewport
import com.example.myapplicationkoG.storage.ImageCache
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
 * Owns EditorState. The UI never calls anything except the public
 * [onPickedImage], [switchSide], [setTool], [applyViewport] etc.
 */
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val preferences = ServiceLocator.projectPreferences(app)
    private val segmentation = ServiceLocator.segmentationService(app)

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.documentFlow.collect { doc ->
                _state.update { it.copy(document = doc) }
            }
        }
    }

    // -------- Viewport (NEVER modifies the document) --------

    fun applyViewport(transform: Viewport) {
        val clamped = transform.copy(
            scale = transform.scale.coerceIn(Viewport.MIN_SCALE, Viewport.MAX_SCALE)
        )
        _state.update { it.copy(viewport = clamped) }
    }

    fun resetViewport() {
        _state.update { it.copy(viewport = Viewport()) }
    }

    // -------- Document mutation --------

    fun switchSide(side: GarmentSideId) {
        _state.update { it.copy(activeSide = side, viewport = Viewport()) }
    }

    fun setTool(tool: EditorTool) {
        _state.update { it.copy(selectedTool = tool) }
    }

    /**
     * User picked an image for a side via SAF. We:
     *  1. Copy the bytes into app private storage (ImageCache).
     *  2. Create/update a stub document entry.
     *  3. Call the backend for segmentation.
     *  4. Persist the final document.
     */
    fun onPickedImage(side: GarmentSideId, source: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (file, dims) = withContext(Dispatchers.IO) {
                    val imported = ImageCache.importFromUri(getApplication(), source, "garment_${side.name.lowercase()}")
                        ?: error("Could not import image")
                    val (w, h) = ImageCache.decodeBounds(imported)
                    imported to (w to h)
                }
                val sourceImage = ImageAsset(
                    id = UUID.randomUUID().toString(),
                    uri = Uri.fromFile(file).toString(),
                    width = dims.first,
                    height = dims.second
                )
                upsertSideStub(side, sourceImage)

                val result = withContext(Dispatchers.IO) { segmentation.segment(file, side) }
                val mask = MaskAsset(
                    id = result.mask.id,
                    uri = result.mask.uri,
                    width = result.mask.width.takeIf { it > 0 } ?: sourceImage.width,
                    height = result.mask.height.takeIf { it > 0 } ?: sourceImage.height
                )
                finalizeSide(side, sourceImage, mask)
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, errorMessage = t.message ?: "Unknown error") }
            }
        }
    }

    fun retrySegmentation(side: GarmentSideId) {
        val s = _state.value
        val src = when (side) {
            GarmentSideId.FRONT -> s.document?.front?.sourceImage
            GarmentSideId.BACK -> s.document?.back?.sourceImage
        } ?: return
        val file = File(Uri.parse(src.uri).path ?: return)
        if (!file.exists()) {
            _state.update { it.copy(errorMessage = "Original image is missing on disk.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = withContext(Dispatchers.IO) { segmentation.segment(file, side) }
                val mask = MaskAsset(
                    id = result.mask.id,
                    uri = result.mask.uri,
                    width = result.mask.width.takeIf { it > 0 } ?: src.width,
                    height = result.mask.height.takeIf { it > 0 } ?: src.height
                )
                finalizeSide(side, src, mask)
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, errorMessage = t.message ?: "Segmentation failed") }
            }
        }
    }

    private fun upsertSideStub(side: GarmentSideId, source: ImageAsset) {
        _state.update { current ->
            val doc = current.document ?: emptyDocument(source)
            val updated = when (side) {
                GarmentSideId.FRONT -> doc.copy(front = GarmentSide(source, garmentMask = null, layers = emptyList()))
                GarmentSideId.BACK -> doc.copy(back = GarmentSide(source, garmentMask = null, layers = emptyList()))
            }
            current.copy(document = updated, activeSide = side)
        }
        persist()
    }

    private fun finalizeSide(side: GarmentSideId, source: ImageAsset, mask: MaskAsset) {
        _state.update { current ->
            val doc = current.document ?: emptyDocument(source)
            val updated = when (side) {
                GarmentSideId.FRONT -> doc.copy(front = GarmentSide(source, mask, emptyList()))
                GarmentSideId.BACK -> doc.copy(back = GarmentSide(source, mask, emptyList()))
            }
            current.copy(document = updated, isLoading = false, errorMessage = null)
        }
        persist()
    }

    private fun emptyDocument(seed: ImageAsset): ClothingDocument {
        val id = UUID.randomUUID().toString()
        val side = GarmentSide(seed, null, emptyList())
        return ClothingDocument(id = id, front = side, back = side)
    }

    private fun persist() {
        viewModelScope.launch {
            val doc = _state.value.document ?: return@launch
            preferences.saveDocument(doc)
        }
    }
}
