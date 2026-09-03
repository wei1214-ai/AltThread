package com.example.myapplicationkoG

import android.content.Context
import android.net.Uri
import com.example.myapplicationkoG.ui.ProfileRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PostComment(
    val id: String = "",
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("avatar_url") val avatar_url: String? = null,
    @SerialName("comment_text") val content: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class SimplePostIdRow(
    @SerialName("post_id") val postId: String
)

@Serializable
private data class NewPost(
    @SerialName("user_id")
    val userId: String,

    val username: String,

    @SerialName("user_profile_pic_url")
    val userProfilePicUrl: String?,

    // Primary single image URL (for backward compatibility)
    @SerialName("media_url")
    val mediaUrl: String,

    // Array/List of image URLs (supports 1-9 photos)
    @SerialName("media_urls")
    val mediaUrls: List<String> = emptyList(),

    @SerialName("is_video")
    val isVideo: Boolean = false,

    @SerialName("clothing_title")
    val clothingTitle: String,

    @SerialName("clothing_category")
    val clothingCategory: String,

    @SerialName("post_type")
    val postType: String,

    val caption: String,

    @SerialName("like_count")
    val likeCount: Int = 0
)

class PostRepository {

    private val samplePost = Post(
        id = "1",
        username = "AltUser",
        userProfilePicUrl = "https://via.placeholder.com/150",
        mediaUrl = "https://fqyixgfokvnvpudiruej.supabase.co/storage/v1/object/public/cloth/althread.jpg",
        mediaUrls = listOf("https://fqyixgfokvnvpudiruej.supabase.co/storage/v1/object/public/cloth/althread.jpg"),
        isVideo = false,
        clothingTitle = "Streetwear Outfit Set",
        clothingCategory = "Trend",
        caption = "Sharing my latest clothing set from AltThread!",
        likeCount = 12
    )

    // Helper to retrieve currently authenticated user ID safely
    private fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    // Get posts with real-time likes and favorites state
    suspend fun getPosts(
        category: String = "All",
        sortBy: String = "latest"
    ): List<Post> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("posts")
                .select {
                    if (category != "For You" && category != "All") {
                        filter { eq("clothing_category", category) }
                    }
                    if (sortBy == "highest_likes") {
                        order(column = "like_count", order = Order.DESCENDING)
                    } else {
                        order(column = "created_at", order = Order.DESCENDING)
                    }
                }
                .decodeList<Post>()

            val postsList = result.ifEmpty { listOf(samplePost.copy(clothingCategory = category)) }
            val userLikedIds = getLikedPostIdsForCurrentUser()
            val userFavoritedIds = getFavoritedPostIdsForCurrentUser()

            postsList.map { post ->
                post.copy(
                    isLikedByCurrentUser = userLikedIds.contains(post.id),
                    isFavoritedByCurrentUser = userFavoritedIds.contains(post.id)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(samplePost.copy(clothingCategory = category))
        }
    }

    // Search posts by keyword and support sorting (Latest vs Most Liked)
    suspend fun searchPosts(query: String, sortBy: String = "latest"): List<Post> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getPosts(category = "All", sortBy = sortBy)
        try {
            val result = supabase.from("posts")
                .select {
                    filter {
                        ilike("caption", "%$query%")
                    }
                    if (sortBy == "highest_likes") {
                        order(column = "like_count", order = Order.DESCENDING)
                    } else {
                        order(column = "created_at", order = Order.DESCENDING)
                    }
                }
                .decodeList<Post>()

            val userLikedIds = getLikedPostIdsForCurrentUser()
            val userFavoritedIds = getFavoritedPostIdsForCurrentUser()

            result.map { post ->
                post.copy(
                    isLikedByCurrentUser = userLikedIds.contains(post.id),
                    isFavoritedByCurrentUser = userFavoritedIds.contains(post.id)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Fetch all posts saved/favorited by the current user
    suspend fun getFavouritePosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val favoritedPostIds = getFavoritedPostIdsForCurrentUser()
            if (favoritedPostIds.isEmpty()) return@withContext emptyList()

            val allPosts = getPosts(category = "All")
            allPosts.filter { post -> favoritedPostIds.contains(post.id) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPostCount(userId: String): Int {
        return try {
            supabase.from("posts")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Post>()
                .size
        } catch (e: Exception) {
            0
        }
    }

    /** Helper: Uploads a single Uri to Supabase storage bucket and returns its public URL */
    private suspend fun uploadSingleImage(
        context: Context,
        uri: Uri,
        userId: String
    ): String = withContext(Dispatchers.IO) {
        val imageBytes = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: error("Could not read image URI: $uri")

        val imagePath = "$userId/${UUID.randomUUID()}.jpg"
        val bucket = supabase.storage.from("cloth")
        bucket.upload(imagePath, imageBytes)
        return@withContext bucket.publicUrl(imagePath)
    }

    /**
     * Multi-image enabled post creation function.
     */
    suspend fun createPost(
        context: Context,
        imageUris: List<Uri>,
        title: String,
        category: String,
        bio: String,
        postType: String = "Post",
        isChallenge: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                error("Please select at least one image.")
            }

            val user = supabase.auth.currentUserOrNull()
                ?: error("Please log in before posting.")
            val profile = ProfileRepository().getMyProfile()

            // ⚡ 并行异步上传多张图片
            val uploadedPublicUrls = imageUris.map { uri ->
                async { uploadSingleImage(context, uri, user.id) }
            }.awaitAll()

            val primaryMediaUrl = uploadedPublicUrls.firstOrNull() ?: ""

            val newPost = NewPost(
                userId = user.id,
                username = profile.username?.ifBlank { "User" } ?: "User",
                userProfilePicUrl = profile.avatar_url,
                mediaUrl = primaryMediaUrl,
                mediaUrls = uploadedPublicUrls,
                isVideo = false,
                clothingTitle = title.trim(),
                clothingCategory = category,
                postType = if (isChallenge) "Challenge" else postType,
                caption = bio.trim(),
                likeCount = 0
            )

            supabase.from("posts").insert(newPost)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /** Overloaded single-image createPost function for backward compatibility */
    suspend fun createPost(
        context: Context,
        imageUri: Uri,
        title: String,
        category: String,
        bio: String,
        isChallenge: Boolean
    ): Boolean {
        return createPost(
            context = context,
            imageUris = listOf(imageUri),
            title = title,
            category = category,
            bio = bio,
            postType = if (isChallenge) "Challenge" else "Post",
            isChallenge = isChallenge
        )
    }

    // Dynamically toggle like status based on authenticated user
    suspend fun toggleLike(postId: String): Int = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext -1

            // 1. Check if authenticated user already liked the post
            val userLiked = supabase.from("post_likes").select {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }.decodeList<SimplePostIdRow>().isNotEmpty()

            // 2. Add or remove like entry
            if (userLiked) {
                supabase.from("post_likes").delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
            } else {
                val profile = ProfileRepository().getMyProfile()
                val username = profile.username?.ifBlank { "User" } ?: "User"

                supabase.from("post_likes").insert(
                    mapOf(
                        "post_id" to postId,
                        "user_id" to userId,
                        "username" to username
                    )
                )
            }

            // 3. Query total count for updated post
            val allLikes = supabase.from("post_likes").select {
                filter { eq("post_id", postId) }
            }.decodeList<SimplePostIdRow>()
            val newLikeCount = allLikes.size

            // 4. Persist count back to the post row
            supabase.from("posts").update(
                mapOf("like_count" to newLikeCount)
            ) {
                filter { eq("id", postId) }
            }

            newLikeCount
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    // Fetch comments from Supabase
    suspend fun getComments(postId: String): List<PostComment> = withContext(Dispatchers.IO) {
        try {
            supabase.from("post_comments").select {
                filter { eq("post_id", postId) }
            }.decodeList<PostComment>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Add new comment
    suspend fun addComment(postId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull()
                ?: error("Please log in before commenting.")

            val profile = ProfileRepository().getMyProfile()

            val username = profile.username
                ?.ifBlank { "User" }
                ?: "User"

            val avatar_url = profile.avatar_url

            supabase.from("post_comments").insert(
                mapOf(
                    "post_id" to postId,
                    "user_id" to user.id,
                    "username" to username,
                    "avatar_url" to avatar_url,
                    "comment_text" to content.trim()
                )
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Toggle Favorite dynamically
    suspend fun toggleFavourite(postId: String, isSaved: Boolean) = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: return@withContext
            if (isSaved) {
                supabase.from("post_favorites").insert(
                    mapOf("post_id" to postId, "user_id" to userId)
                )
            } else {
                supabase.from("post_favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getLikedPostIdsForCurrentUser(): Set<String> {
        return try {
            val userId = getCurrentUserId() ?: return emptySet()
            supabase.from("post_likes").select {
                filter { eq("user_id", userId) }
            }.decodeList<SimplePostIdRow>().map { it.postId }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun getFavoritedPostIdsForCurrentUser(): Set<String> {
        return try {
            val userId = getCurrentUserId() ?: return emptySet()
            supabase.from("post_favorites").select {
                filter { eq("user_id", userId) }
            }.decodeList<SimplePostIdRow>().map { it.postId }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}