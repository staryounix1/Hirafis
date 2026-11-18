package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max

class SupabaseStorageService(
    private val context: Context,
    private val sessionManager: SupabaseSessionManager
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun uploadAvatar(localUri: String): Result<String> =
        uploadCompressedImage(
            localUri = localUri,
            remotePathBuilder = { uid -> "users/$uid/avatar_${System.currentTimeMillis()}.jpg" }
        )

    suspend fun uploadRequestPhoto(requestId: String, localUri: String): Result<String> =
        uploadCompressedImage(
            localUri = localUri,
            remotePathBuilder = { uid -> "requests/$uid/$requestId.jpg" }
        )

    suspend fun uploadPortfolioPhoto(itemId: String, localUri: String): Result<String> =
        uploadCompressedImage(
            localUri = localUri,
            remotePathBuilder = { uid -> "portfolio/$uid/$itemId.jpg" }
        )

    suspend fun uploadVerificationPhoto(slotName: String, localUri: String): Result<String> =
        uploadCompressedImage(
            localUri = localUri,
            remotePathBuilder = { uid -> "verifications/$uid/${slotName}_${System.currentTimeMillis()}.jpg" }
        )

    private suspend fun uploadCompressedImage(
        localUri: String,
        remotePathBuilder: (uid: String) -> String
    ): Result<String> {
        val trimmed = localUri.trim()
        if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return Result.success(trimmed)
        }
        if (!SupabaseConfig.isConfigured) {
            return Result.failure(IllegalStateException("إعدادات Supabase غير مكتملة"))
        }
        val uid = sessionManager.currentUidOrNull
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً لرفع الصور"))
        val bearer = sessionManager.getValidBearerToken()
            ?: return Result.failure(IllegalStateException("انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول"))

        return try {
            val bytes = withContext(Dispatchers.IO) {
                compressImageToJpegBytes(Uri.parse(trimmed))
            } ?: return Result.failure(IllegalArgumentException("تعذر قراءة الصورة المختارة من الجهاز"))

            val objectPath = remotePathBuilder(uid)
            val uploadUrl = SupabaseConfig.objectUploadUrl(objectPath)
            val req = Request.Builder()
                .url(uploadUrl)
                .post(bytes.toRequestBody("image/jpeg".toMediaType()))
                .header("apikey", SupabaseConfig.anonKey)
                .header("Authorization", bearer)
                .header("x-upsert", "true")
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(req).execute()
            }
            response.use { res ->
                if (res.isSuccessful) {
                    Result.success(SupabaseConfig.publicMediaUrl(objectPath))
                } else {
                    val errText = runCatching { res.body?.string() }.getOrNull().orEmpty()
                    Log.w(TAG, "Supabase Storage upload failed (${res.code}): $errText")
                    Result.failure(IllegalStateException("تعذر رفع الصورة إلى Supabase Storage (${res.code})"))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Supabase Storage upload error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun compressImageToJpegBytes(uri: Uri): ByteArray? {
        val resolver = context.applicationContext.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        val maxSide = max(origW, origH)
        while (maxSide / sampleSize > MAX_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val scaled = if (max(decoded.width, decoded.height) > MAX_DIMENSION_PX) {
            val scale = MAX_DIMENSION_PX.toFloat() / max(decoded.width, decoded.height).toFloat()
            val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
            val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        return ByteArrayOutputStream().use { baos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
            scaled.recycle()
            baos.toByteArray()
        }
    }

    companion object {
        private const val TAG = "SupabaseStorage"
        private const val MAX_DIMENSION_PX = 1080
        private const val JPEG_QUALITY = 82
    }
}
