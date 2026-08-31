package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Base contract for all editor layers.
 *
 * Polymorphic serialization uses a `type` discriminator so the same
 * `ClothingDocument` JSON can carry Dye / Cut / Distress / Patch / Stitch /
 * Fabric layers without provider-specific shape leakage.
 *
 * All layer geometry is stored in GARMENT coordinates (the source image's
 * native resolution). Viewport transforms never touch the document.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface EditorLayer {
    val id: String
    val visible: Boolean
    val opacity: Float
    val order: Int
}

/**
 * A dye stroke. The mask is the union of user brush paths intersected with
 * the garment mask (renderer does the intersection so dye can't leak).
 *
 * `color` is a packed ARGB int (same layout as `android.graphics.Color`).
 */
@Serializable
@SerialName("DyeLayer")
data class DyeLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val opacity: Float = 1f,
    override val order: Int = 0,
    val brushPaths: List<BrushStroke>,
    val colorArgb: Int,
    val intensity: Float = 1f,
    val brushRadius: Float = 24f,
    val isEraser: Boolean = false
) : EditorLayer

/**
 * A single brush dab along a path. Points are in garment coordinates; the
 * renderer turns them into a smoothed stroke at draw time.
 */
@Serializable
data class BrushStroke(
    val points: List<Point>,
    val radius: Float
)

/**
 * A cut path. Closed cuts are interpreted as full cutouts; open cuts are
 * line-shaped transparency. The cut mask is intersected with the garment
 * mask by the renderer.
 */
@Serializable
@SerialName("CutLayer")
data class CutLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val opacity: Float = 1f,
    override val order: Int = 0,
    val path: VectorPath,
    val width: Float = 12f
) : EditorLayer

/*
 * Part 2 P2-P3 placeholders. Concrete shapes (mesh deformation, stitch
 * hierarchy, fabric region, etc.) are added in the next rounds. Sealing
 * them now so the document model is forward-compatible — nothing in the
 * renderer needs to know the exact fields yet.
 */
@Serializable
@SerialName("DistressLayer")
data class DistressLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val opacity: Float = 1f,
    override val order: Int = 0,
    val placeholder: Boolean = true
) : EditorLayer

@Serializable
@SerialName("PatchLayer")
data class PatchLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val opacity: Float = 1f,
    override val order: Int = 0,
    val placeholder: Boolean = true
) : EditorLayer

@Serializable
@SerialName("StitchLayer")
data class StitchLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val opacity: Float = 1f,
    override val order: Int = 0,
    val placeholder: Boolean = true
) : EditorLayer

@Serializable
@SerialName("FabricLayer")
data class FabricLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val opacity: Float = 1f,
    override val order: Int = 0,
    val placeholder: Boolean = true
) : EditorLayer
