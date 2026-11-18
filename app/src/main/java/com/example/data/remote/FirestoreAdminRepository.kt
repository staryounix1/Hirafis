package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppRuntimeConfig
import com.example.data.model.AuthorizedAdminRecord
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.VerificationSubmission
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun currentUidOrNull(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    fun observeAppRuntimeConfig(): Flow<AppRuntimeConfig> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "app_config/runtime"
        return db.collection("app_config").document("runtime")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) {
                    AppRuntimeConfig(productionMode = false)
                } else {
                    AppRuntimeConfig(
                        productionMode = snap.getBoolean("productionMode") ?: false,
                        updatedByEmail = snap.getString("updatedByEmail") ?: "",
                        updatedAtMillis = snap.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: 0L
                    )
                }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.GET, path, uid) }
    }

    suspend fun setProductionMode(enabled: Boolean): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً"))
        val path = "app_config/runtime"
        return try {
            db.collection("app_config").document("runtime").set(
                mapOf(
                    "productionMode" to enabled,
                    "updatedByEmail" to (user.email ?: "admin").take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitVerificationRequest(
        craftsmanName: String,
        specialtyId: String,
        city: String,
        nationalIdNumber: String,
        frontPhotoUri: String,
        backPhotoUri: String,
        selfiePhotoUri: String
    ): Result<Unit> {
        val uid = currentUidOrNull()
            ?: return Result.failure(IllegalStateException("يجب تسجيل الدخول بحساب غوغل"))
        val path = "verification_submissions/$uid"
        return try {
            val payload = mapOf(
                "craftsmanId" to uid,
                "craftsmanName" to craftsmanName.trim().ifBlank { "حرفي" }.take(80),
                "specialtyId" to specialtyId.trim().ifBlank { "plumbing" }.take(40),
                "city" to city.trim().ifBlank { "الدار البيضاء" }.take(60),
                "nationalIdNumber" to nationalIdNumber.trim().take(20),
                "frontPhotoUri" to frontPhotoUri.trim().take(500),
                "backPhotoUri" to backPhotoUri.trim().take(500),
                "selfiePhotoUri" to selfiePhotoUri.trim().take(500),
                "status" to IdentityVerificationStatus.PENDING_REVIEW.dbValue,
                "rejectionReason" to "",
                "submittedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("verification_submissions").document(uid).set(payload).await()
            db.collection("users").document(uid).update(
                mapOf(
                    "verificationStatus" to IdentityVerificationStatus.PENDING_REVIEW.dbValue,
                    "rejectionReason" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeVerificationSubmissions(): Flow<List<VerificationSubmission>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "verification_submissions"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc -> doc.toVerificationSubmission() }
                    .sortedByDescending { it.updatedAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    suspend fun reviewVerificationSubmission(
        craftsmanId: String,
        approved: Boolean,
        rejectionReason: String = ""
    ): Result<Unit> {
        val adminEmail = auth.currentUser?.email ?: "admin"
        val status = if (approved) IdentityVerificationStatus.VERIFIED else IdentityVerificationStatus.REJECTED
        val reason = if (approved) "" else rejectionReason.trim().ifBlank { "الصور أو رقم البطاقة غير واضحة، يرجى إعادة الرفع" }.take(300)
        val path = "verification_submissions/$craftsmanId"
        return try {
            db.collection("verification_submissions").document(craftsmanId).update(
                mapOf(
                    "status" to status.dbValue,
                    "rejectionReason" to reason,
                    "reviewedByEmail" to adminEmail.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(craftsmanId).update(
                mapOf(
                    "verificationStatus" to status.dbValue,
                    "rejectionReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            runCatching {
                db.collection("craftsmen_directory").document(craftsmanId).update(
                    mapOf(
                        "isVerified" to approved,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<AdminUserRecord>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "users"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc -> doc.toAdminUserRecord() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observeAllServiceRequests(): Flow<List<ServiceRequest>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "service_requests"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc -> doc.toAdminServiceRequest() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    suspend fun toggleUserSuspended(
        userId: String,
        isCraftsman: Boolean,
        suspended: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            db.collection("users").document(userId).update(
                mapOf(
                    "isSuspended" to suspended,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (isCraftsman) {
                runCatching {
                    db.collection("craftsmen_directory").document(userId).update(
                        mapOf(
                            "isSuspended" to suspended,
                            "online" to !suspended,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelRequestAsAdmin(requestId: String): Result<Unit> {
        val path = "service_requests/$requestId"
        return try {
            db.collection("service_requests").document(requestId).update(
                mapOf(
                    "status" to RequestStatus.CANCELLED.dbValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAuthorizedAdmins(): Flow<List<AuthorizedAdminRecord>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "authorized_admins"
        return db.collection(path)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val email = doc.getString("email") ?: return@mapNotNull null
                    AuthorizedAdminRecord(
                        email = email,
                        addedBy = doc.getString("addedBy") ?: "",
                        createdAtMillis = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: 0L
                    )
                }.sortedBy { it.email }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    suspend fun addAuthorizedAdmin(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        if (clean.length < 4 || !clean.contains("@")) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بريد غوغل صحيح"))
        }
        val addedBy = auth.currentUser?.email ?: "admin"
        val path = "authorized_admins/$clean"
        return try {
            db.collection("authorized_admins").document(clean).set(
                mapOf(
                    "email" to clean,
                    "addedBy" to addedBy,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeAuthorizedAdmin(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        val path = "authorized_admins/$clean"
        return try {
            db.collection("authorized_admins").document(clean).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun handleFlowError(
        error: Throwable,
        operationType: FirestoreOperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private fun DocumentSnapshot.toVerificationSubmission(): VerificationSubmission? {
        val craftsmanId = getString("craftsmanId") ?: return null
        return VerificationSubmission(
            craftsmanId = craftsmanId,
            craftsmanName = getString("craftsmanName") ?: "حرفي",
            specialtyId = getString("specialtyId") ?: "plumbing",
            city = getString("city") ?: "الدار البيضاء",
            nationalIdNumber = getString("nationalIdNumber") ?: "",
            frontPhotoUri = getString("frontPhotoUri") ?: "",
            backPhotoUri = getString("backPhotoUri") ?: "",
            selfiePhotoUri = getString("selfiePhotoUri") ?: "",
            status = IdentityVerificationStatus.fromDb(getString("status")),
            rejectionReason = getString("rejectionReason") ?: "",
            reviewedByEmail = getString("reviewedByEmail") ?: "",
            submittedAtMillis = getTimestamp("submittedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time ?: 0L,
            updatedAtMillis = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time ?: 0L
        )
    }

    private fun DocumentSnapshot.toAdminUserRecord(): AdminUserRecord? {
        val userId = getString("userId") ?: return null
        return AdminUserRecord(
            userId = userId,
            fullName = getString("fullName") ?: "مستخدم",
            email = getString("email") ?: "",
            city = getString("city") ?: "الدار البيضاء",
            role = UserRole.fromDb(getString("role")),
            specialtyId = getString("specialtyId") ?: "plumbing",
            verificationStatus = IdentityVerificationStatus.fromDb(getString("verificationStatus")),
            isSuspended = getBoolean("isSuspended") ?: false,
            fcmTokenPresent = !getString("fcmToken").isNullOrBlank(),
            createdAtMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()?.time ?: 0L
        )
    }

    private fun DocumentSnapshot.toAdminServiceRequest(): ServiceRequest? {
        val clientId = getString("clientId") ?: return null
        val createdMs = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis()
        val updatedMs = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: createdMs
        val pmRaw = getString("paymentMethod") ?: PaymentMethod.CASH.dbValue
        return ServiceRequest(
            id = id,
            clientId = clientId,
            clientName = getString("clientName") ?: "زبون",
            categoryId = getString("categoryId") ?: "plumbing",
            categoryName = getString("categoryName") ?: "سباكة",
            title = getString("title") ?: "طلب خدمة",
            description = getString("description") ?: "",
            addressLabel = getString("addressLabel") ?: "الدار البيضاء",
            lat = (get("lat") as? Number)?.toDouble() ?: 33.5822,
            lng = (get("lng") as? Number)?.toDouble() ?: -7.6324,
            mapX = 0.5f,
            mapY = 0.5f,
            proposedPrice = (get("proposedPrice") as? Number)?.toInt() ?: 120,
            acceptedPrice = (get("acceptedPrice") as? Number)?.toInt(),
            paymentMethod = PaymentMethod.entries.firstOrNull { it.dbValue == pmRaw } ?: PaymentMethod.CASH,
            isUrgent = getBoolean("urgent") ?: false,
            status = RequestStatus.fromDb(getString("status") ?: RequestStatus.SEARCHING.dbValue),
            assignedCraftsmanId = getString("assignedCraftsmanId"),
            assignedCraftsmanName = getString("assignedCraftsmanName"),
            assignedCraftsmanPhone = getString("assignedCraftsmanPhone"),
            assignedCraftsmanRating = (get("assignedCraftsmanRating") as? Number)?.toDouble(),
            photoUri = getString("photoUri"),
            createdAtMillis = createdMs,
            updatedAtMillis = updatedMs
        )
    }
}
