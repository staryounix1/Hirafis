package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val city: String = "",
    val role: String = "client",
    val roleLocked: Boolean = false,
    val specialtyId: String = "plumbing",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

class FirestoreUserProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    suspend fun syncOrRestoreProfile(
        fullName: String,
        email: String,
        city: String,
        chosenRole: String,
        chosenSpecialtyId: String
    ): Result<FirestoreUserProfile> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً عبر غوغل"))
        val docPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null && existing.roleLocked) {
                    return Result.success(existing)
                }
            }

            val cleanName = fullName.trim().ifBlank { "مستخدم حرفي" }.take(80)
            val cleanEmail = email.trim().ifBlank { "user@hirafi.app" }.take(120)
            val cleanCity = city.trim().ifBlank { "الدار البيضاء" }.take(60)
            val safeRole = if (chosenRole == "craftsman") "craftsman" else "client"
            val safeSpec = chosenSpecialtyId.trim().ifBlank { "plumbing" }.take(40)

            val payload = mapOf(
                "userId" to uid,
                "fullName" to cleanName,
                "email" to cleanEmail,
                "city" to cleanCity,
                "role" to safeRole,
                "roleLocked" to true,
                "specialtyId" to safeSpec,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                FirestoreUserProfile(
                    userId = uid,
                    fullName = cleanName,
                    email = cleanEmail,
                    city = cleanCity,
                    role = safeRole,
                    roleLocked = true,
                    specialtyId = safeSpec
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, if (auth.currentUser != null) "WRITE" else "GET", docPath)
            Result.failure(e)
        }
    }

    private fun logFirestoreError(exception: Exception, operation: String, path: String) {
        val code = (exception as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
        val json = JSONObject().apply {
            put("error", exception.message ?: code)
            put("operationType", operation)
            put("path", path)
            put("code", code)
        }
        Log.e("FirestoreError", json.toString(), exception)
    }
}
