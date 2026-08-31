package com.example.myapplicationkoG.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for POST /v1/garments/segment.
 * Application-level only — no Roboflow-specific fields leak into the Android domain.
 */
@Serializable
data class SegmentResponseDto(
    @SerialName("garmentId") val garmentId: String,
    @SerialName("side") val side: String, // "FRONT" or "BACK"
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("maskUrl") val maskUrl: String,
    @SerialName("width") val width: Int,
    @SerialName("height") val height: Int
)
