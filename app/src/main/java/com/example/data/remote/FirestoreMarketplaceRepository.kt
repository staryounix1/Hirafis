package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.CraftsmanPin
import com.example.data.model.HirafiCategories
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PortfolioItem
import com.example.data.model.RequestStatus
import com.example.data.model.ReviewRecord
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class FirestoreMarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    val isAuthenticated: Boolean
        get() = !auth.currentUser?.uid.isNullOrBlank()

    fun currentUidOrNull(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun requireUserId(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // -------------------------------------------------------------------------
    // Real-time Snapshot Flows (Auth-Gated & Query-Aligned)
    // -------------------------------------------------------------------------

    fun observeClientRequests(): Flow<List<ServiceRequest>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "service_requests"
        return db.collection(path)
            .whereEqualTo("clientId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> doc.toServiceRequest() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observeCraftsmanAssignedRequests(): Flow<List<ServiceRequest>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "service_requests"
        return db.collection(path)
            .whereEqualTo("assignedCraftsmanId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> doc.toServiceRequest() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observeSearchingRequests(specialtyId: String): Flow<List<ServiceRequest>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "service_requests"
        return db.collection(path)
            .whereEqualTo("status", RequestStatus.SEARCHING.dbValue)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> doc.toServiceRequest() }
                    .filter { it.categoryId == specialtyId }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observeRequestLiveCoords(requestId: String): Flow<Pair<Double, Double>?> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        if (requestId.isBlank()) return emptyFlow()
        val path = "service_requests/$requestId"
        return db.collection("service_requests").document(requestId)
            .snapshots()
            .map { doc ->
                if (!doc.exists()) null
                else {
                    val lat = (doc.get("craftsmanLat") as? Number)?.toDouble()
                    val lng = (doc.get("craftsmanLng") as? Number)?.toDouble()
                    if (lat != null && lng != null) lat to lng else null
                }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.GET, path, uid) }
    }

    fun observeOffersForRequest(requestId: String, role: UserRole): Flow<List<ServiceOffer>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        if (requestId.isBlank()) return emptyFlow()
        val path = "service_requests/$requestId/offers"
        val filterField = if (role == UserRole.CLIENT) "clientId" else "craftsmanId"
        return db.collection("service_requests")
            .document(requestId)
            .collection("offers")
            .whereEqualTo(filterField, uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> doc.toServiceOffer() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observeMessagesForRequest(requestId: String): Flow<List<ChatMessage>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        if (requestId.isBlank()) return emptyFlow()
        val path = "service_requests/$requestId/messages"
        return db.collection("service_requests")
            .document(requestId)
            .collection("messages")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> doc.toChatMessage() }
                    .sortedBy { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observeReviews(role: UserRole): Flow<List<ReviewRecord>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "reviews"
        val filterField = if (role == UserRole.CLIENT) "clientId" else "craftsmanId"
        return db.collection(path)
            .whereEqualTo(filterField, uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> doc.toReviewRecord() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observeOnlineCraftsmen(centerLat: Double, centerLng: Double): Flow<List<CraftsmanPin>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        val path = "craftsmen_directory"
        return db.collection(path)
            .whereEqualTo("online", true)
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    .filter { it.getBoolean("isSuspended") != true }
                    .mapNotNull { doc ->
                        doc.toCraftsmanPin(centerLat, centerLng)
                    }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    fun observePortfolioForCraftsman(craftsmanId: String): Flow<List<PortfolioItem>> {
        val uid = currentUidOrNull() ?: return emptyFlow()
        if (craftsmanId.isBlank()) return emptyFlow()
        val path = "craftsmen_directory/$craftsmanId/portfolio"
        return db.collection("craftsmen_directory")
            .document(craftsmanId)
            .collection("portfolio")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> doc.toPortfolioItem() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error -> handleFlowError(error, FirestoreOperationType.LIST, path, uid) }
    }

    suspend fun fetchPortfolioOnce(craftsmanId: String): Result<List<PortfolioItem>> {
        val uid = currentUidOrNull() ?: return Result.success(emptyList())
        if (craftsmanId.isBlank()) return Result.success(emptyList())
        val path = "craftsmen_directory/$craftsmanId/portfolio"
        return try {
            val snap = db.collection("craftsmen_directory")
                .document(craftsmanId)
                .collection("portfolio")
                .get()
                .await()
            val list = snap.documents.mapNotNull { it.toPortfolioItem() }
                .sortedByDescending { it.createdAtMillis }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (auth.currentUser?.uid == uid) {
                handleFirestoreError(e, FirestoreOperationType.LIST, path)
            }
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Authenticated Mutations (Result<T> with Bounds Validation)
    // -------------------------------------------------------------------------

    suspend fun createServiceRequest(req: ServiceRequest): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val path = "service_requests/${req.id}"
        return try {
            val payload = mutableMapOf<String, Any>(
                "clientId" to uid,
                "clientName" to req.clientName.trim().ifBlank { "زبون حرفي" }.take(80),
                "categoryId" to req.categoryId.trim().let { if (it.length < 2) "plumbing" else it }.take(40),
                "categoryName" to req.categoryName.trim().let { if (it.length < 2) "سباكة" else it }.take(60),
                "title" to req.title.trim().let { if (it.length < 2) "طلب خدمة" else it }.take(140),
                "description" to req.description.trim().take(600),
                "addressLabel" to req.addressLabel.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(160),
                "lat" to req.lat.coerceIn(-90.0, 90.0),
                "lng" to req.lng.coerceIn(-180.0, 180.0),
                "proposedPrice" to req.proposedPrice.coerceIn(40, 10000),
                "paymentMethod" to req.paymentMethod.dbValue,
                "urgent" to req.isUrgent,
                "status" to RequestStatus.SEARCHING.dbValue,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            req.photoUri?.trim()?.takeIf { it.isNotBlank() }?.take(500)?.let {
                payload["photoUri"] = it
            }
            db.collection("service_requests").document(req.id).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProposedPrice(requestId: String, newPrice: Int): Result<Unit> {
        if (!isAuthenticated) return Result.success(Unit)
        val path = "service_requests/$requestId"
        return try {
            requireUserId()
            db.collection("service_requests").document(requestId).update(
                mapOf(
                    "proposedPrice" to newPrice.coerceIn(40, 10000),
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

    suspend fun acceptOffer(offer: ServiceOffer): Result<Unit> {
        if (!isAuthenticated) return Result.success(Unit)
        val reqPath = "service_requests/${offer.requestId}"
        return try {
            requireUserId()
            db.collection("service_requests").document(offer.requestId).update(
                mapOf(
                    "status" to RequestStatus.ACCEPTED.dbValue,
                    "acceptedPrice" to offer.offeredPrice.coerceIn(40, 10000),
                    "assignedCraftsmanId" to offer.craftsmanId.take(128),
                    "assignedCraftsmanName" to offer.craftsmanName.trim().ifBlank { "حرفي" }.take(80),
                    "assignedCraftsmanPhone" to offer.craftsmanPhone.trim().take(30),
                    "assignedCraftsmanRating" to offer.craftsmanRating.coerceIn(1.0, 5.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val offerDoc = db.collection("service_requests")
                .document(offer.requestId)
                .collection("offers")
                .document(offer.id)
            runCatching {
                offerDoc.update(
                    mapOf(
                        "status" to OfferStatus.ACCEPTED.dbValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, reqPath)
            Result.failure(e)
        }
    }

    suspend fun directAcceptRequestAsCraftsman(
        requestId: String,
        acceptedPrice: Int,
        session: UserSession
    ): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val reqPath = "service_requests/$requestId"
        return try {
            db.collection("service_requests").document(requestId).update(
                mapOf(
                    "status" to RequestStatus.ACCEPTED.dbValue,
                    "acceptedPrice" to acceptedPrice.coerceIn(40, 10000),
                    "assignedCraftsmanId" to uid,
                    "assignedCraftsmanName" to session.fullName.trim().ifBlank { "حرفي" }.take(80),
                    "assignedCraftsmanPhone" to session.phone.trim().take(30),
                    "assignedCraftsmanRating" to 4.9,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, reqPath)
            Result.failure(e)
        }
    }

    suspend fun advanceRequestStatus(requestId: String, nextStatus: RequestStatus): Result<Unit> {
        if (!isAuthenticated) return Result.success(Unit)
        val path = "service_requests/$requestId"
        return try {
            requireUserId()
            db.collection("service_requests").document(requestId).update(
                mapOf(
                    "status" to nextStatus.dbValue,
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

    suspend fun updateCraftsmanLiveLocationOnRequest(
        requestId: String,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        if (!isAuthenticated) return Result.success(Unit)
        val path = "service_requests/$requestId"
        return try {
            requireUserId()
            db.collection("service_requests").document(requestId).update(
                mapOf(
                    "craftsmanLat" to lat.coerceIn(-90.0, 90.0),
                    "craftsmanLng" to lng.coerceIn(-180.0, 180.0),
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

    suspend fun submitOffer(offer: ServiceOffer, clientId: String): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val path = "service_requests/${offer.requestId}/offers/${offer.id}"
        return try {
            val payload = mapOf(
                "requestId" to offer.requestId,
                "clientId" to clientId,
                "craftsmanId" to uid,
                "craftsmanName" to offer.craftsmanName.trim().ifBlank { "حرفي" }.take(80),
                "craftsmanPhone" to offer.craftsmanPhone.trim().let { if (it.length < 4) "+212 612-345678" else it }.take(30),
                "craftsmanRating" to offer.craftsmanRating.coerceIn(1.0, 5.0),
                "completedJobs" to offer.completedJobs.coerceIn(0, 100000),
                "specialty" to offer.specialty.trim().let { if (it.length < 2) "حرفي" else it }.take(60),
                "offeredPrice" to offer.offeredPrice.coerceIn(40, 10000),
                "etaMinutes" to offer.etaMinutes.coerceIn(1, 240),
                "distanceKm" to offer.distanceKm.coerceIn(0.0, 500.0),
                "note" to offer.note.trim().take(300),
                "status" to OfferStatus.PENDING.dbValue,
                "expiresAtMillis" to offer.expiresAtMillis.coerceAtLeast(1L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("service_requests")
                .document(offer.requestId)
                .collection("offers")
                .document(offer.id)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOfferStatusOrCounter(
        requestId: String,
        offerId: String,
        status: OfferStatus,
        offeredPrice: Int,
        note: String,
        expiresAtMillis: Long
    ): Result<Unit> {
        if (!isAuthenticated) return Result.success(Unit)
        val path = "service_requests/$requestId/offers/$offerId"
        return try {
            requireUserId()
            db.collection("service_requests")
                .document(requestId)
                .collection("offers")
                .document(offerId)
                .update(
                    mapOf(
                        "status" to status.dbValue,
                        "offeredPrice" to offeredPrice.coerceIn(40, 10000),
                        "note" to note.trim().take(300),
                        "expiresAtMillis" to expiresAtMillis.coerceAtLeast(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        message: ChatMessage,
        clientId: String,
        assignedCraftsmanId: String?
    ): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val path = "service_requests/${message.requestId}/messages/${message.id}"
        val cleanText = message.text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.success(Unit)
        return try {
            val memberUids = listOfNotNull(
                clientId.takeIf { it.isNotBlank() },
                assignedCraftsmanId?.takeIf { it.isNotBlank() },
                uid
            ).distinct().take(4)

            val payload = mapOf(
                "requestId" to message.requestId,
                "memberUids" to memberUids,
                "senderId" to uid,
                "senderName" to message.senderName.trim().ifBlank { "مستخدم" }.take(80),
                "senderRole" to message.senderRole.dbValue,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("service_requests")
                .document(message.requestId)
                .collection("messages")
                .document(message.id)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReview(review: ReviewRecord): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val path = "reviews/${review.id}"
        return try {
            val payload = mapOf(
                "requestId" to review.requestId,
                "clientId" to uid,
                "craftsmanId" to review.craftsmanId.take(128),
                "rating" to review.rating.coerceIn(1, 5),
                "tags" to review.tags.take(8),
                "comment" to review.comment.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reviews").document(review.id).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertCraftsmanDirectory(
        session: UserSession,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        if (session.role != UserRole.CRAFTSMAN) return Result.success(Unit)
        val path = "craftsmen_directory/$uid"
        return try {
            val docRef = db.collection("craftsmen_directory").document(uid)
            val snap = docRef.get().await()
            val cleanName = session.fullName.trim().let { if (it.length < 2) "حرفي معتمد" else it }.take(80)
            val cleanCity = session.city.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(60)
            val cleanBio = session.bio.trim().take(400)
            val cleanAvatar = session.avatarUri?.trim()?.takeIf { it.isNotBlank() }?.take(500)

            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "city" to cleanCity,
                    "bio" to cleanBio,
                    "experienceYears" to session.experienceYears.coerceIn(0, 60),
                    "basePriceDh" to session.basePriceDh.coerceIn(40, 5000),
                    "lat" to lat.coerceIn(-90.0, 90.0),
                    "lng" to lng.coerceIn(-180.0, 180.0),
                    "online" to session.isCraftsmanOnline,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanAvatar != null) {
                    updates["avatarUri"] = cleanAvatar
                }
                docRef.update(updates).await()
            } else {
                val category = HirafiCategories.byId(session.craftsmanSpecialtyId)
                val payload = mutableMapOf<String, Any>(
                    "craftsmanId" to uid,
                    "displayName" to cleanName,
                    "specialtyId" to category.id,
                    "specialtyName" to category.nameAr,
                    "city" to cleanCity,
                    "bio" to cleanBio,
                    "experienceYears" to session.experienceYears.coerceIn(0, 60),
                    "rating" to 4.9,
                    "completedJobs" to 0,
                    "basePriceDh" to session.basePriceDh.coerceIn(40, 5000),
                    "lat" to lat.coerceIn(-90.0, 90.0),
                    "lng" to lng.coerceIn(-180.0, 180.0),
                    "online" to (session.isCraftsmanOnline && !session.isSuspended),
                    "isVerified" to session.isCraftsmanVerified,
                    "isSuspended" to session.isSuspended,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanAvatar != null) {
                    payload["avatarUri"] = cleanAvatar
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addPortfolioItem(item: PortfolioItem): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val path = "craftsmen_directory/$uid/portfolio/${item.id}"
        return try {
            val payload = mutableMapOf<String, Any>(
                "craftsmanId" to uid,
                "title" to item.title.trim().let { if (it.length < 2) "عمل منجز" else it }.take(80),
                "description" to item.description.trim().take(350),
                "categoryName" to item.categoryName.trim().let { if (it.length < 2) "حرفي" else it }.take(60),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            item.photoUri?.trim()?.takeIf { it.isNotBlank() }?.take(500)?.let {
                payload["photoUri"] = it
            }
            db.collection("craftsmen_directory")
                .document(uid)
                .collection("portfolio")
                .document(item.id)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePortfolioItem(itemId: String): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val path = "craftsmen_directory/$uid/portfolio/$itemId"
        return try {
            db.collection("craftsmen_directory")
                .document(uid)
                .collection("portfolio")
                .document(itemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // DocumentSnapshot Mappers & Flow Error Helper
    // -------------------------------------------------------------------------

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

    private fun DocumentSnapshot.toServiceRequest(): ServiceRequest? {
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

    private fun DocumentSnapshot.toServiceOffer(): ServiceOffer? {
        val craftsmanId = getString("craftsmanId") ?: return null
        val createdMs = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis()
        return ServiceOffer(
            id = id,
            requestId = getString("requestId") ?: "",
            craftsmanId = craftsmanId,
            craftsmanName = getString("craftsmanName") ?: "حرفي",
            craftsmanPhone = getString("craftsmanPhone") ?: "",
            craftsmanRating = (get("craftsmanRating") as? Number)?.toDouble() ?: 4.9,
            completedJobs = (get("completedJobs") as? Number)?.toInt() ?: 0,
            specialty = getString("specialty") ?: "حرفي",
            offeredPrice = (get("offeredPrice") as? Number)?.toInt() ?: 120,
            etaMinutes = (get("etaMinutes") as? Number)?.toInt() ?: 10,
            distanceKm = (get("distanceKm") as? Number)?.toDouble() ?: 1.0,
            note = getString("note") ?: "",
            status = OfferStatus.fromDb(getString("status") ?: OfferStatus.PENDING.dbValue),
            createdAtMillis = createdMs,
            expiresAtMillis = (get("expiresAtMillis") as? Number)?.toLong() ?: (createdMs + 60_000L)
        )
    }

    private fun DocumentSnapshot.toChatMessage(): ChatMessage? {
        val senderId = getString("senderId") ?: return null
        val createdMs = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis()
        val roleRaw = getString("senderRole") ?: UserRole.CLIENT.dbValue
        return ChatMessage(
            id = id,
            requestId = getString("requestId") ?: "",
            senderId = senderId,
            senderName = getString("senderName") ?: "مستخدم",
            senderRole = if (roleRaw == UserRole.CRAFTSMAN.dbValue) UserRole.CRAFTSMAN else UserRole.CLIENT,
            text = getString("text") ?: "",
            createdAtMillis = createdMs
        )
    }

    private fun DocumentSnapshot.toReviewRecord(): ReviewRecord? {
        val requestId = getString("requestId") ?: return null
        val createdMs = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis()
        val rawTags = (get("tags") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        return ReviewRecord(
            id = id,
            requestId = requestId,
            craftsmanId = getString("craftsmanId") ?: "",
            rating = (get("rating") as? Number)?.toInt() ?: 5,
            tags = rawTags,
            comment = getString("comment") ?: "",
            createdAtMillis = createdMs
        )
    }

    private fun DocumentSnapshot.toCraftsmanPin(
        centerLat: Double,
        centerLng: Double
    ): CraftsmanPin? {
        val craftsmanId = getString("craftsmanId") ?: return null
        val lat = (get("lat") as? Number)?.toDouble() ?: return null
        val lng = (get("lng") as? Number)?.toDouble() ?: return null
        val specId = getString("specialtyId") ?: "plumbing"
        val dist = MapRoutingService.haversineKm(centerLat, centerLng, lat, lng).coerceAtLeast(0.2)
        val eta = (dist * 4.5).roundToInt().coerceIn(4, 35)
        return CraftsmanPin(
            id = craftsmanId,
            name = getString("displayName") ?: "حرفي",
            specialtyId = specId,
            specialtyName = getString("specialtyName") ?: HirafiCategories.byId(specId).nameAr,
            rating = (get("rating") as? Number)?.toDouble() ?: 4.9,
            completedJobs = (get("completedJobs") as? Number)?.toInt() ?: 0,
            lat = lat,
            lng = lng,
            basePriceDh = (get("basePriceDh") as? Number)?.toInt() ?: 120,
            distanceKm = dist,
            etaMinutes = eta,
            phone = "",
            isVerified = getBoolean("isVerified") ?: false,
            experienceYears = (get("experienceYears") as? Number)?.toInt() ?: 5,
            bio = getString("bio") ?: "حرفي معتمد على شبكة حرفي",
            avatarUri = getString("avatarUri"),
            isLiveCloud = true,
            city = getString("city") ?: ""
        )
    }

    private fun DocumentSnapshot.toPortfolioItem(): PortfolioItem? {
        val craftsmanId = getString("craftsmanId") ?: return null
        val createdMs = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis()
        return PortfolioItem(
            id = id,
            craftsmanId = craftsmanId,
            title = getString("title") ?: "عمل منجز",
            description = getString("description") ?: "",
            categoryName = getString("categoryName") ?: "حرفي",
            photoUri = getString("photoUri"),
            createdAtMillis = createdMs
        )
    }
}
