package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

class FirebaseCloudStorageService(
    private val context: Context,
    private val storage: FirebaseStorage = Firebase.storage,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        private const val TAG = "HirafiCloudStorage"
        private const val MAX_DIMENSION_PX = 1080
        private const val JPEG_QUALITY = 82
    }

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
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً لرفع الصور"))

        return try {
            val bytes = withContext(Dispatchers.IO) {
                compressImageToJpegBytes(Uri.parse(trimmed))
            } ?: return Result.failure(IllegalArgumentException("تعذر قراءة الصورة المختارة من الجهاز"))

            val ref = storage.reference.child(remotePathBuilder(uid))
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload failed: ${e.message}", e)
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
}
