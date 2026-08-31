package com.example.myapplicationkoG.editor

import androidx.compose.ui.graphics.Color
import com.example.myapplicationkoG.domain.model.ClothingDocument
import com.example.myapplicationkoG.domain.model.GarmentSideId
import com.example.myapplicationkoG.domain.model.Viewport

/**
 * Immutable editor state. The ViewModel owns this and emits via StateFlow.
 * Composable functions only read; they never mutate the document directly.
 */
data class EditorState(
    val document: ClothingDocument? = null,
    val activeSide: GarmentSideId = GarmentSideId.FRONT,
    val viewport: Viewport = Viewport(),
    val selectedTool: EditorTool = EditorTool.NONE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // ---- Part 2 P1 (Dye + Cut) ----
    /** Current brush / cut colour. Packed ARGB. */
    val toolColorArgb: Int = 0xFFD32F2F.toInt(),
    /** Brush radius in garment coordinates (scales with viewport at draw time). */
    val brushRadius: Float = 48f,
    /** Opacity of the next stroke [0,1]. */
    val brushOpacity: Float = 1f,
    /** Dye intensity multiplier [0,1]. */
    val dyeIntensity: Float = 1f,
    /** Cut stroke width in garment coordinates. */
    val cutWidth: Float = 18f,
    /** Live in-progress stroke. null when not drawing. Drives preview only. */
    val inProgressStroke: InProgressStroke? = null,
    /** Whether undo/redo is currently allowed. */
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val activeSideData
        get() = when (activeSide) {
            GarmentSideId.FRONT -> document?.front
            GarmentSideId.BACK -> document?.back
        }

    val toolColor: Color get() = Color(toolColorArgb)
}

/**
 * A live, uncommitted brush / cut stroke. Cleared when the user lifts the
 * finger. Persists only as part of the layer once the ViewModel commits it.
 */
data class InProgressStroke(
    val tool: EditorTool,
    val points: List<com.example.myapplicationkoG.domain.model.Point>
)
