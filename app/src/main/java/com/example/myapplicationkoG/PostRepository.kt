package com.example.myapplicationkoG

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class PostRepository {

    // Get all posts
    suspend fun getPosts(): List<Post> {
        return supabase
            .from("posts")
            .select {
                order(
                    column = "created_at",
                    order = Order.DESCENDING
                )
            }
            .decodeList<Post>()
    }

    // Get posts by category
    suspend fun getPostsByCategory(category: String): List<Post> {
        return supabase
            .from("posts")
            .select {
                filter {
                    eq(
                        column = "category",
                        value = category
                    )
                }

                order(
                    column = "created_at",
                    order = Order.DESCENDING
                )
            }
            .decodeList<Post>()
    }

    // Search posts
    suspend fun searchPosts(keyword: String): List<Post> {
        return supabase
            .from("posts")
            .select {
                filter {
                    ilike(
                        column = "caption",
                        pattern = "%$keyword%"
                    )
                }

                order(
                    column = "created_at",
                    order = Order.DESCENDING
                )
            }
            .decodeList<Post>()
    }
}