package com.example.data.repository

import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.HirafiDao
import com.example.data.local.PortfolioItemEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.ServiceOfferEntity
import com.example.data.local.ServiceRequestEntity
import com.example.data.local.UserPreferencesDataStore
import com.example.data.model.AdminUserRecord
import com.example.data.model.AppRuntimeConfig
import com.example.data.model.AuthorizedAdminRecord
import com.example.data.model.ChatMessage
import com.example.data.model.CraftsmanPin
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PortfolioItem
import com.example.data.model.RequestStatus
import com.example.data.model.ReviewRecord
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.model.VerificationSubmission
import com.example.data.remote.FirebaseCloudStorageService
import com.example.data.remote.FirestoreAdminRepository
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreUserProfile
import com.example.data.remote.FirestoreUserProfileRepository
import com.example.data.remote.MapRoutingService
import com.example.data.remote.SupabaseAuthRequest
import com.example.data.remote.SupabaseChatMessageDto
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseOfferPatchDto
import com.example.data.remote.SupabaseProfileDto
import com.example.data.remote.SupabaseProfileLocationPatchDto
import com.example.data.remote.SupabaseRequestPatchDto
import com.example.data.remote.SupabaseReviewDto
import com.example.data.remote.SupabaseServiceOfferDto
import com.example.data.remote.SupabaseServiceRequestDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

class HirafiRepository(
    private val dao: HirafiDao,
    private val prefs: UserPreferencesDataStore,
    private val firestoreUserRepo: FirestoreUserProfileRepository? = null,
    private val firestoreMarketplaceRepo: FirestoreMarketplaceRepository? = null,
    private val firestoreAdminRepo: FirestoreAdminRepository? = null,
    private val cloudStorageService: FirebaseCloudStorageService? = null
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val userSession: Flow<UserSession> = prefs.userSessionFlow

    val isFirestoreAuthenticated: Boolean
        get() = firestoreMarketplaceRepo?.isAuthenticated == true

    fun getCraftsmenAround(centerLat: Double = 33.5822, centerLng: Double = -7.6324): List<CraftsmanPin> =
        computeCraftsmenAround(centerLat, centerLng)

    val nearbyCraftsmen: List<CraftsmanPin>
        get() = getCraftsmenAround()

    fun observeRequests(): Flow<List<ServiceRequest>> =
        dao.observeAllRequests().map { list -> list.map { it.toDomain() } }

    fun observeRequestById(requestId: String): Flow<ServiceRequest?> =
        dao.observeAllRequests().map { list -> list.firstOrNull { it.id == requestId }?.toDomain() }

    fun observeOffersForRequest(requestId: String): Flow<List<ServiceOffer>> =
        dao.observeOffersForRequest(requestId).map { list -> list.map { it.toDomain() } }

    fun observeMessagesForRequest(requestId: String): Flow<List<ChatMessage>> =
        dao.observeMessagesForRequest(requestId).map { list -> list.map { it.toDomain() } }

    fun observeReviews(): Flow<List<ReviewRecord>> =
        dao.observeReviews().map { list -> list.map { it.toDomain() } }

    fun observePortfolioForCraftsman(craftsmanId: String): Flow<List<PortfolioItem>> =
        dao.observePortfolioForCraftsman(craftsmanId).map { list -> list.map { it.toDomain() } }

    fun observeFirestoreClientRequests(): Flow<List<ServiceRequest>> =
        firestoreMarketplaceRepo?.observeClientRequests() ?: emptyFlow()

    fun observeFirestoreCraftsmanAssignedRequests(): Flow<List<ServiceRequest>> =
        firestoreMarketplaceRepo?.observeCraftsmanAssignedRequests() ?: emptyFlow()

    fun observeFirestoreSearchingRequests(specialtyId: String): Flow<List<ServiceRequest>> =
        firestoreMarketplaceRepo?.observeSearchingRequests(specialtyId) ?: emptyFlow()

    fun observeFirestoreOffersForRequest(requestId: String, role: UserRole): Flow<List<ServiceOffer>> =
        firestoreMarketplaceRepo?.observeOffersForRequest(requestId, role) ?: emptyFlow()

    fun observeFirestoreMessagesForRequest(requestId: String): Flow<List<ChatMessage>> =
        firestoreMarketplaceRepo?.observeMessagesForRequest(requestId) ?: emptyFlow()

    fun observeFirestoreOnlineCraftsmen(centerLat: Double, centerLng: Double): Flow<List<CraftsmanPin>> =
        firestoreMarketplaceRepo?.observeOnlineCraftsmen(centerLat, centerLng) ?: emptyFlow()

    fun observeFirestoreRequestLiveCoords(requestId: String): Flow<Pair<Double, Double>?> =
        firestoreMarketplaceRepo?.observeRequestLiveCoords(requestId) ?: emptyFlow()

    fun observeFirestorePortfolioForCraftsman(craftsmanId: String): Flow<List<PortfolioItem>> =
        firestoreMarketplaceRepo?.observePortfolioForCraftsman(craftsmanId) ?: emptyFlow()

    suspend fun fetchPortfolioOnce(craftsmanId: String): List<PortfolioItem> =
        firestoreMarketplaceRepo?.fetchPortfolioOnce(craftsmanId)?.getOrNull().orEmpty()

    suspend fun cacheRemoteRequests(requests: List<ServiceRequest>) {
        if (requests.isEmpty()) return
        dao.upsertRequests(requests.map { it.toEntity() })
    }

    suspend fun cacheRemoteOffers(offers: List<ServiceOffer>) {
        if (offers.isEmpty()) return
        dao.upsertOffers(offers.map { it.toEntity() })
    }

    suspend fun cacheRemoteMessages(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        dao.insertMessages(messages.map { it.toEntity() })
    }

    suspend fun cacheRemotePortfolio(items: List<PortfolioItem>) {
        if (items.isEmpty()) return
        dao.upsertPortfolioItems(items.map { it.toEntity() })
    }

    fun observeMyCloudProfile(): Flow<FirestoreUserProfile?> =
        firestoreUserRepo?.observeMyProfile() ?: emptyFlow()

    fun observeAppRuntimeConfig(): Flow<AppRuntimeConfig> =
        firestoreAdminRepo?.observeAppRuntimeConfig() ?: emptyFlow()

    fun observeAdminVerificationSubmissions(): Flow<List<VerificationSubmission>> =
        firestoreAdminRepo?.observeVerificationSubmissions() ?: emptyFlow()

    fun observeAdminAllUsers(): Flow<List<AdminUserRecord>> =
        firestoreAdminRepo?.observeAllUsers() ?: emptyFlow()

    fun observeAdminAllRequests(): Flow<List<ServiceRequest>> =
        firestoreAdminRepo?.observeAllServiceRequests() ?: emptyFlow()

    fun observeAuthorizedAdmins(): Flow<List<AuthorizedAdminRecord>> =
        firestoreAdminRepo?.observeAuthorizedAdmins() ?: emptyFlow()

    suspend fun clearSeededRequests() {
        dao.clearSeededRequests()
    }

    suspend fun syncFcmToken(token: String) {
        if (token.isBlank()) return
        prefs.saveFcmToken(token)
        firestoreUserRepo?.updateFcmToken(token)
    }

    suspend fun applyRemoteVerificationUpdate(
        status: IdentityVerificationStatus,
        rejectionReason: String,
        isSuspended: Boolean
    ) {
        prefs.updateIdentityVerificationStatus(status, rejectionReason)
        prefs.setSuspended(isSuspended)
    }

    suspend fun seedInitialMarketplaceRequestsIfEmpty() {
        if (isFirestoreAuthenticated) return
        val existing = dao.observeAllRequests().first()
        if (existing.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val samples = listOf(
            ServiceRequestEntity(
                id = "req_seed_1",
                clientId = "client_sara",
                clientName = "سارة المنصوري",
                categoryId = "plumbing",
                categoryName = "سباكة",
                title = "تسرب مياه تحت حوض المطبخ",
                description = "يوجد تسرب مستمر في أنبوب الصرف تحت الحوض وأحتاج لإصلاح عاجل مع تبديل الجوان.",
                addressLabel = "المعاريف، شارع الزرقطوني",
                lat = 33.5822,
                lng = -7.6324,
                mapX = 0.48f,
                mapY = 0.44f,
                proposedPrice = 130,
                acceptedPrice = null,
                paymentMethod = PaymentMethod.CASH.dbValue,
                isUrgent = true,
                status = RequestStatus.SEARCHING.dbValue,
                assignedCraftsmanId = null,
                assignedCraftsmanName = null,
                assignedCraftsmanPhone = null,
                assignedCraftsmanRating = null,
                photoUri = null,
                createdAtMillis = now - 180_000L,
                updatedAtMillis = now - 180_000L
            ),
            ServiceRequestEntity(
                id = "req_seed_2",
                clientId = "client_mehdi",
                clientName = "المهدي بركة",
                categoryId = "electricity",
                categoryName = "كهرباء",
                title = "انقطاع القاطع الكهربائي الرئيسي",
                description = "الديجونكتور يفصل عند تشغيل السخان الكهربائي، أحتاج فحص اللوحة وتغيير القاطع إذا لزم.",
                addressLabel = "سيدي معروف، التكنوبارك",
                lat = 33.5398,
                lng = -7.6481,
                mapX = 0.42f,
                mapY = 0.72f,
                proposedPrice = 160,
                acceptedPrice = null,
                paymentMethod = PaymentMethod.CASH.dbValue,
                isUrgent = true,
                status = RequestStatus.SEARCHING.dbValue,
                assignedCraftsmanId = null,
                assignedCraftsmanName = null,
                assignedCraftsmanPhone = null,
                assignedCraftsmanRating = null,
                photoUri = null,
                createdAtMillis = now - 420_000L,
                updatedAtMillis = now - 420_000L
            ),
            ServiceRequestEntity(
                id = "req_seed_3",
                clientId = "client_nadia",
                clientName = "نادية الشرايبي",
                categoryId = "ac",
                categoryName = "تبريد وتكييف",
                title = "تنظيف وتعبئة غاز المكيف",
                description = "مكيف سبليت 12000 BTU لا يبرد بشكل جيد منذ أسبوع.",
                addressLabel = "أنفا، قرب موروكو مول",
                lat = 33.5898,
                lng = -7.6852,
                mapX = 0.24f,
                mapY = 0.36f,
                proposedPrice = 220,
                acceptedPrice = null,
                paymentMethod = PaymentMethod.TRANSFER.dbValue,
                isUrgent = false,
                status = RequestStatus.SEARCHING.dbValue,
                assignedCraftsmanId = null,
                assignedCraftsmanName = null,
                assignedCraftsmanPhone = null,
                assignedCraftsmanRating = null,
                photoUri = null,
                createdAtMillis = now - 720_000L,
                updatedAtMillis = now - 720_000L
            )
        )
        dao.upsertRequests(samples)
    }

    suspend fun setRole(role: UserRole): Boolean = prefs.setRole(role)

    suspend fun registerFirstTimeAccount(
        fullName: String,
        email: String = "",
        phone: String = "+212 612-345678",
        city: String,
        role: UserRole,
        specialtyId: String?,
        passwordHash: String? = null,
        authProvider: String = "email",
        userIdOverride: String? = null
    ): Boolean {
        val registered = prefs.registerFirstTimeAccount(
            fullName = fullName,
            email = email,
            phone = phone,
            city = city,
            role = role,
            specialtyId = specialtyId,
            passwordHash = passwordHash,
            authProvider = authProvider,
            userIdOverride = userIdOverride
        )
        if (registered) {
            val session = prefs.userSessionFlow.first()
            pushProfileToSupabase(session)
        }
        return registered
    }

    suspend fun completeGoogleAccountLogin(
        uid: String,
        displayName: String,
        preferredDisplayName: String = "",
        email: String,
        city: String,
        preferredRole: UserRole,
        preferredSpecialtyId: String?
    ): Result<UserSession> {
        val userRepo = firestoreUserRepo
            ?: return Result.failure(IllegalStateException("تعذر الاتصال بخدمة قاعدة البيانات السحابية"))
        val resolvedName = preferredDisplayName.trim().takeIf { it.length >= 2 }
            ?: displayName.trim().takeIf { it.length >= 2 }
            ?: email.substringBefore("@").trim().takeIf { it.length >= 2 }
            ?: "مستخدم غوغل"
        val resolvedSpec = preferredSpecialtyId ?: "plumbing"

        val synced = userRepo.syncOrRestoreProfile(
            fullName = resolvedName,
            email = email,
            city = city,
            chosenRole = preferredRole.dbValue,
            chosenSpecialtyId = resolvedSpec
        ).getOrElse { err ->
            return Result.failure(err)
        }

        val finalRole = UserRole.fromDb(synced.role)
        val finalSpecialty = if (finalRole == UserRole.CRAFTSMAN) {
            synced.specialtyId.takeIf { it.isNotBlank() } ?: resolvedSpec
        } else null

        val finalName = synced.fullName.takeIf { it.isNotBlank() } ?: resolvedName
        val finalEmail = synced.email.takeIf { it.isNotBlank() } ?: email
        val finalCity = synced.city.takeIf { it.isNotBlank() } ?: city
        val finalPhone = synced.phone.takeIf { it.isNotBlank() } ?: "+212 612-345678"

        prefs.registerFirstTimeAccount(
            fullName = finalName,
            email = finalEmail,
            phone = finalPhone,
            city = finalCity,
            role = finalRole,
            specialtyId = finalSpecialty,
            authProvider = "google",
            userIdOverride = uid,
            avatarUri = synced.avatarUri,
            bio = synced.bio,
            experienceYears = synced.experienceYears.toInt(),
            basePriceDh = synced.basePriceDh.toInt(),
            verificationStatus = IdentityVerificationStatus.fromDb(synced.verificationStatus),
            rejectionReason = synced.rejectionReason,
            isSuspended = synced.isSuspended,
            forceOverwrite = true
        )
        val updatedSession = prefs.userSessionFlow.first()
        if (finalRole == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(updatedSession.city)
            val dirResult = firestoreMarketplaceRepo?.upsertCraftsmanDirectory(updatedSession, coord.lat, coord.lng)
            if (dirResult != null && dirResult.isFailure) {
                return Result.failure(
                    dirResult.exceptionOrNull()
                        ?: IllegalStateException("تم حفظ حسابك ولكن تعذر نشر ملفك في دليل الحرفيين")
                )
            }
        }
        return Result.success(updatedSession)
    }

    suspend fun restoreExistingGoogleProfileIfLocked(
        uid: String,
        fallbackEmail: String
    ): Result<UserSession?> {
        val userRepo = firestoreUserRepo ?: return Result.success(null)
        val existing = userRepo.fetchExistingProfileIfLocked().getOrElse { err ->
            return Result.failure(err)
        } ?: return Result.success(null)

        val restoredRole = UserRole.fromDb(existing.role)
        val restoredSpec = if (restoredRole == UserRole.CRAFTSMAN) {
            existing.specialtyId.takeIf { it.isNotBlank() } ?: "plumbing"
        } else null
        prefs.registerFirstTimeAccount(
            fullName = existing.fullName.ifBlank { "مستخدم حرفي" },
            email = existing.email.ifBlank { fallbackEmail },
            phone = existing.phone.ifBlank { "+212 612-345678" },
            city = existing.city.ifBlank { "الدار البيضاء" },
            role = restoredRole,
            specialtyId = restoredSpec,
            authProvider = "google",
            userIdOverride = uid,
            avatarUri = existing.avatarUri,
            bio = existing.bio,
            experienceYears = existing.experienceYears.toInt(),
            basePriceDh = existing.basePriceDh.toInt(),
            verificationStatus = IdentityVerificationStatus.fromDb(existing.verificationStatus),
            rejectionReason = existing.rejectionReason,
            isSuspended = existing.isSuspended,
            forceOverwrite = true
        )
        val updatedSession = prefs.userSessionFlow.first()
        if (restoredRole == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(updatedSession.city)
            firestoreMarketplaceRepo?.upsertCraftsmanDirectory(updatedSession, coord.lat, coord.lng)
        }
        return Result.success(updatedSession)
    }

    suspend fun syncCraftsmanDirectoryPresence(
        latOverride: Double? = null,
        lngOverride: Double? = null
    ): Result<Unit> {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN || !isFirestoreAuthenticated) {
            return Result.success(Unit)
        }
        val cityCoord = MoroccanLocations.coordForCity(session.city)
        val lat = latOverride ?: cityCoord.lat
        val lng = lngOverride ?: cityCoord.lng
        return firestoreMarketplaceRepo?.upsertCraftsmanDirectory(session, lat, lng)
            ?: Result.success(Unit)
    }

    suspend fun setCraftsmanOnline(isOnline: Boolean) {
        prefs.setCraftsmanOnline(isOnline)
        val session = prefs.userSessionFlow.first()
        if (session.role == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(session.city)
            firestoreMarketplaceRepo?.upsertCraftsmanDirectory(session, coord.lat, coord.lng)
        }
    }

    suspend fun setCity(city: String) {
        prefs.setCity(city)
        val session = prefs.userSessionFlow.first()
        firestoreUserRepo?.updateProfile(
            fullName = session.fullName,
            phone = session.phone,
            city = city,
            avatarUri = session.avatarUri,
            bio = session.bio,
            experienceYears = session.experienceYears,
            basePriceDh = session.basePriceDh
        )
        if (session.role == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(city)
            firestoreMarketplaceRepo?.upsertCraftsmanDirectory(session.copy(city = city), coord.lat, coord.lng)
        }
    }

    suspend fun setSpecialty(specialtyId: String): Boolean {
        val lockedNow = prefs.setSpecialty(specialtyId)
        if (lockedNow) {
            val session = prefs.userSessionFlow.first()
            pushProfileToSupabase(session)
        }
        return lockedNow
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        city: String,
        avatarUri: String? = null,
        bio: String? = null,
        experienceYears: Int? = null,
        basePriceDh: Int? = null
    ) {
        val cloudAvatarUri = if (!avatarUri.isNullOrBlank() && cloudStorageService != null && isFirestoreAuthenticated) {
            cloudStorageService.uploadAvatar(avatarUri).getOrNull() ?: avatarUri
        } else avatarUri

        prefs.updateProfile(
            fullName = fullName,
            phone = phone,
            city = city,
            avatarUri = cloudAvatarUri,
            bio = bio,
            experienceYears = experienceYears,
            basePriceDh = basePriceDh
        )
        val session = prefs.userSessionFlow.first()
        firestoreUserRepo?.updateProfile(
            fullName = session.fullName,
            phone = session.phone,
            city = session.city,
            avatarUri = session.avatarUri,
            bio = session.bio,
            experienceYears = session.experienceYears,
            basePriceDh = session.basePriceDh
        )
        if (session.role == UserRole.CRAFTSMAN) {
            val coord = MoroccanLocations.coordForCity(session.city)
            firestoreMarketplaceRepo?.upsertCraftsmanDirectory(session, coord.lat, coord.lng)
        }
        pushProfileToSupabase(session)
    }

    suspend fun addPortfolioItem(
        title: String,
        description: String,
        photoUri: String?
    ): Result<PortfolioItem> {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN) {
            return Result.failure(IllegalStateException("معرض الأعمال متاح للحرفيين فقط"))
        }
        val cleanTitle = title.trim()
        if (cleanTitle.length < 2) {
            return Result.failure(IllegalArgumentException("يرجى إدخال عنوان للعمل المنجز"))
        }
        val category = HirafiCategories.byId(session.craftsmanSpecialtyId)
        val itemId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val uploadedPhotoUri = if (!photoUri.isNullOrBlank() && cloudStorageService != null && isFirestoreAuthenticated) {
            cloudStorageService.uploadPortfolioPhoto(itemId, photoUri).getOrNull() ?: photoUri
        } else photoUri

        val item = PortfolioItem(
            id = itemId,
            craftsmanId = session.userId,
            title = cleanTitle.take(80),
            description = description.trim().take(350),
            categoryName = category.nameAr,
            photoUri = uploadedPhotoUri?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            createdAtMillis = now
        )
        dao.upsertPortfolioItem(item.toEntity())
        firestoreMarketplaceRepo?.addPortfolioItem(item)
        return Result.success(item)
    }

    suspend fun deletePortfolioItem(itemId: String) {
        dao.deletePortfolioItem(itemId)
        firestoreMarketplaceRepo?.deletePortfolioItem(itemId)
    }

    suspend fun submitCraftsmanIdentityVerification(
        nationalIdNumber: String,
        frontPhotoUri: String,
        backPhotoUri: String,
        selfiePhotoUri: String
    ): Result<Boolean> {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN) {
            return Result.failure(IllegalStateException("التحقق من الهوية مخصص للحرفيين فقط"))
        }

        var cloudImagesUploaded = true
        val finalFront = if (cloudStorageService != null && isFirestoreAuthenticated) {
            cloudStorageService.uploadVerificationPhoto("front", frontPhotoUri).getOrElse {
                cloudImagesUploaded = false
                frontPhotoUri
            }
        } else frontPhotoUri

        val finalBack = if (cloudStorageService != null && isFirestoreAuthenticated) {
            cloudStorageService.uploadVerificationPhoto("back", backPhotoUri).getOrElse {
                cloudImagesUploaded = false
                backPhotoUri
            }
        } else backPhotoUri

        val finalSelfie = if (cloudStorageService != null && isFirestoreAuthenticated) {
            cloudStorageService.uploadVerificationPhoto("selfie", selfiePhotoUri).getOrElse {
                cloudImagesUploaded = false
                selfiePhotoUri
            }
        } else selfiePhotoUri

        prefs.saveIdentityVerificationSubmission(
            nationalIdNumber = nationalIdNumber,
            frontPhotoUri = finalFront,
            backPhotoUri = finalBack,
            selfiePhotoUri = finalSelfie,
            status = IdentityVerificationStatus.PENDING_REVIEW
        )
        prefs.updateIdentityVerificationStatus(IdentityVerificationStatus.PENDING_REVIEW, "")

        val cloudResult = firestoreAdminRepo?.submitVerificationRequest(
            craftsmanName = session.fullName,
            specialtyId = session.craftsmanSpecialtyId,
            city = session.city,
            nationalIdNumber = nationalIdNumber,
            frontPhotoUri = finalFront,
            backPhotoUri = finalBack,
            selfiePhotoUri = finalSelfie
        )
        if (cloudResult != null && cloudResult.isFailure) {
            return Result.failure(
                cloudResult.exceptionOrNull() ?: IllegalStateException("تعذر إرسال طلب التوثيق إلى السحابة")
            )
        }
        return Result.success(cloudImagesUploaded)
    }

    suspend fun resetCraftsmanIdentityVerification() {
        val session = prefs.userSessionFlow.first()
        if (session.role != UserRole.CRAFTSMAN) return
        prefs.updateIdentityVerificationStatus(IdentityVerificationStatus.NOT_SUBMITTED, "")
    }

    suspend fun setProductionMode(enabled: Boolean): Result<Unit> =
        firestoreAdminRepo?.setProductionMode(enabled)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun reviewCraftsmanVerification(
        craftsmanId: String,
        approved: Boolean,
        rejectionReason: String = ""
    ): Result<Unit> =
        firestoreAdminRepo?.reviewVerificationSubmission(craftsmanId, approved, rejectionReason)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun toggleUserSuspended(
        userId: String,
        isCraftsman: Boolean,
        suspended: Boolean
    ): Result<Unit> =
        firestoreAdminRepo?.toggleUserSuspended(userId, isCraftsman, suspended)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun cancelRequestAsAdmin(requestId: String): Result<Unit> =
        firestoreAdminRepo?.cancelRequestAsAdmin(requestId)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun addAuthorizedAdmin(email: String): Result<Unit> =
        firestoreAdminRepo?.addAuthorizedAdmin(email)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun removeAuthorizedAdmin(email: String): Result<Unit> =
        firestoreAdminRepo?.removeAuthorizedAdmin(email)
            ?: Result.failure(IllegalStateException("خدمة الإدارة غير متوفرة"))

    suspend fun createServiceRequest(
        categoryId: String,
        title: String,
        description: String,
        addressLabel: String,
        lat: Double,
        lng: Double,
        mapX: Float,
        mapY: Float,
        proposedPrice: Int,
        paymentMethod: PaymentMethod,
        isUrgent: Boolean,
        photoUri: String?,
        preferredCraftsmanId: String? = null,
        radiusKm: Int = 5,
        productionMode: Boolean = false
    ): ServiceRequest {
        val session = prefs.userSessionFlow.first()
        val category = HirafiCategories.byId(categoryId)
        val now = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString()
        val uploadedPhotoUri = if (!photoUri.isNullOrBlank() && cloudStorageService != null && isFirestoreAuthenticated) {
            cloudStorageService.uploadRequestPhoto(requestId, photoUri).getOrNull() ?: photoUri
        } else photoUri
        val resolvedTitle = title.trim().let { if (it.length < 2) "طلب ${category.nameAr}" else it }
        val entity = ServiceRequestEntity(
            id = requestId,
            clientId = session.userId,
            clientName = session.fullName,
            categoryId = category.id,
            categoryName = category.nameAr,
            title = resolvedTitle,
            description = description.trim(),
            addressLabel = addressLabel.trim().ifBlank { session.city },
            lat = lat,
            lng = lng,
            mapX = mapX,
            mapY = mapY,
            proposedPrice = proposedPrice,
            acceptedPrice = null,
            paymentMethod = paymentMethod.dbValue,
            isUrgent = isUrgent,
            status = RequestStatus.SEARCHING.dbValue,
            assignedCraftsmanId = null,
            assignedCraftsmanName = null,
            assignedCraftsmanPhone = null,
            assignedCraftsmanRating = null,
            photoUri = uploadedPhotoUri,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val domain = entity.toDomain()
        dao.upsertRequest(entity)
        firestoreMarketplaceRepo?.createServiceRequest(domain)
        pushRequestToSupabase(entity, session)

        // Only simulate local sample offers when NOT in productionMode and targeting offline/seeded craftsman
        val isSeededSampleTarget = preferredCraftsmanId != null && preferredCraftsmanId.startsWith("c_")
        if (!productionMode && (!isFirestoreAuthenticated || isSeededSampleTarget)) {
            simulateLiveIncomingOffers(
                requestId = requestId,
                categoryId = categoryId,
                lat = lat,
                lng = lng,
                proposedPrice = proposedPrice,
                preferredCraftsmanId = preferredCraftsmanId,
                radiusKm = radiusKm
            )
        }
        return domain
    }

    suspend fun raiseRequestPrice(requestId: String, newPrice: Int) {
        val existing = dao.getRequestById(requestId) ?: return
        val updated = existing.copy(
            proposedPrice = newPrice,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.upsertRequest(updated)
        firestoreMarketplaceRepo?.updateProposedPrice(requestId, newPrice)
        patchRequestInSupabase(
            requestId = requestId,
            patch = SupabaseRequestPatchDto(proposedPrice = newPrice)
        )
        if (!isFirestoreAuthenticated && updated.status == RequestStatus.SEARCHING.dbValue) {
            simulateQuickPriceBoostOffer(requestId, existing.categoryId, existing.lat, existing.lng, newPrice)
        }
    }

    suspend fun expandSearchRadius(requestId: String, newRadiusKm: Int) {
        val existing = dao.getRequestById(requestId) ?: return
        if (existing.status != RequestStatus.SEARCHING.dbValue) return
        if (isFirestoreAuthenticated) return
        val existingOffers = dao.observeOffersForRequest(requestId).first()
        val alreadyOfferedIds = existingOffers.map { it.craftsmanId }.toSet()
        val candidates = getCraftsmenAround(existing.lat, existing.lng)
            .filter { it.id !in alreadyOfferedIds && it.distanceKm <= newRadiusKm.toDouble() }
            .sortedWith(compareByDescending<CraftsmanPin> { it.specialtyId == existing.categoryId }.thenBy { it.distanceKm })
            .take(2)

        repoScope.launch {
            candidates.forEachIndexed { idx, craftsman ->
                delay(if (idx == 0) 800L else 1900L)
                val currentReq = dao.getRequestById(requestId) ?: return@launch
                if (currentReq.status != RequestStatus.SEARCHING.dbValue) return@launch
                val now = System.currentTimeMillis()
                val offerPrice = if (idx == 0) currentReq.proposedPrice else currentReq.proposedPrice + 15
                dao.upsertOffer(
                    ServiceOfferEntity(
                        id = UUID.randomUUID().toString(),
                        requestId = requestId,
                        craftsmanId = craftsman.id,
                        craftsmanName = craftsman.name,
                        craftsmanPhone = craftsman.phone,
                        craftsmanRating = craftsman.rating,
                        completedJobs = craftsman.completedJobs,
                        specialty = craftsman.specialtyName,
                        offeredPrice = offerPrice,
                        etaMinutes = craftsman.etaMinutes,
                        distanceKm = craftsman.distanceKm,
                        note = "استلمت طلبك بعد توسيع نطاق البحث إلى $newRadiusKm كم",
                        status = OfferStatus.PENDING.dbValue,
                        createdAtMillis = now,
                        expiresAtMillis = now + 50_000L
                    )
                )
            }
        }
    }

    private fun simulateLiveIncomingOffers(
        requestId: String,
        categoryId: String,
        lat: Double,
        lng: Double,
        proposedPrice: Int,
        preferredCraftsmanId: String?,
        radiusKm: Int
    ) {
        repoScope.launch {
            val allAround = getCraftsmenAround(lat, lng)
            val preferred = preferredCraftsmanId?.let { id -> allAround.firstOrNull { it.id == id } }
            val matching = allAround.filter { it.specialtyId == categoryId && it.id != preferredCraftsmanId }
            val withinRadius = matching.filter { it.distanceKm <= radiusKm.toDouble() }
            val others = allAround.filter { it.specialtyId != categoryId && it.id != preferredCraftsmanId }
            val pool = (listOfNotNull(preferred) + withinRadius + matching + others)
                .distinctBy { it.id }
                .take(3)

            val delays = if (preferred != null) listOf(550L, 2100L, 3600L) else listOf(1000L, 2300L, 3900L)
            pool.forEachIndexed { index, craftsman ->
                delay(delays.getOrElse(index) { 2000L })
                val currentReq = dao.getRequestById(requestId) ?: return@launch
                if (currentReq.status != RequestStatus.SEARCHING.dbValue) return@launch

                val offerPrice = when {
                    preferred != null && index == 0 -> proposedPrice
                    index == 0 -> proposedPrice + 20
                    index == 1 -> proposedPrice
                    else -> (proposedPrice - 10).coerceAtLeast(60)
                }
                val note = when {
                    preferred != null && index == 0 -> "تلقيت طلبك المباشر ومستعد للانطلاق فوراً!"
                    index == 0 -> "متوفر الآن مع كامل المعدات الأصلية (${craftsman.experienceYears} سنوات خبرة)"
                    index == 1 -> "سأصل خلال ${craftsman.etaMinutes} دقائق وبنفس سعرك"
                    else -> "عرض سريع على بُعد ${craftsman.distanceKm} كم منك"
                }
                val now = System.currentTimeMillis()
                val offer = ServiceOfferEntity(
                    id = UUID.randomUUID().toString(),
                    requestId = requestId,
                    craftsmanId = craftsman.id,
                    craftsmanName = craftsman.name,
                    craftsmanPhone = craftsman.phone,
                    craftsmanRating = craftsman.rating,
                    completedJobs = craftsman.completedJobs,
                    specialty = craftsman.specialtyName,
                    offeredPrice = offerPrice,
                    etaMinutes = craftsman.etaMinutes,
                    distanceKm = craftsman.distanceKm,
                    note = note,
                    status = OfferStatus.PENDING.dbValue,
                    createdAtMillis = now,
                    expiresAtMillis = now + 55_000L
                )
                dao.upsertOffer(offer)
            }
        }
    }

    private fun simulateQuickPriceBoostOffer(
        requestId: String,
        categoryId: String,
        lat: Double,
        lng: Double,
        newPrice: Int
    ) {
        repoScope.launch {
            delay(950L)
            val currentReq = dao.getRequestById(requestId) ?: return@launch
            if (currentReq.status != RequestStatus.SEARCHING.dbValue) return@launch
            val around = getCraftsmenAround(lat, lng)
            val craftsman = around.firstOrNull { it.specialtyId == categoryId } ?: around.first()
            val now = System.currentTimeMillis()
            val offer = ServiceOfferEntity(
                id = UUID.randomUUID().toString(),
                requestId = requestId,
                craftsmanId = "${craftsman.id}_boost_${now % 1000}",
                craftsmanName = craftsman.name,
                craftsmanPhone = craftsman.phone,
                craftsmanRating = craftsman.rating,
                completedJobs = craftsman.completedJobs,
                specialty = craftsman.specialtyName,
                offeredPrice = newPrice,
                etaMinutes = (craftsman.etaMinutes - 2).coerceAtLeast(3),
                distanceKm = craftsman.distanceKm,
                note = "موافق على السعر الجديد ($newPrice درهم) وجاهز للانطلاق",
                status = OfferStatus.PENDING.dbValue,
                createdAtMillis = now,
                expiresAtMillis = now + 50_000L
            )
            dao.upsertOffer(offer)
        }
    }

    suspend fun submitCraftsmanBid(
        requestId: String,
        offeredPrice: Int,
        etaMinutes: Int,
        note: String
    ) {
        val session = prefs.userSessionFlow.first()
        require(session.isCraftsmanSpecialtyLocked) { "يجب تأكيد مهنتك الوحيدة أولاً" }
        val req = dao.getRequestById(requestId) ?: return
        require(req.categoryId == session.craftsmanSpecialtyId) { "لا يمكنك تقديم عرض لطلب خارج مهنتك" }
        val specialty = HirafiCategories.byId(session.craftsmanSpecialtyId)
        val now = System.currentTimeMillis()
        val offer = ServiceOfferEntity(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            craftsmanId = session.userId,
            craftsmanName = session.fullName,
            craftsmanPhone = session.phone,
            craftsmanRating = 4.9,
            completedJobs = 64,
            specialty = specialty.nameAr,
            offeredPrice = offeredPrice,
            etaMinutes = etaMinutes,
            distanceKm = 1.2,
            note = note.ifBlank { "جاهز للوصول خلال $etaMinutes دقائق" },
            status = OfferStatus.PENDING.dbValue,
            createdAtMillis = now,
            expiresAtMillis = now + 60_000L
        )
        dao.upsertOffer(offer)
        firestoreMarketplaceRepo?.submitOffer(offer.toDomain(), req.clientId)
        pushOfferToSupabase(offer, session)
    }

    suspend fun craftsmanDirectAcceptRequest(requestId: String) {
        val req = dao.getRequestById(requestId) ?: return
        val session = prefs.userSessionFlow.first()
        require(session.isCraftsmanSpecialtyLocked) { "يجب تأكيد مهنتك الوحيدة أولاً" }
        require(req.categoryId == session.craftsmanSpecialtyId) { "لا يمكنك قبول طلب خارج مهنتك" }
        val now = System.currentTimeMillis()
        val updated = req.copy(
            status = RequestStatus.ACCEPTED.dbValue,
            acceptedPrice = req.proposedPrice,
            assignedCraftsmanId = session.userId,
            assignedCraftsmanName = session.fullName,
            assignedCraftsmanPhone = session.phone,
            assignedCraftsmanRating = 4.9,
            updatedAtMillis = now
        )
        dao.upsertRequest(updated)
        firestoreMarketplaceRepo?.directAcceptRequestAsCraftsman(
            requestId = requestId,
            acceptedPrice = req.proposedPrice,
            session = session
        )
        patchRequestInSupabase(
            requestId = requestId,
            patch = SupabaseRequestPatchDto(
                status = RequestStatus.ACCEPTED.dbValue,
                acceptedPrice = req.proposedPrice,
                assignedCraftsmanId = session.userId.takeIf { session.isSupabaseAuthenticated },
                assignedCraftsmanName = session.fullName,
                assignedCraftsmanPhone = session.phone
            )
        )
        insertSystemWelcomeMessage(
            requestId = requestId,
            clientId = req.clientId,
            assignedCraftsmanId = session.userId,
            senderId = session.userId,
            senderName = session.fullName,
            role = UserRole.CRAFTSMAN,
            text = "تم قبول طلبك بسعر ${req.proposedPrice} درهم. أنا متوجه إليك الآن!"
        )
    }

    suspend fun acceptOffer(offer: ServiceOffer) {
        val req = dao.getRequestById(offer.requestId) ?: return
        val now = System.currentTimeMillis()
        val updatedOffer = ServiceOfferEntity(
            id = offer.id,
            requestId = offer.requestId,
            craftsmanId = offer.craftsmanId,
            craftsmanName = offer.craftsmanName,
            craftsmanPhone = offer.craftsmanPhone,
            craftsmanRating = offer.craftsmanRating,
            completedJobs = offer.completedJobs,
            specialty = offer.specialty,
            offeredPrice = offer.offeredPrice,
            etaMinutes = offer.etaMinutes,
            distanceKm = offer.distanceKm,
            note = offer.note,
            status = OfferStatus.ACCEPTED.dbValue,
            createdAtMillis = offer.createdAtMillis,
            expiresAtMillis = offer.expiresAtMillis
        )
        dao.upsertOffer(updatedOffer)
        dao.declineOtherOffers(offer.requestId, offer.id)

        val updatedReq = req.copy(
            status = RequestStatus.ACCEPTED.dbValue,
            acceptedPrice = offer.offeredPrice,
            assignedCraftsmanId = offer.craftsmanId,
            assignedCraftsmanName = offer.craftsmanName,
            assignedCraftsmanPhone = offer.craftsmanPhone,
            assignedCraftsmanRating = offer.craftsmanRating,
            updatedAtMillis = now
        )
        dao.upsertRequest(updatedReq)

        firestoreMarketplaceRepo?.acceptOffer(offer)
        patchOfferInSupabase(offer.id, SupabaseOfferPatchDto(status = OfferStatus.ACCEPTED.dbValue))
        patchRequestInSupabase(
            requestId = req.id,
            patch = SupabaseRequestPatchDto(
                status = RequestStatus.ACCEPTED.dbValue,
                acceptedPrice = offer.offeredPrice,
                assignedCraftsmanName = offer.craftsmanName,
                assignedCraftsmanPhone = offer.craftsmanPhone
            )
        )

        insertSystemWelcomeMessage(
            requestId = req.id,
            clientId = req.clientId,
            assignedCraftsmanId = offer.craftsmanId,
            senderId = req.clientId,
            senderName = req.clientName,
            role = UserRole.CLIENT,
            text = "تم قبول عرض ${offer.craftsmanName} بسعر ${offer.offeredPrice} درهم، بانتظار الوصول خلال ${offer.etaMinutes} دقائق."
        )
    }

    suspend fun declineOffer(offerId: String) {
        val offer = dao.getOfferById(offerId) ?: return
        dao.upsertOffer(offer.copy(status = OfferStatus.DECLINED.dbValue))
        firestoreMarketplaceRepo?.updateOfferStatusOrCounter(
            requestId = offer.requestId,
            offerId = offerId,
            status = OfferStatus.DECLINED,
            offeredPrice = offer.offeredPrice,
            note = offer.note,
            expiresAtMillis = offer.expiresAtMillis
        )
        patchOfferInSupabase(offerId, SupabaseOfferPatchDto(status = OfferStatus.DECLINED.dbValue))
    }

    suspend fun counterOffer(offer: ServiceOffer, counterPrice: Int) {
        val newExpires = System.currentTimeMillis() + 45_000L
        val noteText = "عرض مضاد من الزبون: $counterPrice درهم"
        val updated = ServiceOfferEntity(
            id = offer.id,
            requestId = offer.requestId,
            craftsmanId = offer.craftsmanId,
            craftsmanName = offer.craftsmanName,
            craftsmanPhone = offer.craftsmanPhone,
            craftsmanRating = offer.craftsmanRating,
            completedJobs = offer.completedJobs,
            specialty = offer.specialty,
            offeredPrice = counterPrice,
            etaMinutes = offer.etaMinutes,
            distanceKm = offer.distanceKm,
            note = noteText,
            status = OfferStatus.COUNTERED.dbValue,
            createdAtMillis = offer.createdAtMillis,
            expiresAtMillis = newExpires
        )
        dao.upsertOffer(updated)
        firestoreMarketplaceRepo?.updateOfferStatusOrCounter(
            requestId = offer.requestId,
            offerId = offer.id,
            status = OfferStatus.COUNTERED,
            offeredPrice = counterPrice,
            note = noteText,
            expiresAtMillis = newExpires
        )
        patchOfferInSupabase(
            offer.id,
            SupabaseOfferPatchDto(status = OfferStatus.COUNTERED.dbValue, offeredPrice = counterPrice)
        )
        if (!isFirestoreAuthenticated || offer.craftsmanId.startsWith("c_")) {
            repoScope.launch {
                delay(1400L)
                val latest = dao.getOfferById(offer.id) ?: return@launch
                if (latest.status == OfferStatus.COUNTERED.dbValue) {
                    val agreed = (offer.offeredPrice + counterPrice) / 2
                    val finalPrice = if (counterPrice >= offer.offeredPrice - 20) counterPrice else agreed
                    dao.upsertOffer(
                        latest.copy(
                            offeredPrice = finalPrice,
                            note = "تمت الموافقة على التفاوض: $finalPrice درهم",
                            status = OfferStatus.PENDING.dbValue,
                            expiresAtMillis = System.currentTimeMillis() + 45_000L
                        )
                    )
                }
            }
        }
    }

    suspend fun advanceJobStatus(requestId: String, nextStatus: RequestStatus) {
        val req = dao.getRequestById(requestId) ?: return
        val updated = req.copy(status = nextStatus.dbValue, updatedAtMillis = System.currentTimeMillis())
        dao.upsertRequest(updated)
        firestoreMarketplaceRepo?.advanceRequestStatus(requestId, nextStatus)
        patchRequestInSupabase(
            requestId = requestId,
            patch = SupabaseRequestPatchDto(status = nextStatus.dbValue)
        )
    }

    suspend fun cancelRequest(requestId: String) {
        advanceJobStatus(requestId, RequestStatus.CANCELLED)
    }

    suspend fun sendChatMessage(requestId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val session = prefs.userSessionFlow.first()
        val req = dao.getRequestById(requestId)
        insertSystemWelcomeMessage(
            requestId = requestId,
            clientId = req?.clientId ?: session.userId,
            assignedCraftsmanId = req?.assignedCraftsmanId,
            senderId = session.userId,
            senderName = session.fullName,
            role = session.role,
            text = trimmed
        )
    }

    private suspend fun insertSystemWelcomeMessage(
        requestId: String,
        clientId: String,
        assignedCraftsmanId: String?,
        senderId: String,
        senderName: String,
        role: UserRole,
        text: String
    ) {
        val msg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            senderId = senderId,
            senderName = senderName,
            senderRole = role.dbValue,
            text = text,
            createdAtMillis = System.currentTimeMillis()
        )
        dao.insertMessage(msg)
        firestoreMarketplaceRepo?.sendChatMessage(
            message = msg.toDomain(),
            clientId = clientId,
            assignedCraftsmanId = assignedCraftsmanId
        )
        val session = prefs.userSessionFlow.first()
        val api = SupabaseClientProvider.getApiServiceOrNull()
        val token = session.supabaseAccessToken
        if (api != null && !token.isNullOrBlank()) {
            runCatching {
                api.postChatMessage(
                    apiKey = SupabaseConfig.anonKey,
                    bearer = "Bearer $token",
                    body = SupabaseChatMessageDto(
                        id = msg.id,
                        requestId = requestId,
                        senderId = senderId,
                        senderName = senderName,
                        senderRole = role.dbValue,
                        messageText = text
                    )
                )
            }.onFailure { Log.w(TAG, "Supabase chat post skipped/failed", it) }
        }
    }

    suspend fun submitReview(
        requestId: String,
        craftsmanId: String,
        rating: Int,
        tags: List<String>,
        comment: String
    ) {
        val review = ReviewEntity(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            craftsmanId = craftsmanId,
            rating = rating,
            tagsJoined = tags.joinToString(","),
            comment = comment,
            createdAtMillis = System.currentTimeMillis()
        )
        dao.insertReview(review)
        firestoreMarketplaceRepo?.submitReview(review.toDomain())
        val session = prefs.userSessionFlow.first()
        val api = SupabaseClientProvider.getApiServiceOrNull()
        val token = session.supabaseAccessToken
        if (api != null && !token.isNullOrBlank()) {
            runCatching {
                api.postReview(
                    apiKey = SupabaseConfig.anonKey,
                    bearer = "Bearer $token",
                    body = SupabaseReviewDto(
                        id = review.id,
                        requestId = requestId,
                        craftsmanId = craftsmanId.takeIf { it.length == 36 },
                        clientId = session.userId.takeIf { it.length == 36 },
                        rating = rating,
                        tags = tags,
                        comment = comment
                    )
                )
            }.onFailure { Log.w(TAG, "Supabase review post failed", it) }
        }
    }

    suspend fun publishCraftsmanLiveLocation(lat: Double, lng: Double, activeRequestId: String? = null) {
        val session = prefs.userSessionFlow.first()
        if (session.role == UserRole.CRAFTSMAN && isFirestoreAuthenticated) {
            firestoreMarketplaceRepo?.upsertCraftsmanDirectory(session, lat, lng)
            if (!activeRequestId.isNullOrBlank()) {
                firestoreMarketplaceRepo?.updateCraftsmanLiveLocationOnRequest(activeRequestId, lat, lng)
            }
        }
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return
        val token = session.supabaseAccessToken ?: return
        if (session.userId.length != 36) return
        runCatching {
            api.patchProfileLocation(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                idFilter = "eq.${session.userId}",
                patch = SupabaseProfileLocationPatchDto(lat = lat, lng = lng)
            )
        }.onFailure { Log.w(TAG, "Live location publish failed", it) }
    }

    suspend fun fetchAssignedCraftsmanLocation(craftsmanId: String): Pair<Double, Double>? {
        if (craftsmanId.length != 36) return null
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return null
        val session = prefs.userSessionFlow.first()
        val token = session.supabaseAccessToken ?: return null
        return runCatching {
            val response = api.getProfileLocation(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                idFilter = "eq.$craftsmanId"
            )
            if (response.isSuccessful) {
                response.body()?.firstOrNull()?.let { dto ->
                    if (dto.lat != 0.0 || dto.lng != 0.0) dto.lat to dto.lng else null
                }
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun signInWithSupabase(email: String, password: String): Result<String> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً."))
        return runCatching {
            val response = api.signIn(SupabaseConfig.anonKey, SupabaseAuthRequest(email.trim(), password))
            if (!response.isSuccessful) {
                error("فشل تسجيل الدخول (${response.code()})")
            }
            val body = response.body() ?: error("استجابة فارغة من Supabase")
            val token = body.accessToken ?: error("لم يتم استلام رمز الجلسة")
            val uid = body.user?.id ?: "supabase_user"
            prefs.saveSupabaseAuth(uid, email.trim(), token)
            val session = prefs.userSessionFlow.first()
            pushProfileToSupabase(session)
            syncFromSupabase()
            "تم تسجيل الدخول وربط حسابك مع Supabase بنجاح"
        }
    }

    suspend fun signUpWithSupabase(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        city: String
    ): Result<String> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("يرجى إضافة SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets أولاً."))
        return runCatching {
            prefs.updateProfile(fullName, phone, city)
            val response = api.signUp(SupabaseConfig.anonKey, SupabaseAuthRequest(email.trim(), password))
            if (!response.isSuccessful) {
                error("فشل إنشاء الحساب (${response.code()})")
            }
            val body = response.body()
            val token = body?.accessToken
            val uid = body?.user?.id
            if (!token.isNullOrBlank() && !uid.isNullOrBlank()) {
                prefs.saveSupabaseAuth(uid, email.trim(), token)
                val session = prefs.userSessionFlow.first()
                pushProfileToSupabase(session)
                "تم إنشاء الحساب وتفعيله على Supabase بنجاح"
            } else {
                "تم إنشاء الحساب! تحقق من بريدك الإلكتروني لتأكيد التفعيل ثم سجل الدخول."
            }
        }
    }

    suspend fun signOutSupabase() {
        prefs.clearSupabaseAuth()
    }

    suspend fun logoutAccount() {
        prefs.logoutAccount()
    }

    suspend fun syncFromSupabase(): Result<Int> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.failure(IllegalStateException("Supabase غير مفعّل بعد"))
        val session = prefs.userSessionFlow.first()
        val token = session.supabaseAccessToken
            ?: return Result.failure(IllegalStateException("قم بتسجيل الدخول أولاً للمزامنة"))
        return runCatching {
            val response = api.getServiceRequests(SupabaseConfig.anonKey, "Bearer $token")
            if (!response.isSuccessful) {
                error("تعذر جلب الطلبات (${response.code()})")
            }
            val remote = response.body().orEmpty()
            val mapped = remote.map { dto ->
                ServiceRequestEntity(
                    id = dto.id,
                    clientId = dto.clientId ?: session.userId,
                    clientName = dto.clientName,
                    categoryId = dto.categoryId,
                    categoryName = dto.categoryName,
                    title = dto.title,
                    description = dto.description,
                    addressLabel = dto.addressLabel,
                    lat = dto.lat,
                    lng = dto.lng,
                    mapX = 0.5f,
                    mapY = 0.5f,
                    proposedPrice = dto.proposedPrice,
                    acceptedPrice = dto.acceptedPrice,
                    paymentMethod = dto.paymentMethod,
                    isUrgent = dto.isUrgent,
                    status = dto.status,
                    assignedCraftsmanId = dto.assignedCraftsmanId,
                    assignedCraftsmanName = dto.assignedCraftsmanName,
                    assignedCraftsmanPhone = dto.assignedCraftsmanPhone,
                    assignedCraftsmanRating = 4.9,
                    photoUri = dto.photoUri,
                    createdAtMillis = System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )
            }
            if (mapped.isNotEmpty()) {
                dao.upsertRequests(mapped)
            }
            mapped.size
        }
    }

    suspend fun syncOffersForRequestFromSupabase(requestId: String): Result<Int> {
        val api = SupabaseClientProvider.getApiServiceOrNull()
            ?: return Result.success(0)
        val session = prefs.userSessionFlow.first()
        val token = session.supabaseAccessToken ?: return Result.success(0)
        return runCatching {
            val response = api.getOffersForRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                requestIdFilter = "eq.$requestId"
            )
            if (!response.isSuccessful) {
                error("تعذر جلب العروض (${response.code()})")
            }
            val now = System.currentTimeMillis()
            val remote = response.body().orEmpty()
                .let { list ->
                    if (session.role == UserRole.CRAFTSMAN) {
                        list.filter { it.craftsmanId == session.userId }
                    } else {
                        list
                    }
                }
            val mapped = remote.map { dto ->
                val existing = dao.getOfferById(dto.id)
                ServiceOfferEntity(
                    id = dto.id,
                    requestId = dto.requestId,
                    craftsmanId = dto.craftsmanId ?: existing?.craftsmanId ?: "remote_craftsman",
                    craftsmanName = dto.craftsmanName,
                    craftsmanPhone = dto.craftsmanPhone,
                    craftsmanRating = dto.craftsmanRating,
                    completedJobs = dto.completedJobs,
                    specialty = dto.specialty,
                    offeredPrice = dto.offeredPrice,
                    etaMinutes = dto.etaMinutes,
                    distanceKm = dto.distanceKm,
                    note = dto.note,
                    status = dto.status,
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    expiresAtMillis = existing?.expiresAtMillis ?: (now + 55_000L)
                )
            }
            if (mapped.isNotEmpty()) {
                dao.upsertOffers(mapped)
            }
            mapped.size
        }
    }

    private suspend fun pushProfileToSupabase(session: UserSession) {
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return
        val token = session.supabaseAccessToken ?: return
        runCatching {
            api.upsertProfile(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                profile = SupabaseProfileDto(
                    id = session.userId,
                    fullName = session.fullName,
                    phone = session.phone,
                    city = session.city,
                    role = session.role.dbValue,
                    specialty = HirafiCategories.byId(session.craftsmanSpecialtyId).nameAr,
                    isOnline = session.isCraftsmanOnline
                )
            )
        }.onFailure { Log.w(TAG, "Profile sync failed", it) }
    }

    private suspend fun pushRequestToSupabase(entity: ServiceRequestEntity, session: UserSession) {
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return
        val token = session.supabaseAccessToken ?: return
        runCatching {
            api.createServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                body = SupabaseServiceRequestDto(
                    id = entity.id,
                    clientId = session.userId.takeIf { it.length == 36 },
                    clientName = entity.clientName,
                    categoryId = entity.categoryId,
                    categoryName = entity.categoryName,
                    title = entity.title,
                    description = entity.description,
                    addressLabel = entity.addressLabel,
                    lat = entity.lat,
                    lng = entity.lng,
                    proposedPrice = entity.proposedPrice,
                    acceptedPrice = entity.acceptedPrice,
                    paymentMethod = entity.paymentMethod,
                    isUrgent = entity.isUrgent,
                    status = entity.status,
                    photoUri = entity.photoUri
                )
            )
        }.onFailure { Log.w(TAG, "Supabase create request failed", it) }
    }

    private suspend fun patchRequestInSupabase(requestId: String, patch: SupabaseRequestPatchDto) {
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return
        val session = prefs.userSessionFlow.first()
        val token = session.supabaseAccessToken ?: return
        runCatching {
            api.patchServiceRequest(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                idFilter = "eq.$requestId",
                patch = patch
            )
        }.onFailure { Log.w(TAG, "Supabase patch request failed", it) }
    }

    private suspend fun pushOfferToSupabase(offer: ServiceOfferEntity, session: UserSession) {
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return
        val token = session.supabaseAccessToken ?: return
        runCatching {
            api.createOffer(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                body = SupabaseServiceOfferDto(
                    id = offer.id,
                    requestId = offer.requestId,
                    craftsmanId = session.userId.takeIf { it.length == 36 },
                    craftsmanName = offer.craftsmanName,
                    craftsmanPhone = offer.craftsmanPhone,
                    craftsmanRating = offer.craftsmanRating,
                    completedJobs = offer.completedJobs,
                    specialty = offer.specialty,
                    offeredPrice = offer.offeredPrice,
                    etaMinutes = offer.etaMinutes,
                    distanceKm = offer.distanceKm,
                    note = offer.note,
                    status = offer.status
                )
            )
        }.onFailure { Log.w(TAG, "Supabase create offer failed", it) }
    }

    private suspend fun patchOfferInSupabase(offerId: String, patch: SupabaseOfferPatchDto) {
        val api = SupabaseClientProvider.getApiServiceOrNull() ?: return
        val session = prefs.userSessionFlow.first()
        val token = session.supabaseAccessToken ?: return
        runCatching {
            api.patchOffer(
                apiKey = SupabaseConfig.anonKey,
                bearer = "Bearer $token",
                idFilter = "eq.$offerId",
                patch = patch
            )
        }.onFailure { Log.w(TAG, "Supabase patch offer failed", it) }
    }

    private fun ServiceRequest.toEntity() = ServiceRequestEntity(
        id = id,
        clientId = clientId,
        clientName = clientName,
        categoryId = categoryId,
        categoryName = categoryName,
        title = title,
        description = description,
        addressLabel = addressLabel,
        lat = lat,
        lng = lng,
        mapX = mapX,
        mapY = mapY,
        proposedPrice = proposedPrice,
        acceptedPrice = acceptedPrice,
        paymentMethod = paymentMethod.dbValue,
        isUrgent = isUrgent,
        status = status.dbValue,
        assignedCraftsmanId = assignedCraftsmanId,
        assignedCraftsmanName = assignedCraftsmanName,
        assignedCraftsmanPhone = assignedCraftsmanPhone,
        assignedCraftsmanRating = assignedCraftsmanRating,
        photoUri = photoUri,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    private fun ServiceOffer.toEntity() = ServiceOfferEntity(
        id = id,
        requestId = requestId,
        craftsmanId = craftsmanId,
        craftsmanName = craftsmanName,
        craftsmanPhone = craftsmanPhone,
        craftsmanRating = craftsmanRating,
        completedJobs = completedJobs,
        specialty = specialty,
        offeredPrice = offeredPrice,
        etaMinutes = etaMinutes,
        distanceKm = distanceKm,
        note = note,
        status = status.dbValue,
        createdAtMillis = createdAtMillis,
        expiresAtMillis = expiresAtMillis
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id,
        requestId = requestId,
        senderId = senderId,
        senderName = senderName,
        senderRole = senderRole.dbValue,
        text = text,
        createdAtMillis = createdAtMillis
    )

    private fun PortfolioItem.toEntity() = PortfolioItemEntity(
        id = id,
        craftsmanId = craftsmanId,
        title = title,
        description = description,
        categoryName = categoryName,
        photoUri = photoUri,
        createdAtMillis = createdAtMillis
    )

    private fun PortfolioItemEntity.toDomain() = PortfolioItem(
        id = id,
        craftsmanId = craftsmanId,
        title = title,
        description = description,
        categoryName = categoryName,
        photoUri = photoUri,
        createdAtMillis = createdAtMillis
    )

    private fun ServiceRequestEntity.toDomain() = ServiceRequest(
        id = id,
        clientId = clientId,
        clientName = clientName,
        categoryId = categoryId,
        categoryName = categoryName,
        title = title,
        description = description,
        addressLabel = addressLabel,
        lat = lat,
        lng = lng,
        mapX = mapX,
        mapY = mapY,
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
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    private fun ServiceOfferEntity.toDomain() = ServiceOffer(
        id = id,
        requestId = requestId,
        craftsmanId = craftsmanId,
        craftsmanName = craftsmanName,
        craftsmanPhone = craftsmanPhone,
        craftsmanRating = craftsmanRating,
        completedJobs = completedJobs,
        specialty = specialty,
        offeredPrice = offeredPrice,
        etaMinutes = etaMinutes,
        distanceKm = ((distanceKm * 10.0).roundToInt() / 10.0),
        note = note,
        status = OfferStatus.fromDb(status),
        createdAtMillis = createdAtMillis,
        expiresAtMillis = expiresAtMillis
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        requestId = requestId,
        senderId = senderId,
        senderName = senderName,
        senderRole = if (senderRole == UserRole.CRAFTSMAN.dbValue) UserRole.CRAFTSMAN else UserRole.CLIENT,
        text = text,
        createdAtMillis = createdAtMillis
    )

    private fun ReviewEntity.toDomain() = ReviewRecord(
        id = id,
        requestId = requestId,
        craftsmanId = craftsmanId,
        rating = rating,
        tags = tagsJoined.split(",").filter { it.isNotBlank() },
        comment = comment,
        createdAtMillis = createdAtMillis
    )

    companion object {
        private const val TAG = "HirafiRepository"

        fun computeCraftsmenAround(
            centerLat: Double = 33.5822,
            centerLng: Double = -7.6324
        ): List<CraftsmanPin> {
            data class Seed(
                val id: String,
                val name: String,
                val specialtyId: String,
                val rating: Double,
                val jobs: Int,
                val dLat: Double,
                val dLng: Double,
                val basePrice: Int,
                val phone: String,
                val years: Int,
                val bio: String
            )

            val seeds = listOf(
                Seed("c_karim", "كريم بنعلي", "plumbing", 4.9, 184, 0.0065, -0.0052, 120, "+212 661-234510", 8, "متخصص في كشف وإصلاح التسربات والترصيص الصحي المنزلي"),
                Seed("c_hamza", "حمزة السليماني", "plumbing", 4.7, 96, -0.0115, 0.0125, 110, "+212 664-112233", 5, "تركيب سخانات المياه، الصنابير، وإصلاح الأعطال المستعجلة"),
                Seed("c_omar", "عمر التازي", "electricity", 4.8, 142, 0.0042, 0.0110, 140, "+212 662-887120", 7, "صيانة اللوحات الكهربائية، القواطع، وتمديد الأسلاك بأمان"),
                Seed("c_amine", "أمين بنجلون", "electricity", 4.92, 203, -0.0140, -0.0130, 150, "+212 668-550190", 9, "تشخيص الأعطال الكهربائية المنزلية وتركيب الإضاءة الذكية"),
                Seed("c_rachid", "رشيد الفاسي", "ac", 4.9, 215, -0.0088, -0.0045, 180, "+212 663-449012", 10, "تعبئة غاز المكيفات، تنظيف الفلاتر، وإصلاح التبريد"),
                Seed("c_badr", "بدر الشرقاوي", "ac", 4.78, 118, 0.0165, 0.0145, 190, "+212 669-221478", 6, "صيانة المكيفات المنفصلة والمركزية بضمان على الخدمة"),
                Seed("c_youssef", "يوسف الإدريسي", "carpentry", 4.85, 129, 0.0028, -0.0115, 150, "+212 665-998877", 8, "إصلاح الأبواب والنوافذ الخشبية وخزائن المطبخ"),
                Seed("c_hicham", "هشام الزروالي", "carpentry", 4.72, 74, 0.0190, -0.0080, 140, "+212 670-334890", 5, "نجارة الخشب والألمنيوم وتركيب الأثاث المنزلي"),
                Seed("c_salim", "سليم أمين", "painting", 4.75, 88, -0.0105, 0.0048, 220, "+212 667-334455", 7, "صباغة الجدران الداخلية والديكور العصري بدون بقع"),
                Seed("c_fouad", "فؤاد المرابط", "painting", 4.88, 162, 0.0210, 0.0095, 240, "+212 671-889044", 11, "معالجة الرطوبة والصباغة المائية والزيتية بسرعة وإتقان"),
                Seed("c_mourad", "مراد الشرقاوي", "locksmith", 4.95, 310, 0.0045, 0.0032, 100, "+212 666-771122", 12, "فتح الأقفال المغلقة بدون إتلاف وتبديل الأسطوانات فوراً"),
                Seed("c_zakaria", "زكرياء الحلو", "locksmith", 4.81, 136, -0.0155, 0.0160, 95, "+212 672-665544", 6, "نسخ المفاتيح المشفرة وإصلاح أقفال الأبواب المصفحة"),
                Seed("c_hassan", "حسن الودغيري", "masonry", 4.84, 151, 0.0095, -0.0135, 210, "+212 673-443322", 14, "ترميم الجدران، تركيب الزليج والرخام وإصلاح التسربات"),
                Seed("c_driss", "إدريس الناصري", "masonry", 4.76, 92, -0.0220, -0.0070, 200, "+212 674-119988", 9, "أشغال البناء الصغيرة، الجبس، وتجديد الحمامات والمطابخ"),
                Seed("c_nadia_pro", "خديجة السفياني", "cleaning", 4.93, 245, -0.0048, 0.0075, 120, "+212 675-778899", 7, "تنظيف عميق للمنازل والمفروشات بمواد آمنة ومعقمة"),
                Seed("c_samira", "سميرة العلمي", "cleaning", 4.82, 114, 0.0145, -0.0165, 110, "+212 676-223344", 5, "تنظيف الشقق بعد الإصلاحات والزجاج والمطابخ")
            )

            return seeds.map { s ->
                val pLat = centerLat + s.dLat
                val pLng = centerLng + s.dLng
                val dist = MapRoutingService.haversineKm(centerLat, centerLng, pLat, pLng).coerceAtLeast(0.3)
                val eta = (dist * 4.5).roundToInt().coerceIn(4, 35)
                CraftsmanPin(
                    id = s.id,
                    name = s.name,
                    specialtyId = s.specialtyId,
                    specialtyName = HirafiCategories.byId(s.specialtyId).nameAr,
                    rating = s.rating,
                    completedJobs = s.jobs,
                    lat = pLat,
                    lng = pLng,
                    basePriceDh = s.basePrice,
                    distanceKm = dist,
                    etaMinutes = eta,
                    phone = s.phone,
                    isVerified = true,
                    experienceYears = s.years,
                    bio = s.bio,
                    isLiveCloud = false
                )
            }.sortedBy { it.distanceKm }
        }
    }
}
