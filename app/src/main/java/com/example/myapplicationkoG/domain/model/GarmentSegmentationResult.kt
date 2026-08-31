package com.example.myapplicationkoG.domain.model

/**
 * Application-level result of a segmentation request.
 * Android domain layer must NEVER see Roboflow-specific shapes.
 */
data class GarmentSegmentationResult(
    val sourceImage: ImageAsset,
    val mask: MaskAsset,
    val confidence: Float?,
    val width: Int,
    val height: Int
)
