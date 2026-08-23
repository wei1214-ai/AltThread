package com.example.myapplicationkoG

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Long,
    val username: String,
    val image_url: String,
    val caption: String,
    val category: String,
    val created_at: String? = null
)