package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable

/**
 * One side of a garment (FRONT or BACK).
 * Holds the original source image and an independent mask.
 *
 * Editor layers will be added back in a follow-up.
 */
@Serializable
data class GarmentSide(
    val sourceImage: ImageAsset,
    val garmentMask: MaskAsset? = null,
    /** Path to the 1080x1080 rotated, center-cropped design-space bitmap. */
    val designSpacePath: String? = null,
)