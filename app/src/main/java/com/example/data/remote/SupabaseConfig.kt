package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    val rawUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = rawUrl.startsWith("https://") &&
            !rawUrl.contains("YOUR_SUPABASE_URL") &&
            anonKey.isNotBlank() &&
            !anonKey.contains("YOUR_SUPABASE_ANON_KEY")

    val baseUrlWithSlash: String
        get() = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
}
