package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    const val STORAGE_BUCKET = "hirafi-media"

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

    fun publicMediaUrl(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/public/$STORAGE_BUCKET/$cleanPath"
    }

    fun objectUploadUrl(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/$STORAGE_BUCKET/$cleanPath"
    }
}
