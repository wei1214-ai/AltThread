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

object ChallengeSession {
    @Volatile var postId: String? = null
    @Volatile var title: String? = null
    @Volatile var description: String? = null

    fun stage(postId: String, title: String, description: String) {
        this.postId = postId
        this.title = title
        this.description = description
    }

    fun stage(title: String, description: String) {
        stage("", title, description)
    }

    fun consume(): Triple<String?, String?, String?> {
        val id = postId
        val t = title
        val d = description
        postId = null
        title = null
        description = null
        return Triple(id, t, d)
    }

    fun peek(): Triple<String?, String?, String?> = Triple(postId, title, description)
    fun peekPair(): Pair<String?, String?> = title to description
}
