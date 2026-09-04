package com.example.myapplicationkoG.ui.editor

import com.example.myapplicationkoG.domain.model.GarmentSideId

data class LoadedDesign(
    val dye: Map<GarmentSideId, DyeState>,
    val buttons: Map<GarmentSideId, List<PlacedButton>>
)

/**
 * One-shot handoff from the Continue screen to the editor.
 * Consumed once so recomposition never re-applies stale data.
 */
object DesignSession {
    @Volatile private var pending: LoadedDesign? = null

    fun stage(dye: Map<GarmentSideId, DyeState>, buttons: Map<GarmentSideId, List<PlacedButton>>) {
        pending = LoadedDesign(dye, buttons)
    }

    fun consume(): LoadedDesign? {
        val p = pending
        pending = null
        return p
    }
}
