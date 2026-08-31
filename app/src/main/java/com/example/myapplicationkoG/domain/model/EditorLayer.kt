package com.example.myapplicationkoG.domain.model

/**
 * Base contract for all editor layers.
 * Part 1 only defines the contract. Concrete layer types
 * (Dye, Cut, Distress, Patch, Stitch, Fabric) are Part 2.
 */
sealed interface EditorLayer {
    val id: String
    val visible: Boolean
    val opacity: Float
    val order: Int
}
