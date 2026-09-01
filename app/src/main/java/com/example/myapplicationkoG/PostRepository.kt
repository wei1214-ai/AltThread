package com.example.myapplicationkoG

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FavouriteRow(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
private data class LikeRow(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String? = null
)

class PostRepository {

    private val CURRENT_USER = "AltUser"

    private val samplePost = Post(
        id = "1",
        username = "AltUser",
        userProfilePicUrl = "https://via.placeholder.com/150",
        mediaUrl = "https://fqyixgfokvnvpudiruej.supabase.co/storage/v1/object/public/cloth/althread.jpg",
        isVideo = false,
        clothingTitle = "Streetwear Outfit Set",
        clothingCategory = "For You",
        caption = "Sharing my latest clothing set from AltThread!",
        initialLikeCount = 12
    )

    // Fetch all posts ("For You")
    suspend fun getPosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("posts")
                .select()
                .decodeList<Post>()
            result.ifEmpty { listOf(samplePost) }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(samplePost)
        }
    }

    // Fetch posts filtered by category
    suspend fun getPostsByCategory(category: String): List<Post> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("posts")
                .select {
                    filter {
                        eq("clothing_category", category)
                    }
                }
                .decodeList<Post>()
            result.ifEmpty { listOf(samplePost.copy(clothingCategory = category)) }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(samplePost.copy(clothingCategory = category))
        }
    }

    // Search posts by keyword
    suspend fun searchPosts(keyword: String): List<Post> = withContext(Dispatchers.IO) {
        try {
            supabase.from("posts")
                .select {
                    filter {
                        ilike("clothing_title", "%$keyword%")
                    }
                }
                .decodeList<Post>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Toggle Favourite Status in Supabase
    suspend fun toggleFavourite(postId: String, isSaved: Boolean) = withContext(Dispatchers.IO) {
        try {
            if (isSaved) {
                supabase.from("favourites").insert(
                    mapOf(
                        "user_id" to CURRENT_USER,
                        "post_id" to postId
                    )
                )
            } else {
                supabase.from("favourites").delete {
                    filter {
                        eq("user_id", CURRENT_USER)
                        eq("post_id", postId)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // Check if a post is already bookmarked
    suspend fun isPostBookmarked(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("favourites").select {
                filter {
                    eq("user_id", CURRENT_USER)
                    eq("post_id", postId)
                }
            }.decodeList<FavouriteRow>()
            result.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fetch posts saved as favourites by current user (FIXED)
    suspend fun getFavouritePosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch saved rows specifically for CURRENT_USER
            val favRows = supabase.from("favourites").select {
                filter {
                    eq("user_id", CURRENT_USER)
                }
            }.decodeList<FavouriteRow>()

            val savedIds = favRows.map { it.postId }.toSet()

            if (savedIds.isEmpty()) return@withContext emptyList()

            // 2. Fetch all posts and filter in Kotlin directly to prevent SQL type-casting issues
            val allPosts = supabase.from("posts").select().decodeList<Post>()
            allPosts.filter { post -> savedIds.contains(post.id) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Check if user has liked a post
    suspend fun isPostLiked(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("likes").select {
                filter {
                    eq("user_id", CURRENT_USER)
                    eq("post_id", postId)
                }
            }.decodeList<LikeRow>()
            result.isNotEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Toggle Like Status and sync count in Supabase
    suspend fun toggleLike(postId: String, isLiked: Boolean, newCount: Int) = withContext(Dispatchers.IO) {
        try {
            if (isLiked) {
                supabase.from("likes").insert(
                    mapOf(
                        "user_id" to CURRENT_USER,
                        "post_id" to postId
                    )
                )
            } else {
                supabase.from("likes").delete {
                    filter {
                        eq("user_id", CURRENT_USER)
                        eq("post_id", postId)
                    }
                }
            }

            // Update total like count on the post
            supabase.from("posts").update(
                mapOf("initial_like_count" to newCount)
            ) {
                filter {
                    eq("id", postId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}