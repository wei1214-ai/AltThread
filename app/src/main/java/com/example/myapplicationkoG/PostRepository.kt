package com.example.myapplicationkoG

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
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

    // 临时测试用户，之后可替换为真实登录的 User ID/Name
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

    // 1. 获取所有帖子 (支持分类 Category + 排序 Sort: latest 或 highest_likes)
    suspend fun getPosts(
        category: String = "For You",
        sortBy: String = "latest"
    ): List<Post> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("posts")
                .select {
                    if (category != "For You") {
                        filter {
                            eq("clothing_category", category)
                        }
                    }
                    if (sortBy == "highest_likes") {
                        order(column = "like_count", order = Order.DESCENDING)
                    } else {
                        order(column = "created_at", order = Order.DESCENDING)
                    }
                }
                .decodeList<Post>()
            result.ifEmpty { listOf(samplePost.copy(clothingCategory = category)) }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(samplePost.copy(clothingCategory = category))
        }
    }

    // 2. 按关键词搜索帖子
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

    // 3. 检查当前用户是否已点赞
    suspend fun isPostLiked(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("post_likes").select {
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

    // 4. 点赞 / 取消点赞 (同步更新 post_likes 表和 posts 表的点赞总数)
    suspend fun toggleLike(postId: String, isLiked: Boolean, newCount: Int) = withContext(Dispatchers.IO) {
        try {
            if (isLiked) {
                // 写入点赞记录
                val likeData = PostLike(postId = postId, userId = CURRENT_USER, username = CURRENT_USER)
                supabase.from("post_likes").insert(likeData)
            } else {
                // 删除点赞记录
                supabase.from("post_likes").delete {
                    filter {
                        eq("user_id", CURRENT_USER)
                        eq("post_id", postId)
                    }
                }
            }

            // 更新 posts 表的 like_count
            supabase.from("posts").update(
                mapOf("like_count" to newCount)
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

    // 5. 获取点赞该帖子的所有用户列表 (像 IG 一样查看谁点了赞)
    suspend fun getUsersWhoLikedPost(postId: String): List<PostLike> = withContext(Dispatchers.IO) {
        try {
            supabase.from("post_likes").select {
                filter {
                    eq("post_id", postId)
                }
            }.decodeList<PostLike>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 6. 检查当前用户是否已收藏
    suspend fun isPostBookmarked(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("post_favorites").select {
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

    // 7. 收藏 / 取消收藏 状态切换
    suspend fun toggleFavourite(postId: String, isSaved: Boolean) = withContext(Dispatchers.IO) {
        try {
            if (isSaved) {
                supabase.from("post_favorites").insert(
                    mapOf(
                        "user_id" to CURRENT_USER,
                        "post_id" to postId
                    )
                )
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
            throw e
        }
    }

    // 8. 获取当前用户收藏的所有帖子 (展示在 Profile 界面)
    suspend fun getFavouritePosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val favRows = supabase.from("post_favorites").select {
                filter {
                    eq("user_id", CURRENT_USER)
                }
            }.decodeList<FavouriteRow>()

            val savedIds = favRows.map { it.postId }.toSet()

            if (savedIds.isEmpty()) return@withContext emptyList()

            val allPosts = supabase.from("posts").select().decodeList<Post>()
            allPosts.filter { post -> savedIds.contains(post.id) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}