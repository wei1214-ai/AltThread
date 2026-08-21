package com.example.myapplicationkoG

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

val supabase: SupabaseClient = createSupabaseClient(
    supabaseUrl = "https://fqyixgfokvnvpudiruej.supabase.co",
    "sb_publishable_XvUT9LxMnJ8qSJM6KVun5Q_pfUb-lUE"
){ install(Auth)}