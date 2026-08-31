package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable

/**
 * One side of a garment (FRONT or BACK).
 * Holds the original source image and an independent mask.
 * Layers are non-destructive edits stacked on top.
 */
@Serializable
data class GarmentSide(
    val sourceImage: ImageAsset,
    val garmentMask: MaskAsset? = null,
    val layers: List<EditorLayer> = emptyList()
)
