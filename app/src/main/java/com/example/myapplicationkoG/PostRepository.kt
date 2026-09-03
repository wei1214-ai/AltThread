package com.example.myapplicationkoG

import com.example.myapplicationkoG.ui.ProfileRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostComment(
    val id: String = "",
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String = "AltUser",
    @SerialName("username") val username: String = "AltUser",
    @SerialName("avatar_url") val avatar_url: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class SimplePostIdRow(
    @SerialName("post_id") val postId: String
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
        likeCount = 12
    )

    // Get posts with likes and favorites state
    suspend fun getPosts(
        category: String = "For You",
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
                        // 包含标题模糊匹配
                        ilike("clothing_title", "%$query%")
                    }
                    // 动态排序支持
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

    // 重构：精确同步点赞人次与数据库 like_count 字段
    suspend fun toggleLike(postId: String): Int = withContext(Dispatchers.IO) {
        try {
            // 1. 检查当前用户是否已点赞
            val userLiked = supabase.from("post_likes").select {
                filter {
                    eq("post_id", postId)
                    eq("user_id", CURRENT_USER)
                }
            }.decodeList<SimplePostIdRow>().isNotEmpty()

            // 2. 根据状态插入或删除点赞记录
            if (userLiked) {
                supabase.from("post_likes").delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", CURRENT_USER)
                    }
                }
            } else {
                supabase.from("post_likes").insert(
                    mapOf(
                        "post_id" to postId,
                        "user_id" to CURRENT_USER,
                        "username" to CURRENT_USER
                    )
                )
            }

            // 3. 计算最新真实点赞总数
            val allLikes = supabase.from("post_likes").select {
                filter { eq("post_id", postId) }
            }.decodeList<SimplePostIdRow>()
            val newLikeCount = allLikes.size

            // 4. 更新 Posts 表中的 like_count 计数器
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

    // Get comments from Supabase
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

            val username =  profile.username
                ?.ifBlank { "User" }
                ?: "User"

            val avatar_url = profile.avatar_url


            supabase.from("post_comments").insert(
                mapOf(
                    "post_id" to postId,
                    "user_id" to user.id,
                    "username" to username,
                    "avatar_url" to avatar_url,
                    "content" to content.trim()
                )
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Toggle Favorite
    suspend fun toggleFavourite(postId: String, isSaved: Boolean) = withContext(Dispatchers.IO) {
        try {
            if (isSaved) {
                supabase.from("post_favorites").insert(mapOf("post_id" to postId, "user_id" to CURRENT_USER))
            } else {
                supabase.from("post_favorites").delete {
                    filter {
                        eq("user_id", CURRENT_USER)
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
            supabase.from("post_likes").select {
                filter { eq("user_id", CURRENT_USER) }
            }.decodeList<SimplePostIdRow>().map { it.postId }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun getFavoritedPostIdsForCurrentUser(): Set<String> {
        return try {
            supabase.from("post_favorites").select {
                filter { eq("user_id", CURRENT_USER) }
            }.decodeList<SimplePostIdRow>().map { it.postId }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}