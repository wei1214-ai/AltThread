package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable

/**
 * A reference to a garment mask. Stored independently from the source image.
 * Source image bytes are NEVER modified to "bake" the mask in.
 */
@Serializable
data class MaskAsset(
    val id: String,
    val uri: String,
    val width: Int,
    val height: Int
)
