package com.example.myapplicationkoG

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String = "",
    val username: String = "AltUser",

    @SerialName("user_profile_pic_url")
    val userProfilePicUrl: String? = null,

    @SerialName("media_url")
    val mediaUrl: String = "",

    @SerialName("is_video")
    val isVideo: Boolean = false,

    @SerialName("clothing_title")
    val clothingTitle: String = "",

    @SerialName("clothing_category")
    val clothingCategory: String = "For You",

    val caption: String = "",

    @SerialName("initial_like_count")
    val initialLikeCount: Int = 0
)