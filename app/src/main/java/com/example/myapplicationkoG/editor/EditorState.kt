package com.example.myapplicationkoG.editor

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
    val errorMessage: String? = null
) {
    val activeSideData
        get() = when (activeSide) {
            GarmentSideId.FRONT -> document?.front
            GarmentSideId.BACK -> document?.back
        }
}
