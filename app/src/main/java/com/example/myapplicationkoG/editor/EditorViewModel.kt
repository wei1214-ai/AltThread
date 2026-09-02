package com.example.myapplicationkoG.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplicationkoG.di.ServiceLocator
import com.example.myapplicationkoG.domain.model.BrushStroke
import com.example.myapplicationkoG.domain.model.ClothingDocument
import com.example.myapplicationkoG.domain.model.CutLayer
import com.example.myapplicationkoG.domain.model.DyeLayer
import com.example.myapplicationkoG.domain.model.EditorLayer
import com.example.myapplicationkoG.domain.model.GarmentSide
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.domain.model.ImageAsset
import com.example.myapplicationkoG.domain.model.MaskAsset
import com.example.myapplicationkoG.domain.model.Point
import com.example.myapplicationkoG.domain.model.VectorPath
import com.example.myapplicationkoG.domain.model.Viewport
import com.example.myapplicationkoG.inference.InferenceResult
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
 * Owns EditorState. The UI never calls anything except the public methods.
 *
 * Part 2 P1 adds:
 *  - tool property setters (color / radius / opacity / intensity / cutWidth)
 *  - live stroke state (beginStroke / extendStroke / endStroke)
 *  - undo / redo (snapshot-based)
 *  - mutation pipeline that always commits a snapshot AFTER applying a
 *    change, so the next undo rolls back to exactly the pre-mutation state
 */
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val preferences = ServiceLocator.projectPreferences(app)
    private val inference = ServiceLocator.inferencePipeline(app)
    private val history = UndoRedoManager()

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.documentFlow.collect { doc ->
                if (doc == null) return@collect
                _state.update {
                    // Fresh load: reset history. A more sophisticated implementation
                    // would persist history too, but for MVP history is session-only.
                    history.clear()
                    it.copy(
                        document = doc,
                        canUndo = false,
                        canRedo = false,
                        errorMessage = null
                    )
                }
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
        _state.update { it.copy(selectedTool = tool, inProgressStroke = null) }
    }

    // -------- Tool property setters (P1) --------

    fun setToolColor(argb: Int) = _state.update { it.copy(toolColorArgb = argb) }
    fun setBrushRadius(r: Float) = _state.update { it.copy(brushRadius = r.coerceIn(2f, 400f)) }
    fun setBrushOpacity(o: Float) = _state.update { it.copy(brushOpacity = o.coerceIn(0f, 1f)) }
    fun setDyeIntensity(i: Float) = _state.update { it.copy(dyeIntensity = i.coerceIn(0f, 1f)) }
    fun setCutWidth(w: Float) = _state.update { it.copy(cutWidth = w.coerceIn(2f, 200f)) }

    // -------- Undo / Redo (P1) --------

    fun undo() {
        val current = _state.value.document ?: return
        val target = history.undo(current) ?: return
        _state.update {
            it.copy(
                document = target,
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                inProgressStroke = null
            )
        }
        persist()
    }

    fun redo() {
        val current = _state.value.document ?: return
        val target = history.redo(current) ?: return
        _state.update {
            it.copy(
                document = target,
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                inProgressStroke = null
            )
        }
        persist()
    }

    // -------- Live stroke (preview only; persisted on endStroke) --------

    fun beginStroke(point: Point) {
        val tool = _state.value.selectedTool
        if (tool != EditorTool.DYE && tool != EditorTool.CUT) return
        _state.update { it.copy(inProgressStroke = InProgressStroke(tool, listOf(point))) }
    }

    fun extendStroke(point: Point) {
        val tool = _state.value.selectedTool
        if (tool != EditorTool.DYE && tool != EditorTool.CUT) return
        _state.update { current ->
            val live = current.inProgressStroke ?: InProgressStroke(tool, emptyList())
            val updated = live.copy(points = live.points + point)
            current.copy(inProgressStroke = updated)
        }
    }

    fun endStroke() {
        val current = _state.value
        val live = current.inProgressStroke ?: return
        if (live.points.isEmpty()) {
            _state.update { it.copy(inProgressStroke = null) }
            return
        }
        when (live.tool) {
            EditorTool.DYE -> commitDyeStroke(live)
            EditorTool.CUT -> commitCutPath(live)
            else -> _state.update { it.copy(inProgressStroke = null) }
        }
    }

    private fun commitDyeStroke(live: InProgressStroke) {
        val current = _state.value
        val side = current.activeSideData ?: return
        val newLayer = DyeLayer(
            id = UUID.randomUUID().toString(),
            visible = true,
            opacity = current.brushOpacity,
            order = side.layers.size,
            brushPaths = listOf(
                BrushStroke(
                    points = live.points,
                    radius = current.brushRadius
                )
            ),
            colorArgb = current.toolColorArgb,
            intensity = current.dyeIntensity,
            brushRadius = current.brushRadius,
            isEraser = false
        )
        applyLayerAddition(side, newLayer)
    }

    private fun commitCutPath(live: InProgressStroke) {
        val current = _state.value
        val side = current.activeSideData ?: return
        val newLayer = CutLayer(
            id = UUID.randomUUID().toString(),
            visible = true,
            opacity = 1f,
            order = side.layers.size,
            path = VectorPath(points = live.points, closed = false),
            width = current.cutWidth
        )
        applyLayerAddition(side, newLayer)
    }

    private fun applyLayerAddition(side: GarmentSide, layer: EditorLayer) {
        snapshot()
        _state.update { current ->
            val updatedSide = side.copy(layers = side.layers + layer)
            val doc = when (current.activeSide) {
                GarmentSideId.FRONT -> current.document?.copy(front = updatedSide)
                GarmentSideId.BACK -> current.document?.copy(back = updatedSide)
            }
            current.copy(
                document = doc,
                inProgressStroke = null,
                canUndo = history.canUndo,
                canRedo = history.canRedo
            )
        }
        persist()
    }

    /**
     * Capture the current document as an undo checkpoint. Called BEFORE
     * mutating the document so undo can restore this exact state.
     */
    private fun snapshot() {
        val doc = _state.value.document ?: return
        history.commit(doc)
    }

    // -------- Image import (unchanged from Part 1) --------

    fun onPickedImage(side: GarmentSideId, source: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 1. Copy the user's photo into the app's cache so we own
                //    the URI across configuration changes.
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

                // 2. Run the on-device pipeline (YOLO → SAM 2.1 → OpenCV).
                //    This produces a 1080x1080 design-space bitmap + a mask.
                val result = withContext(Dispatchers.IO) {
                    inference.run(Uri.fromFile(file))
                }
                val dsPath = withContext(Dispatchers.IO) {
                    ImageCache.exportBitmap(getApplication(), result.designSpace, "design_${side.name.lowercase()}.png")
                }
                val maskPath = withContext(Dispatchers.IO) {
                    ImageCache.exportBitmap(getApplication(), result.mask, "mask_${side.name.lowercase()}.png")
                }
                val mask = MaskAsset(
                    id = UUID.randomUUID().toString(),
                    uri = Uri.fromFile(maskPath).toString(),
                    width = result.mask.width,
                    height = result.mask.height,
                )
                finalizeSideWithDesign(side, sourceImage, mask, Uri.fromFile(dsPath).toString())
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
                val result = withContext(Dispatchers.IO) {
                    inference.run(Uri.fromFile(file))
                }
                val dsPath = withContext(Dispatchers.IO) {
                    ImageCache.exportBitmap(getApplication(), result.designSpace, "design_${side.name.lowercase()}.png")
                }
                val maskPath = withContext(Dispatchers.IO) {
                    ImageCache.exportBitmap(getApplication(), result.mask, "mask_${side.name.lowercase()}.png")
                }
                val mask = MaskAsset(
                    id = UUID.randomUUID().toString(),
                    uri = Uri.fromFile(maskPath).toString(),
                    width = result.mask.width,
                    height = result.mask.height,
                )
                finalizeSideWithDesign(side, src, mask, Uri.fromFile(dsPath).toString())
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, errorMessage = t.message ?: "Segmentation failed") }
            }
        }
    }

    private fun upsertSideStub(side: GarmentSideId, source: ImageAsset) {
        _state.update { current ->
            val doc = current.document ?: emptyDocument(source)
            val updated = when (side) {
                GarmentSideId.FRONT -> doc.copy(front = GarmentSide(source, garmentMask = null, designSpacePath = null, layers = emptyList()))
                GarmentSideId.BACK -> doc.copy(back = GarmentSide(source, garmentMask = null, designSpacePath = null, layers = emptyList()))
            }
            current.copy(document = updated, activeSide = side)
        }
        persist()
    }

    private fun finalizeSide(side: GarmentSideId, source: ImageAsset, mask: MaskAsset) {
        _state.update { current ->
            val doc = current.document ?: emptyDocument(source)
            val updated = when (side) {
                GarmentSideId.FRONT -> doc.copy(front = GarmentSide(source, mask, designSpacePath = null, layers = emptyList()))
                GarmentSideId.BACK -> doc.copy(back = GarmentSide(source, mask, designSpacePath = null, layers = emptyList()))
            }
            current.copy(document = updated, isLoading = false, errorMessage = null)
        }
        persist()
    }

    /**
     * Same as [finalizeSide] but also stores the 1080x1080 design-space
     * bitmap path produced by the on-device pipeline (YOLO + SAM 2.1 +
     * OpenCV). The design space is what the user sees first inside the
     * editor.
     */
    private fun finalizeSideWithDesign(
        side: GarmentSideId,
        source: ImageAsset,
        mask: MaskAsset,
        designSpacePath: String,
    ) {
        _state.update { current ->
            val doc = current.document ?: emptyDocument(source)
            val updated = when (side) {
                GarmentSideId.FRONT -> doc.copy(
                    front = GarmentSide(
                        sourceImage = source,
                        garmentMask = mask,
                        designSpacePath = designSpacePath,
                        layers = emptyList()
                    )
                )
                GarmentSideId.BACK -> doc.copy(
                    back = GarmentSide(
                        sourceImage = source,
                        garmentMask = mask,
                        designSpacePath = designSpacePath,
                        layers = emptyList()
                    )
                )
            }
            current.copy(document = updated, isLoading = false, errorMessage = null)
        }
        persist()
    }

    private fun emptyDocument(seed: ImageAsset): ClothingDocument {
        val id = UUID.randomUUID().toString()
        val side = GarmentSide(seed, null, null, emptyList())
        return ClothingDocument(id = id, front = side, back = side)
    }

    private fun persist() {
        viewModelScope.launch {
            val doc = _state.value.document ?: return@launch
            preferences.saveDocument(doc)
        }
    }
}
