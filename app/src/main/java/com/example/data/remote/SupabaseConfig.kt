package com.example.data.remote

import com.example.BuildConfig

object SupabaseConfig {
    const val STORAGE_BUCKET = "hirafi-media"
    const val KYC_BUCKET = "hirafi-kyc"
    const val KYC_PRIVATE_REF_PREFIX = "kyc://hirafi-kyc/"

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

    fun kycObjectUploadUrl(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/$KYC_BUCKET/$cleanPath"
    }

    fun privateKycReference(objectPath: String): String {
        val cleanPath = objectPath.trimStart('/')
        return "$KYC_PRIVATE_REF_PREFIX$cleanPath"
    }

    fun signObjectUrl(bucket: String, objectPath: String): String {
        val cleanBucket = bucket.trim('/')
        val cleanPath = objectPath.trimStart('/')
        return "$rawUrl/storage/v1/object/sign/$cleanBucket/$cleanPath"
    }

    fun toAbsoluteStorageUrl(pathOrUrl: String): String {
        val clean = pathOrUrl.trim()
        if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            return clean
        }
        val normalizedPath = if (clean.startsWith("/storage/v1/")) {
            clean
        } else if (clean.startsWith("/")) {
            "/storage/v1$clean"
        } else {
            "/storage/v1/$clean"
        }
        return "$rawUrl$normalizedPath"
    }

    fun parsePrivateKycReference(uri: String): Pair<String, String>? {
        val trimmed = uri.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith(KYC_PRIVATE_REF_PREFIX)) {
            val path = trimmed.removePrefix(KYC_PRIVATE_REF_PREFIX).trimStart('/')
            return if (path.isNotBlank()) KYC_BUCKET to path else null
        }
        val authenticatedMarker = "/storage/v1/object/authenticated/$KYC_BUCKET/"
        val authIdx = trimmed.indexOf(authenticatedMarker)
        if (authIdx >= 0) {
            val path = trimmed.substring(authIdx + authenticatedMarker.length).substringBefore('?').trimStart('/')
            return if (path.isNotBlank()) KYC_BUCKET to path else null
        }
        val legacyPublicMarker = "/storage/v1/object/public/$STORAGE_BUCKET/verifications/"
        val legacyIdx = trimmed.indexOf(legacyPublicMarker)
        if (legacyIdx >= 0) {
            val path = "verifications/" + trimmed.substring(legacyIdx + legacyPublicMarker.length).substringBefore('?').trimStart('/')
            return STORAGE_BUCKET to path
        }
        return null
    }
}
