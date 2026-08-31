package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable

/**
 * Root document for the editor. The garment is the source of truth.
 * Both sides are independent: switching FRONT/BACK must preserve state.
 */
@Serializable
data class ClothingDocument(
    val id: String,
    val front: GarmentSide,
    val back: GarmentSide
)
