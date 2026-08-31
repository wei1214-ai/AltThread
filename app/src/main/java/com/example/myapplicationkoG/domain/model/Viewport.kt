package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable

/**
 * Viewport state. Separated from ClothingDocument so that
 * pan/zoom NEVER mutates the document.
 *
 * All persistent edits (Part 2) are stored in garment coordinates,
 * not in screen or viewport coordinates.
 *
 * Coordinate system:
 *   screen coords -> viewport transform (scale/translation) -> garment coords
 */
@Serializable
data class Viewport(
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val rotation: Float = 0f
) {
    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 8f
    }
}
