package com.example.myapplicationkoG

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository {

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
            if (result.isEmpty()) listOf(samplePost) else result
        } catch (e: Exception) {
            listOf(samplePost) // Fallback so picture always renders during testing
        }
    }

    // Fetch posts filtered by category ("Vintage", "Streetwear", etc.)
    suspend fun getPostsByCategory(category: String): List<Post> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("posts")
                .select {
                    filter {
                        eq("clothing_category", category)
                    }
                }
                .decodeList<Post>()
            if (result.isEmpty()) listOf(samplePost.copy(clothingCategory = category)) else result
        } catch (e: Exception) {
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
            emptyList()
        }
    }
}