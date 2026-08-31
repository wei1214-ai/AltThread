package com.example.myapplicationkoG.domain.model

import kotlinx.serialization.Serializable

/**
 * A reference to an image asset. The bytes live outside the document
 * (in app private storage, SAF URI, or a remote URL).
 * Document only stores the reference and the intrinsic dimensions.
 */
@Serializable
data class ImageAsset(
    val id: String,
    val uri: String,
    val width: Int,
    val height: Int
)
