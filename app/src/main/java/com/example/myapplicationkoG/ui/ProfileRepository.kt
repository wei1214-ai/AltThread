package com.example.myapplicationkoG.ui

import com.example.myapplicationkoG.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val avatar_url: String? = null
)

class ProfileRepository {

    suspend fun getMyProfile(): UserProfile {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("No user is logged in")

        return supabase
            .from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle()
    }
}