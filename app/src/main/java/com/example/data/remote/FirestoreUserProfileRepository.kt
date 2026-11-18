package com.example.data.remote

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.data.model.AdminAuthorizationPolicy
import com.example.data.model.HirafiCategories
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "+212 612-345678",
    val city: String = "",
    val role: String = "client",
    val roleLocked: Boolean = false,
    val specialtyId: String = "plumbing",
    val avatarUri: String? = null,
    val bio: String = "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية",
    val experienceYears: Long = 5L,
    val basePriceDh: Long = 120L,
    val fcmToken: String = "",
    val verificationStatus: String = "not_submitted",
    val rejectionReason: String = "",
    val isSuspended: Boolean = false,
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

    fun observeMyProfile(): Flow<FirestoreUserProfile?> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        val path = "users/$uid"
        return db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(
                        FirestoreUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else null
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError, FirestoreOperationType.GET, path)
                }
            }
    }

    suspend fun fetchExistingProfileIfLocked(): Result<FirestoreUserProfile?> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً عبر غوغل"))
        val docPath = "users/$uid"
        return try {
            val snap = db.collection("users").document(uid).get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null && existing.roleLocked) {
                    return Result.success(existing)
                }
            }
            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun isEmailAuthorizedAdmin(email: String): Boolean {
        val normalized = email.trim().lowercase()
        if (normalized == AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL.lowercase()) return true
        return try {
            val snap = db.collection("authorized_admins").document(normalized).get().await()
            snap.exists()
        } catch (_: Exception) {
            false
        }
    }

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

            val safeRole = when (chosenRole) {
                "craftsman" -> "craftsman"
                "admin" -> "admin"
                else -> "client"
            }

            val cleanEmail = email.trim().lowercase().let { if (it.length < 3) "user@hirafi.app" else it }.take(120)
            if (safeRole == "admin" && !isEmailAuthorizedAdmin(cleanEmail)) {
                return Result.failure(
                    IllegalStateException(
                        "هذا الحساب ($cleanEmail) غير مصرح له كأدمين. يرجى الدخول ببريد الإدارة أو إضافته من لوحة الأدمين."
                    )
                )
            }

            val cleanName = fullName.trim().let { if (it.length < 2) "مستخدم حرفي" else it }.take(80)
            val cleanCity = city.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(60)
            val safeSpec = chosenSpecialtyId.trim().let { if (it.length < 2) "plumbing" else it }.take(40)
            val defaultBasePrice = HirafiCategories.byId(safeSpec).defaultPriceDh

            val payload = mapOf(
                "userId" to uid,
                "fullName" to cleanName,
                "email" to cleanEmail,
                "phone" to "+212 612-345678",
                "city" to cleanCity,
                "role" to safeRole,
                "roleLocked" to true,
                "specialtyId" to safeSpec,
                "bio" to "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية",
                "experienceYears" to 5,
                "basePriceDh" to defaultBasePrice,
                "verificationStatus" to "not_submitted",
                "rejectionReason" to "",
                "isSuspended" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                FirestoreUserProfile(
                    userId = uid,
                    fullName = cleanName,
                    email = cleanEmail,
                    phone = "+212 612-345678",
                    city = cleanCity,
                    role = safeRole,
                    roleLocked = true,
                    specialtyId = safeSpec,
                    basePriceDh = defaultBasePrice.toLong(),
                    verificationStatus = "not_submitted",
                    isSuspended = false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (auth.currentUser != null) FirestoreOperationType.WRITE else FirestoreOperationType.GET,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        city: String,
        avatarUri: String?,
        bio: String,
        experienceYears: Int,
        basePriceDh: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val docPath = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName.trim().let { if (it.length < 2) "مستخدم حرفي" else it }.take(80),
                "phone" to phone.trim().let { if (it.length < 6) "+212 612-345678" else it }.take(30),
                "city" to city.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(60),
                "bio" to bio.trim().take(400),
                "experienceYears" to experienceYears.coerceIn(0, 60),
                "basePriceDh" to basePriceDh.coerceIn(40, 5000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanAvatar = avatarUri?.trim()?.takeIf { it.isNotBlank() }?.take(500)
            if (cleanAvatar != null) {
                updates["avatarUri"] = cleanAvatar
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val clean = token.trim().take(500)
        if (clean.isBlank()) return Result.success(Unit)
        val docPath = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "fcmToken" to clean,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
