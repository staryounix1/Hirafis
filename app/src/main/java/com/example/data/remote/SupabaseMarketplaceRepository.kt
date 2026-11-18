package com.example.data.remote

import android.util.Log
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

class SupabaseMarketplaceRepository(
    private val sessionManager: SupabaseSessionManager
) {
    private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 24)

    val isAuthenticated: Boolean
        get() = sessionManager.isAuthenticated

    fun currentUidOrNull(): String? = sessionManager.currentUidOrNull

    fun triggerRefresh() {
        refreshSignal.tryEmit(Unit)
    }

    // -------------------------------------------------------------------------
    // Real-time Polling Flows over Supabase PostgREST
    // -------------------------------------------------------------------------

    fun observeClientRequests(): Flow<List<ServiceRequest>> = pollingFlow(3000L) { api, bearer, uid ->
        val res = api.getServiceRequests(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer,
            clientIdFilter = "eq.$uid"
        )
        if (res.isSuccessful) res.body().orEmpty().map { it.toDomain() } else null
    }

    fun observeCraftsmanAssignedRequests(): Flow<List<ServiceRequest>> = pollingFlow(3000L) { api, bearer, uid ->
        val res = api.getServiceRequests(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer,
            assignedCraftsmanIdFilter = "eq.$uid"
        )
        if (res.isSuccessful) res.body().orEmpty().map { it.toDomain() } else null
    }

    fun observeSearchingRequests(specialtyId: String): Flow<List<ServiceRequest>> = pollingFlow(3000L) { api, bearer, _ ->
        val res = api.getServiceRequests(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer,
            statusFilter = "eq.${RequestStatus.SEARCHING.dbValue}",
            categoryIdFilter = "eq.$specialtyId"
        )
        if (res.isSuccessful) res.body().orEmpty().map { it.toDomain() } else null
    }

    fun observeRequestLiveCoords(requestId: String): Flow<Pair<Double, Double>?> = pollingFlow(2500L) { api, bearer, _ ->
        if (requestId.isBlank()) return@pollingFlow null
        val res = api.getServiceRequests(
            apiKey = SupabaseConfig.anonKey,
            bearer = bearer,
            idFilter = "eq.$requestId"
        )
        if (res.isSuccessful) {
            val dto = res.body()?.firstOrNull()
            if (dto?.craftsmanLat != null && dto.craftsmanLng != null) {
                dto.craftsmanLat to dto.craftsmanLng
            } else {
                null
            }
        } else null
    }

    fun observeOffersForRequest(requestId: String, role: UserRole): Flow<List<ServiceOffer>> =
        pollingFlow(2500L) { api, bearer, uid ->
            if (requestId.isBlank()) return@pollingFlow emptyList()
            val res = api.getOffersForRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                requestIdFilter = "eq.$requestId",
                craftsmanIdFilter = if (role == UserRole.CRAFTSMAN) "eq.$uid" else null
            )
            if (res.isSuccessful) {
                res.body().orEmpty().map { it.toDomain() }
            } else null
        }

    fun observeMessagesForRequest(requestId: String): Flow<List<ChatMessage>> =
        pollingFlow(2000L) { api, bearer, _ ->
            if (requestId.isBlank()) return@pollingFlow emptyList()
            val res = api.getChatMessages(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                requestIdFilter = "eq.$requestId"
            )
            if (res.isSuccessful) {
                res.body().orEmpty().map { it.toDomain() }
            } else null
        }

    fun observeReviews(role: UserRole): Flow<List<ReviewRecord>> =
        pollingFlow(6000L) { api, bearer, uid ->
            val res = api.getReviews(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                clientIdFilter = if (role == UserRole.CLIENT) "eq.$uid" else null,
                craftsmanIdFilter = if (role == UserRole.CRAFTSMAN) "eq.$uid" else null
            )
            if (res.isSuccessful) res.body().orEmpty().map { it.toDomain() } else null
        }

    fun observeOnlineCraftsmen(centerLat: Double, centerLng: Double): Flow<List<CraftsmanPin>> =
        pollingFlow(5000L) { api, bearer, _ ->
            val res = api.getProfilesByFilter(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                roleFilter = "eq.craftsman",
                onlineFilter = "eq.true"
            )
            if (res.isSuccessful) {
                res.body().orEmpty()
                    .filter { !it.isSuspended }
                    .map { it.toCraftsmanPin(centerLat, centerLng) }
            } else null
        }

    fun observePortfolioForCraftsman(craftsmanId: String): Flow<List<PortfolioItem>> =
        pollingFlow(5000L) { api, bearer, _ ->
            if (craftsmanId.isBlank() || craftsmanId.startsWith("c_")) return@pollingFlow emptyList()
            val res = api.getPortfolioItems(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                craftsmanIdFilter = "eq.$craftsmanId"
            )
            if (res.isSuccessful) res.body().orEmpty().map { it.toDomain() } else null
        }

    suspend fun fetchPortfolioOnce(craftsmanId: String): Result<List<PortfolioItem>> {
        if (craftsmanId.isBlank() || craftsmanId.startsWith("c_")) return Result.success(emptyList())
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(emptyList())
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(emptyList())
        return try {
            val res = api.getPortfolioItems(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                craftsmanIdFilter = "eq.$craftsmanId"
            )
            if (res.isSuccessful) {
                Result.success(res.body().orEmpty().map { it.toDomain() })
            } else {
                Result.failure(IllegalStateException("تعذر جلب معرض الأعمال (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Authenticated Mutations
    // -------------------------------------------------------------------------

    suspend fun createServiceRequest(req: ServiceRequest): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.createServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                body = SupabaseServiceRequestDto(
                    id = req.id,
                    clientId = uid,
                    clientName = req.clientName.trim().ifBlank { "زبون حرفي" }.take(80),
                    categoryId = req.categoryId.trim().let { if (it.length < 2) "plumbing" else it }.take(40),
                    categoryName = req.categoryName.trim().let { if (it.length < 2) "سباكة" else it }.take(60),
                    title = req.title.trim().let { if (it.length < 2) "طلب خدمة" else it }.take(140),
                    description = req.description.trim().take(600),
                    addressLabel = req.addressLabel.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(160),
                    lat = req.lat.coerceIn(-90.0, 90.0),
                    lng = req.lng.coerceIn(-180.0, 180.0),
                    proposedPrice = req.proposedPrice.coerceIn(40, 10000),
                    paymentMethod = req.paymentMethod.dbValue,
                    isUrgent = req.isUrgent,
                    status = RequestStatus.SEARCHING.dbValue,
                    photoUri = req.photoUri?.trim()?.takeIf { it.isNotBlank() }?.take(500)
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر إنشاء الطلب في Supabase (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProposedPrice(requestId: String, newPrice: Int): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.patchServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$requestId",
                patch = SupabaseRequestPatchDto(proposedPrice = newPrice.coerceIn(40, 10000))
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر تحديث السعر (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptOffer(offer: ServiceOffer): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.patchServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.${offer.requestId}",
                patch = SupabaseRequestPatchDto(
                    status = RequestStatus.ACCEPTED.dbValue,
                    acceptedPrice = offer.offeredPrice.coerceIn(40, 10000),
                    assignedCraftsmanId = offer.craftsmanId.takeIf { it.length == 36 },
                    assignedCraftsmanName = offer.craftsmanName.trim().ifBlank { "حرفي" }.take(80),
                    assignedCraftsmanPhone = offer.craftsmanPhone.trim().take(30),
                    assignedCraftsmanRating = offer.craftsmanRating.coerceIn(1.0, 5.0)
                )
            )
            runCatching {
                api.patchOffer(
                    apiKey = SupabaseConfig.anonKey,
                    bearer = bearer,
                    idFilter = "eq.${offer.id}",
                    patch = SupabaseOfferPatchDto(status = OfferStatus.ACCEPTED.dbValue)
                )
            }
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر قبول العرض (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun directAcceptRequestAsCraftsman(
        requestId: String,
        acceptedPrice: Int,
        session: UserSession
    ): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.patchServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$requestId",
                patch = SupabaseRequestPatchDto(
                    status = RequestStatus.ACCEPTED.dbValue,
                    acceptedPrice = acceptedPrice.coerceIn(40, 10000),
                    assignedCraftsmanId = uid,
                    assignedCraftsmanName = session.fullName.trim().ifBlank { "حرفي" }.take(80),
                    assignedCraftsmanPhone = session.phone.trim().take(30),
                    assignedCraftsmanRating = 4.9
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر قبول الطلب (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun advanceRequestStatus(requestId: String, nextStatus: RequestStatus): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.patchServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$requestId",
                patch = SupabaseRequestPatchDto(status = nextStatus.dbValue)
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر تحديث حالة الطلب (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCraftsmanLiveLocationOnRequest(
        requestId: String,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            api.patchServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$requestId",
                patch = SupabaseRequestPatchDto(
                    craftsmanLat = lat.coerceIn(-90.0, 90.0),
                    craftsmanLng = lng.coerceIn(-180.0, 180.0)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitOffer(offer: ServiceOffer, clientId: String): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.createOffer(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                body = SupabaseServiceOfferDto(
                    id = offer.id,
                    requestId = offer.requestId,
                    clientId = clientId.takeIf { it.length == 36 },
                    craftsmanId = uid,
                    craftsmanName = offer.craftsmanName.trim().ifBlank { "حرفي" }.take(80),
                    craftsmanPhone = offer.craftsmanPhone.trim().let { if (it.length < 4) "+212 612-345678" else it }.take(30),
                    craftsmanRating = offer.craftsmanRating.coerceIn(1.0, 5.0),
                    completedJobs = offer.completedJobs.coerceIn(0, 100000),
                    specialty = offer.specialty.trim().let { if (it.length < 2) "حرفي" else it }.take(60),
                    offeredPrice = offer.offeredPrice.coerceIn(40, 10000),
                    etaMinutes = offer.etaMinutes.coerceIn(1, 240),
                    distanceKm = offer.distanceKm.coerceIn(0.0, 500.0),
                    note = offer.note.trim().take(300),
                    status = OfferStatus.PENDING.dbValue,
                    expiresAtMillis = offer.expiresAtMillis.coerceAtLeast(1L)
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر إرسال العرض (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOfferStatusOrCounter(
        offerId: String,
        status: OfferStatus,
        offeredPrice: Int,
        note: String,
        expiresAtMillis: Long
    ): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.patchOffer(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$offerId",
                patch = SupabaseOfferPatchDto(
                    status = status.dbValue,
                    offeredPrice = offeredPrice.coerceIn(40, 10000),
                    note = note.trim().take(300),
                    expiresAtMillis = expiresAtMillis.coerceAtLeast(1L)
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر تحديث العرض (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(message: ChatMessage): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        val cleanText = message.text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.success(Unit)
        return try {
            val res = api.postChatMessage(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                body = SupabaseChatMessageDto(
                    id = message.id,
                    requestId = message.requestId,
                    senderId = uid,
                    senderName = message.senderName.trim().ifBlank { "مستخدم" }.take(80),
                    senderRole = message.senderRole.dbValue,
                    messageText = cleanText
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر إرسال الرسالة (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitReview(review: ReviewRecord): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.postReview(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                body = SupabaseReviewDto(
                    id = review.id,
                    requestId = review.requestId,
                    craftsmanId = review.craftsmanId.takeIf { it.length == 36 },
                    clientId = uid,
                    rating = review.rating.coerceIn(1, 5),
                    tags = review.tags.take(8),
                    comment = review.comment.trim().take(500)
                )
            )
            if (res.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("تعذر حفظ التقييم (${res.code()})"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val category = HirafiCategories.byId(session.craftsmanSpecialtyId)
            val res = api.patchProfile(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$uid",
                patch = SupabaseProfileUpdatePatchDto(
                    fullName = session.fullName.trim().let { if (it.length < 2) "حرفي معتمد" else it }.take(80),
                    city = session.city.trim().let { if (it.length < 2) "الدار البيضاء" else it }.take(60),
                    avatarUri = session.avatarUri?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                    bio = session.bio.trim().take(400),
                    experienceYears = session.experienceYears.coerceIn(0, 60),
                    basePriceDh = session.basePriceDh.coerceIn(40, 5000),
                    specialtyId = category.id,
                    specialty = category.nameAr,
                    isOnline = session.isCraftsmanOnline && !session.isSuspended,
                    lat = lat.coerceIn(-90.0, 90.0),
                    lng = lng.coerceIn(-180.0, 180.0)
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر تحديث تواجد الحرفي (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPortfolioItem(item: PortfolioItem): Result<Unit> {
        val uid = currentUidOrNull() ?: return Result.success(Unit)
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            val res = api.createPortfolioItem(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                body = SupabasePortfolioItemDto(
                    id = item.id,
                    craftsmanId = uid,
                    title = item.title.trim().let { if (it.length < 2) "عمل منجز" else it }.take(80),
                    description = item.description.trim().take(350),
                    categoryName = item.categoryName.trim().let { if (it.length < 2) "حرفي" else it }.take(60),
                    photoUri = item.photoUri?.trim()?.takeIf { it.isNotBlank() }?.take(500)
                )
            )
            if (res.isSuccessful) {
                triggerRefresh()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("تعذر حفظ العمل (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePortfolioItem(itemId: String): Result<Unit> {
        val bearer = sessionManager.getValidBearerToken() ?: return Result.success(Unit)
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return Result.success(Unit)
        return try {
            api.deletePortfolioItem(
                apiKey = SupabaseConfig.anonKey,
                bearer = bearer,
                idFilter = "eq.$itemId"
            )
            triggerRefresh()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers & Mappers
    // -------------------------------------------------------------------------

    private fun <T> pollingFlow(
        intervalMs: Long,
        fetcher: suspend (api: SupabaseApiService, bearer: String, uid: String) -> T?
    ): Flow<T> = flow {
        while (true) {
            val uid = sessionManager.currentUidOrNull
            val bearer = sessionManager.getValidBearerToken()
            val api = SupabaseClientProvider.getApiServiceOrNull()
            if (uid != null && bearer != null && api != null) {
                runCatching { fetcher(api, bearer, uid) }
                    .onSuccess { value -> if (value != null) emit(value) }
                    .onFailure { err ->
                        if (err is CancellationException) throw err
                        Log.w(TAG, "Polling error: ${err.message}")
                    }
            }
            withTimeoutOrNull(intervalMs) {
                refreshSignal.first()
            }
        }
    }

    private fun SupabaseServiceRequestDto.toDomain(): ServiceRequest {
        val createdMs = SupabaseTimestampParser.parseIsoToMillis(createdAt)
        val updatedMs = SupabaseTimestampParser.parseIsoToMillis(updatedAt ?: createdAt)
        return ServiceRequest(
            id = id,
            clientId = clientId.orEmpty(),
            clientName = clientName.ifBlank { "زبون" },
            categoryId = categoryId.ifBlank { "plumbing" },
            categoryName = categoryName.ifBlank { "سباكة" },
            title = title.ifBlank { "طلب خدمة" },
            description = description,
            addressLabel = addressLabel.ifBlank { "الدار البيضاء" },
            lat = lat,
            lng = lng,
            mapX = 0.5f,
            mapY = 0.5f,
            proposedPrice = proposedPrice,
            acceptedPrice = acceptedPrice,
            paymentMethod = PaymentMethod.entries.firstOrNull { it.dbValue == paymentMethod } ?: PaymentMethod.CASH,
            isUrgent = isUrgent,
            status = RequestStatus.fromDb(status),
            assignedCraftsmanId = assignedCraftsmanId,
            assignedCraftsmanName = assignedCraftsmanName,
            assignedCraftsmanPhone = assignedCraftsmanPhone,
            assignedCraftsmanRating = assignedCraftsmanRating,
            photoUri = photoUri,
            createdAtMillis = createdMs,
            updatedAtMillis = updatedMs
        )
    }

    private fun SupabaseServiceOfferDto.toDomain(): ServiceOffer {
        val createdMs = SupabaseTimestampParser.parseIsoToMillis(createdAt)
        val resolvedExpiry = if (expiresAtMillis > 0L) expiresAtMillis else (createdMs + 60_000L)
        return ServiceOffer(
            id = id,
            requestId = requestId,
            craftsmanId = craftsmanId.orEmpty(),
            craftsmanName = craftsmanName.ifBlank { "حرفي" },
            craftsmanPhone = craftsmanPhone,
            craftsmanRating = craftsmanRating,
            completedJobs = completedJobs,
            specialty = specialty,
            offeredPrice = offeredPrice,
            etaMinutes = etaMinutes,
            distanceKm = distanceKm,
            note = note,
            status = OfferStatus.fromDb(status),
            createdAtMillis = createdMs,
            expiresAtMillis = resolvedExpiry
        )
    }

    private fun SupabaseChatMessageDto.toDomain(): ChatMessage {
        val createdMs = SupabaseTimestampParser.parseIsoToMillis(createdAt)
        return ChatMessage(
            id = id,
            requestId = requestId,
            senderId = senderId,
            senderName = senderName.ifBlank { "مستخدم" },
            senderRole = UserRole.fromDb(senderRole),
            text = messageText,
            createdAtMillis = createdMs
        )
    }

    private fun SupabaseReviewDto.toDomain(): ReviewRecord {
        val createdMs = SupabaseTimestampParser.parseIsoToMillis(createdAt)
        return ReviewRecord(
            id = id,
            requestId = requestId,
            craftsmanId = craftsmanId.orEmpty(),
            rating = rating,
            tags = tags,
            comment = comment,
            createdAtMillis = createdMs
        )
    }

    private fun SupabaseProfileDto.toCraftsmanPin(centerLat: Double, centerLng: Double): CraftsmanPin {
        val specId = specialtyId.ifBlank { "plumbing" }
        val dist = MapRoutingService.haversineKm(centerLat, centerLng, lat, lng).coerceAtLeast(0.2)
        val eta = (dist * 4.5).roundToInt().coerceIn(4, 35)
        return CraftsmanPin(
            id = id,
            name = fullName.ifBlank { "حرفي" },
            specialtyId = specId,
            specialtyName = specialty.ifBlank { HirafiCategories.byId(specId).nameAr },
            rating = rating,
            completedJobs = completedJobs,
            lat = lat,
            lng = lng,
            basePriceDh = basePriceDh,
            distanceKm = dist,
            etaMinutes = eta,
            phone = phone,
            isVerified = isVerified || verificationStatus == "verified",
            experienceYears = experienceYears,
            bio = bio.ifBlank { "حرفي معتمد على شبكة حرفي" },
            avatarUri = avatarUri,
            isLiveCloud = true,
            city = city
        )
    }

    private fun SupabasePortfolioItemDto.toDomain(): PortfolioItem = PortfolioItem(
        id = id,
        craftsmanId = craftsmanId,
        title = title,
        description = description,
        categoryName = categoryName,
        photoUri = photoUri,
        createdAtMillis = SupabaseTimestampParser.parseIsoToMillis(createdAt)
    )

    companion object {
        private const val TAG = "SupabaseMarketplace"
    }
}
