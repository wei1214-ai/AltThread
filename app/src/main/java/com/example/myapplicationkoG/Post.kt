package com.example.myapplicationkoG

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// 1. Main Post Model
@Serializable
data class Post(
    val id: String = "",

    @SerialName("user_id")
    val userId: String = "",

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

    @SerialName("like_count")
    val likeCount: Int = 0,

    @SerialName("created_at")
    val createdAt: String = "",

    // UI state helper fields (ignored by Supabase during direct JSON parsing)
    @Transient
    val isLikedByCurrentUser: Boolean = false,

    @Transient
    val isFavoritedByCurrentUser: Boolean = false
)

// 2. Track who liked which post
@Serializable
data class PostLike(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    val username: String? = null,
    @SerialName("user_profile_pic_url") val userProfilePicUrl: String? = null
)

// 3. Track saved/bookmarked posts for Profile
@Serializable
data class PostFavorite(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String
)