package com.example.myapplicationkoG.rendering

import com.example.myapplicationkoG.domain.model.GarmentSide
import com.example.myapplicationkoG.domain.model.Viewport
import androidx.compose.ui.graphics.ImageBitmap

/**
 * What the renderer needs to draw one frame.
 * - [source] is the garment source image, decoded once and reused.
 * - [mask] is the segmentation mask, decoded once and reused.
 * - [viewport] is the current pan/zoom; renderer applies it but never mutates it.
 *
 * Coordinates used for compositing are viewport (screen) coordinates;
 * the underlying edits (Part 2) live in garment coordinates.
 */
data class RenderState(
    val side: GarmentSide,
    val viewport: Viewport,
    val source: ImageBitmap?,
    val mask: ImageBitmap?
)
