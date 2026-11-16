package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.location.LiveLocationBus
import com.example.data.location.LiveTrackingState
import com.example.data.location.RealTimeLocationService
import com.example.data.location.RealTimeLocationSnapshot
import com.example.data.model.ChatMessage
import com.example.data.model.CraftsmanPin
import com.example.data.model.CraftsmanSortOption
import com.example.data.model.HirafiCategories
import com.example.data.model.LiveRouteState
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RequestStatus
import com.example.data.model.ReviewRecord
import com.example.data.model.SearchPlaceResult
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.remote.MapRoutingService
import com.example.data.remote.SupabaseConfig
import com.example.data.repository.HirafiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DraftRequestState(
    val selectedCategoryId: String = "plumbing",
    val title: String = "",
    val description: String = "",
    val proposedPrice: Int = HirafiCategories.byId("plumbing").defaultPriceDh,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isUrgent: Boolean = true,
    val photoUri: String? = null,
    val addressLabel: String = MoroccanLocations.neighborhoods.first().nameAr,
    val lat: Double = MoroccanLocations.neighborhoods.first().lat,
    val lng: Double = MoroccanLocations.neighborhoods.first().lng,
    val isGpsActive: Boolean = false,
    val isResolvingAddress: Boolean = false
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

private data class CraftsmanFilterParams(
    val query: String,
    val radiusKm: Int,
    val sort: CraftsmanSortOption,
    val onlySelectedCategory: Boolean,
    val selectedCategoryId: String
)

class HirafiViewModel(
    private val repository: HirafiRepository,
    private val locationService: RealTimeLocationService? = null
) : ViewModel() {

    val userSession: StateFlow<UserSession> = repository.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSession()
    )

    val allRequests: StateFlow<List<ServiceRequest>> = repository.observeRequests().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val allReviews: StateFlow<List<ReviewRecord>> = repository.observeReviews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _draft = MutableStateFlow(DraftRequestState())
    val draft: StateFlow<DraftRequestState> = _draft.asStateFlow()

    private val _nearbyCraftsmen = MutableStateFlow(
        repository.getCraftsmenAround(
            MoroccanLocations.neighborhoods.first().lat,
            MoroccanLocations.neighborhoods.first().lng
        )
    )
    val nearbyCraftsmenFlow: StateFlow<List<CraftsmanPin>> = _nearbyCraftsmen.asStateFlow()

    private val _craftsmanSearchQuery = MutableStateFlow("")
    val craftsmanSearchQuery: StateFlow<String> = _craftsmanSearchQuery.asStateFlow()

    private val _searchRadiusKm = MutableStateFlow(5)
    val searchRadiusKm: StateFlow<Int> = _searchRadiusKm.asStateFlow()

    private val _craftsmanSort = MutableStateFlow(CraftsmanSortOption.NEAREST)
    val craftsmanSort: StateFlow<CraftsmanSortOption> = _craftsmanSort.asStateFlow()

    private val _onlySelectedCategoryOnMap = MutableStateFlow(false)
    val onlySelectedCategoryOnMap: StateFlow<Boolean> = _onlySelectedCategoryOnMap.asStateFlow()

    private val _offerSort = MutableStateFlow(OfferSortOption.BEST_PRICE)
    val offerSort: StateFlow<OfferSortOption> = _offerSort.asStateFlow()

    private val _userGpsLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userGpsLocation: StateFlow<Pair<Double, Double>?> = _userGpsLocation.asStateFlow()

    private val _cameraTarget = MutableStateFlow<Pair<Double, Double>?>(null)
    val cameraTarget: StateFlow<Pair<Double, Double>?> = _cameraTarget.asStateFlow()

    private val _liveRoute = MutableStateFlow(LiveRouteState())
    val liveRoute: StateFlow<LiveRouteState> = _liveRoute.asStateFlow()

    private val _placeSearchResults = MutableStateFlow<List<SearchPlaceResult>>(emptyList())
    val placeSearchResults: StateFlow<List<SearchPlaceResult>> = _placeSearchResults.asStateFlow()

    private val _isSearchingPlaces = MutableStateFlow(false)
    val isSearchingPlaces: StateFlow<Boolean> = _isSearchingPlaces.asStateFlow()

    private val _activeClientRequestId = MutableStateFlow<String?>(null)
    val activeClientRequestId: StateFlow<String?> = _activeClientRequestId.asStateFlow()

    private val _activeChatRequestId = MutableStateFlow<String?>(null)
    val activeChatRequestId: StateFlow<String?> = _activeChatRequestId.asStateFlow()

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _pendingReviewRequest = MutableStateFlow<ServiceRequest?>(null)
    val pendingReviewRequest: StateFlow<ServiceRequest?> = _pendingReviewRequest.asStateFlow()

    private val _trackingState = MutableStateFlow(
        LiveTrackingState(hasLocationPermission = locationService?.hasLocationPermission() == true)
    )
    val trackingState: StateFlow<LiveTrackingState> = _trackingState.asStateFlow()

    private var geocodeJob: Job? = null
    private var trackingJob: Job? = null
    private var gpsStreamJob: Job? = null
    private var activeTrackedRequestKey: String? = null
    private var lastCraftsmenCenter: Pair<Double, Double>? = null

    val tickerMillis: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(500L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), System.currentTimeMillis())

    val filteredNearbyCraftsmen: StateFlow<List<CraftsmanPin>> = combine(
        _nearbyCraftsmen,
        combine(
            _craftsmanSearchQuery,
            _searchRadiusKm,
            _craftsmanSort,
            _onlySelectedCategoryOnMap,
            _draft
        ) { query, radius, sort, onlyCat, draftState ->
            CraftsmanFilterParams(
                query = query,
                radiusKm = radius,
                sort = sort,
                onlySelectedCategory = onlyCat,
                selectedCategoryId = draftState.selectedCategoryId
            )
        }
    ) { pins, params ->
        val q = params.query.trim()
        val filtered = pins.filter { pin ->
            val withinRadius = params.radiusKm >= 20 || pin.distanceKm <= params.radiusKm.toDouble()
            val matchesCat = !params.onlySelectedCategory || pin.specialtyId == params.selectedCategoryId
            val matchesQuery = q.isEmpty() ||
                pin.name.contains(q, ignoreCase = true) ||
                pin.specialtyName.contains(q, ignoreCase = true) ||
                pin.bio.contains(q, ignoreCase = true)
            withinRadius && matchesCat && matchesQuery
        }
        when (params.sort) {
            CraftsmanSortOption.NEAREST -> filtered.sortedBy { it.distanceKm }
            CraftsmanSortOption.TOP_RATED -> filtered.sortedWith(
                compareByDescending<CraftsmanPin> { it.rating }.thenBy { it.distanceKm }
            )
            CraftsmanSortOption.LOWEST_PRICE -> filtered.sortedWith(
                compareBy<CraftsmanPin> { it.basePriceDh }.thenBy { it.distanceKm }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), _nearbyCraftsmen.value)

    val activeClientRequest: StateFlow<ServiceRequest?> = combine(
        allRequests,
        _activeClientRequestId,
        userSession
    ) { requests, explicitId, session ->
        if (explicitId != null) {
            requests.firstOrNull { it.id == explicitId }
        } else {
            requests.firstOrNull {
                it.clientId == session.userId &&
                    it.status != RequestStatus.COMPLETED &&
                    it.status != RequestStatus.CANCELLED
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val craftsmanOpenRequests: StateFlow<List<ServiceRequest>> = combine(
        allRequests,
        userSession
    ) { requests, session ->
        if (!session.isCraftsmanSpecialtyLocked || !session.isCraftsmanOnline) {
            emptyList()
        } else {
            requests.filter { req ->
                req.status == RequestStatus.SEARCHING &&
                    req.categoryId == session.craftsmanSpecialtyId
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeOffers: StateFlow<List<ServiceOffer>> = activeClientRequest
        .flatMapLatest { req ->
            if (req == null) flowOf(emptyList()) else repository.observeOffersForRequest(req.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sortedActiveOffers: StateFlow<List<ServiceOffer>> = combine(
        activeOffers,
        _offerSort,
        tickerMillis
    ) { offers, sort, now ->
        val visible = offers.filter { offer ->
            val activeStatus = offer.status == OfferStatus.PENDING || offer.status == OfferStatus.COUNTERED
            val notExpired = offer.expiresAtMillis > now || offer.status == OfferStatus.COUNTERED
            activeStatus && notExpired
        }
        when (sort) {
            OfferSortOption.BEST_PRICE -> visible.sortedWith(
                compareBy<ServiceOffer> { it.offeredPrice }.thenBy { it.etaMinutes }
            )
            OfferSortOption.FASTEST_ETA -> visible.sortedWith(
                compareBy<ServiceOffer> { it.etaMinutes }.thenBy { it.offeredPrice }
            )
            OfferSortOption.TOP_RATED -> visible.sortedWith(
                compareByDescending<ServiceOffer> { it.craftsmanRating }.thenBy { it.offeredPrice }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatRequestId
        .flatMapLatest { reqId ->
            if (reqId == null) flowOf(emptyList()) else repository.observeMessagesForRequest(reqId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val isSupabaseConfigured: Boolean
        get() = SupabaseConfig.isConfigured

    init {
        viewModelScope.launch {
            repository.seedInitialMarketplaceRequestsIfEmpty()
        }
        viewModelScope.launch {
            combine(allRequests, userSession, _activeClientRequestId) { requests, session, explicitId ->
                if (session.role == UserRole.CRAFTSMAN) {
                    requests.firstOrNull {
                        it.assignedCraftsmanId == session.userId &&
                            it.status != RequestStatus.COMPLETED &&
                            it.status != RequestStatus.CANCELLED
                    }
                } else {
                    if (explicitId != null) {
                        requests.firstOrNull { it.id == explicitId }
                    } else {
                        requests.firstOrNull {
                            it.clientId == session.userId &&
                                it.status != RequestStatus.COMPLETED &&
                                it.status != RequestStatus.CANCELLED
                        }
                    }
                }
            }.collect { req ->
                if (req == null ||
                    req.status == RequestStatus.SEARCHING ||
                    req.status == RequestStatus.CANCELLED ||
                    req.status == RequestStatus.COMPLETED
                ) {
                    activeTrackedRequestKey = null
                    trackingJob?.cancel()
                    locationService?.stopForegroundJobTracking()
                    _liveRoute.value = LiveRouteState()
                    _trackingState.update {
                        it.copy(
                            isTracking = false,
                            isForegroundServiceActive = false,
                            trackedRequestId = null,
                            hasLocationPermission = locationService?.hasLocationPermission() == true
                        )
                    }
                } else {
                    val key = "${req.id}_${req.assignedCraftsmanId}_${req.status.dbValue}"
                    if (activeTrackedRequestKey != key) {
                        activeTrackedRequestKey = key
                        startLiveRouteTracking(req)
                    }
                }
            }
        }
    }

    fun refreshLocationTracking(context: Context) {
        val svc = locationService ?: RealTimeLocationService(context.applicationContext)
        val hasPerm = svc.hasLocationPermission()
        _trackingState.update { it.copy(hasLocationPermission = hasPerm) }
        val currentSession = userSession.value
        val activeJob = if (currentSession.role == UserRole.CRAFTSMAN) {
            allRequests.value.firstOrNull {
                it.assignedCraftsmanId == currentSession.userId &&
                    it.status != RequestStatus.COMPLETED &&
                    it.status != RequestStatus.CANCELLED &&
                    it.status != RequestStatus.SEARCHING
            }
        } else {
            activeClientRequest.value?.takeIf {
                it.status != RequestStatus.SEARCHING &&
                    it.status != RequestStatus.COMPLETED &&
                    it.status != RequestStatus.CANCELLED
            }
        }
        if (activeJob != null && hasPerm) {
            activeTrackedRequestKey = "${activeJob.id}_${activeJob.assignedCraftsmanId}_${activeJob.status.dbValue}_gps"
            startLiveRouteTracking(activeJob)
        }
    }

    private fun startLiveRouteTracking(req: ServiceRequest) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            val isCraftsman = userSession.value.role == UserRole.CRAFTSMAN
            val hasPerm = locationService?.hasLocationPermission() == true
            val gps = _userGpsLocation.value
            val assignedPin = _nearbyCraftsmen.value.firstOrNull { it.id == req.assignedCraftsmanId }
                ?: _nearbyCraftsmen.value.firstOrNull()
            val startLat = if (isCraftsman && gps != null) gps.first else (assignedPin?.lat ?: (req.lat + 0.009))
            val startLng = if (isCraftsman && gps != null) gps.second else (assignedPin?.lng ?: (req.lng - 0.008))

            _trackingState.update {
                it.copy(
                    isTracking = true,
                    hasLocationPermission = hasPerm,
                    trackedRequestId = req.id,
                    errorMessage = null
                )
            }

            if (isCraftsman) {
                trackAsCraftsmanWithPlayServices(req, startLat, startLng)
            } else {
                trackCraftsmanForClient(req, startLat, startLng)
            }
        }
    }

    private suspend fun trackAsCraftsmanWithPlayServices(
        req: ServiceRequest,
        fallbackLat: Double,
        fallbackLng: Double
    ) {
        val svc = locationService
        val hasPerm = svc?.hasLocationPermission() == true
        if (!hasPerm) {
            // Compute initial route using known fallback so map stays informative until permission granted
            val initialRoute = MapRoutingService.fetchDrivingRoute(fallbackLat, fallbackLng, req.lat, req.lng)
            _liveRoute.value = LiveRouteState(
                routePoints = if (req.status == RequestStatus.ARRIVED || req.status == RequestStatus.IN_PROGRESS) emptyList() else initialRoute.points,
                craftsmanLat = fallbackLat,
                craftsmanLng = fallbackLng,
                remainingDistanceKm = if (req.status == RequestStatus.ARRIVED || req.status == RequestStatus.IN_PROGRESS) 0.0 else initialRoute.distanceKm,
                etaMinutes = if (req.status == RequestStatus.ARRIVED || req.status == RequestStatus.IN_PROGRESS) 0 else initialRoute.durationMinutes
            )
            _trackingState.update {
                it.copy(
                    isTracking = true,
                    hasLocationPermission = false,
                    isForegroundServiceActive = false,
                    errorMessage = "يرجى تفعيل صلاحية الموقع للتتبع المباشر"
                )
            }
            return
        }

        // Start foreground location service for continuous GPS tracking during active job (even during Google Maps navigation)
        if (req.status == RequestStatus.ACCEPTED || req.status == RequestStatus.EN_ROUTE) {
            svc?.startForegroundJobTracking(req.id, req.title)
        } else {
            svc?.stopForegroundJobTracking()
        }

        var lastRoutedLat = fallbackLat
        var lastRoutedLng = fallbackLng
        var lastRouteFetchMs = 0L
        var lastSupabaseSyncMs = 0L
        var cachedPolyline: List<Pair<Double, Double>> = emptyList()

        suspend fun applyCraftsmanFix(fix: RealTimeLocationSnapshot) {
            _userGpsLocation.value = fix.lat to fix.lng
            _trackingState.update {
                it.copy(
                    isTracking = true,
                    hasLocationPermission = true,
                    isForegroundServiceActive = LiveLocationBus.isForegroundServiceActive.value,
                    latestFix = fix,
                    errorMessage = null
                )
            }

            val distKm = MapRoutingService.haversineKm(fix.lat, fix.lng, req.lat, req.lng)
            val arrivedOrWorking = req.status == RequestStatus.ARRIVED || req.status == RequestStatus.IN_PROGRESS || distKm <= 0.08
            val nowMs = System.currentTimeMillis()
            val movedKm = MapRoutingService.haversineKm(lastRoutedLat, lastRoutedLng, fix.lat, fix.lng)

            if (!arrivedOrWorking && (cachedPolyline.isEmpty() || movedKm >= 0.04 || nowMs - lastRouteFetchMs >= 12_000L)) {
                lastRoutedLat = fix.lat
                lastRoutedLng = fix.lng
                lastRouteFetchMs = nowMs
                val route = MapRoutingService.fetchDrivingRoute(fix.lat, fix.lng, req.lat, req.lng)
                cachedPolyline = route.points
            }

            val eta = if (arrivedOrWorking) 0 else RealTimeLocationService.computeEtaMinutes(distKm, fix.speedMps)
            _liveRoute.value = LiveRouteState(
                routePoints = if (arrivedOrWorking) emptyList() else cachedPolyline,
                craftsmanLat = fix.lat,
                craftsmanLng = fix.lng,
                remainingDistanceKm = if (arrivedOrWorking) 0.0 else distKm,
                etaMinutes = eta
            )

            if (nowMs - lastSupabaseSyncMs >= 5000L) {
                lastSupabaseSyncMs = nowMs
                repository.publishCraftsmanLiveLocation(fix.lat, fix.lng)
            }
        }

        // Seed immediately with high-accuracy Play Services fix or fallback
        val immediate = svc?.getCurrentFix()
        if (immediate != null) {
            applyCraftsmanFix(immediate)
        } else {
            applyCraftsmanFix(
                RealTimeLocationSnapshot(
                    lat = fallbackLat,
                    lng = fallbackLng,
                    source = com.example.data.location.LocationSource.LAST_KNOWN
                )
            )
        }

        // Continuously collect Play Services real-time updates (from direct callbackFlow & LiveLocationBus)
        val liveFlow = if (svc != null) {
            merge(
                svc.observeRealTimeLocation(intervalMillis = 3500L, minDistanceMeters = 5f),
                LiveLocationBus.latestCraftsmanFix.let { busFlow ->
                    kotlinx.coroutines.flow.flow {
                        busFlow.collect { snap -> if (snap != null) emit(snap) }
                    }
                }
            )
        } else {
            return
        }

        liveFlow.collect { fix ->
            applyCraftsmanFix(fix)
        }
    }

    private suspend fun trackCraftsmanForClient(
        req: ServiceRequest,
        initialCraftsmanLat: Double,
        initialCraftsmanLng: Double
    ) {
        val route = MapRoutingService.fetchDrivingRoute(
            startLat = initialCraftsmanLat,
            startLng = initialCraftsmanLng,
            endLat = req.lat,
            endLng = req.lng
        )
        val pts = route.points
        if (pts.isEmpty()) return

        when (req.status) {
            RequestStatus.ACCEPTED -> {
                val remotePos = req.assignedCraftsmanId?.let { repository.fetchAssignedCraftsmanLocation(it) }
                val cLat = remotePos?.first ?: pts.first().first
                val cLng = remotePos?.second ?: pts.first().second
                val activeRoute = if (remotePos != null) {
                    MapRoutingService.fetchDrivingRoute(cLat, cLng, req.lat, req.lng)
                } else route
                _liveRoute.value = LiveRouteState(
                    routePoints = activeRoute.points,
                    craftsmanLat = cLat,
                    craftsmanLng = cLng,
                    remainingDistanceKm = activeRoute.distanceKm,
                    etaMinutes = activeRoute.durationMinutes
                )
            }
            RequestStatus.EN_ROUTE -> {
                val assignedId = req.assignedCraftsmanId
                val isRemoteCraftsmanUuid = assignedId != null && assignedId.length == 36
                if (isRemoteCraftsmanUuid && SupabaseConfig.isConfigured) {
                    // Real-time remote telemetry polling loop synced with craftsman's Play Services GPS
                    while (true) {
                        val liveCoord = repository.fetchAssignedCraftsmanLocation(assignedId)
                            ?: LiveLocationBus.latestCraftsmanFix.value?.let { it.lat to it.lng }
                        if (liveCoord != null) {
                            val dist = MapRoutingService.haversineKm(liveCoord.first, liveCoord.second, req.lat, req.lng)
                            val updatedRoute = MapRoutingService.fetchDrivingRoute(liveCoord.first, liveCoord.second, req.lat, req.lng)
                            _liveRoute.value = LiveRouteState(
                                routePoints = updatedRoute.points,
                                craftsmanLat = liveCoord.first,
                                craftsmanLng = liveCoord.second,
                                remainingDistanceKm = if (dist <= 0.08) 0.0 else updatedRoute.distanceKm,
                                etaMinutes = if (dist <= 0.08) 0 else updatedRoute.durationMinutes
                            )
                        }
                        delay(4000L)
                    }
                } else {
                    // Smooth route-following live progression for local/verified offline craftsmen
                    val totalSteps = pts.size.coerceAtLeast(2)
                    for (idx in 0 until totalSteps) {
                        val liveBusOverride = LiveLocationBus.latestCraftsmanFix.value
                        if (liveBusOverride != null && req.assignedCraftsmanId == userSession.value.userId) {
                            val dist = MapRoutingService.haversineKm(liveBusOverride.lat, liveBusOverride.lng, req.lat, req.lng)
                            _liveRoute.value = LiveRouteState(
                                routePoints = pts.subList(idx, pts.size),
                                craftsmanLat = liveBusOverride.lat,
                                craftsmanLng = liveBusOverride.lng,
                                remainingDistanceKm = dist,
                                etaMinutes = RealTimeLocationService.computeEtaMinutes(dist, liveBusOverride.speedMps)
                            )
                        } else {
                            val currentPos = pts[idx]
                            val fractionRemaining = 1.0 - (idx.toDouble() / (totalSteps - 1).coerceAtLeast(1))
                            val remDist = ((route.distanceKm * fractionRemaining) * 10.0).roundToInt() / 10.0
                            val remEta = (route.durationMinutes * fractionRemaining).roundToInt().coerceAtLeast(1)
                            _liveRoute.value = LiveRouteState(
                                routePoints = pts.subList(idx, pts.size),
                                craftsmanLat = currentPos.first,
                                craftsmanLng = currentPos.second,
                                remainingDistanceKm = remDist,
                                etaMinutes = remEta
                            )
                        }
                        delay(1100L)
                    }
                    _liveRoute.value = LiveRouteState(
                        routePoints = listOf(req.lat to req.lng),
                        craftsmanLat = req.lat,
                        craftsmanLng = req.lng,
                        remainingDistanceKm = 0.0,
                        etaMinutes = 0
                    )
                }
            }
            RequestStatus.ARRIVED, RequestStatus.IN_PROGRESS -> {
                _liveRoute.value = LiveRouteState(
                    routePoints = emptyList(),
                    craftsmanLat = req.lat,
                    craftsmanLng = req.lng,
                    remainingDistanceKm = 0.0,
                    etaMinutes = 0
                )
            }
            else -> {}
        }
    }

    fun selectCategory(categoryId: String) {
        val category = HirafiCategories.byId(categoryId)
        _draft.update {
            it.copy(
                selectedCategoryId = categoryId,
                proposedPrice = category.defaultPriceDh
            )
        }
    }

    fun updateDraftTitle(title: String) {
        _draft.update { it.copy(title = title) }
    }

    fun updateDraftDescription(description: String) {
        _draft.update { it.copy(description = description) }
    }

    fun adjustDraftPrice(delta: Int) {
        _draft.update {
            it.copy(proposedPrice = (it.proposedPrice + delta).coerceIn(40, 5000))
        }
    }

    fun setDraftPrice(price: Int) {
        _draft.update {
            it.copy(proposedPrice = price.coerceIn(40, 5000))
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _draft.update { it.copy(paymentMethod = method) }
    }

    fun toggleUrgent(isUrgent: Boolean) {
        _draft.update { it.copy(isUrgent = isUrgent) }
    }

    fun setDraftPhotoUri(uri: String?) {
        _draft.update { it.copy(photoUri = uri) }
    }

    fun onMapPinMoved(context: Context, lat: Double, lng: Double, moveCamera: Boolean = false) {
        if (moveCamera) {
            _cameraTarget.value = lat to lng
        }
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            delay(450L)
            _draft.update {
                it.copy(
                    lat = lat,
                    lng = lng,
                    isGpsActive = false,
                    isResolvingAddress = true
                )
            }
            val prev = lastCraftsmenCenter
            if (prev == null || MapRoutingService.haversineKm(prev.first, prev.second, lat, lng) >= 0.25) {
                lastCraftsmenCenter = lat to lng
                _nearbyCraftsmen.value = repository.getCraftsmenAround(lat, lng)
            }
            val resolved = MapRoutingService.reverseGeocode(context, lat, lng)
            _draft.update {
                it.copy(
                    addressLabel = resolved,
                    isResolvingAddress = false
                )
            }
        }
    }

    fun selectNeighborhood(label: String, lat: Double, lng: Double) {
        geocodeJob?.cancel()
        _draft.update {
            it.copy(
                addressLabel = label,
                lat = lat,
                lng = lng,
                isGpsActive = false,
                isResolvingAddress = false
            )
        }
        _cameraTarget.value = lat to lng
        lastCraftsmenCenter = lat to lng
        _nearbyCraftsmen.value = repository.getCraftsmenAround(lat, lng)
    }

    fun searchPlaces(context: Context, query: String) {
        viewModelScope.launch {
            _isSearchingPlaces.value = true
            _placeSearchResults.value = MapRoutingService.searchPlaces(context, query)
            _isSearchingPlaces.value = false
        }
    }

    fun clearPlaceSearchResults() {
        _placeSearchResults.value = emptyList()
    }

    fun consumeCameraTarget() {
        _cameraTarget.value = null
    }

    fun startLiveGpsTracking(context: Context) {
        val svc = locationService ?: RealTimeLocationService(context.applicationContext)
        _trackingState.update { it.copy(hasLocationPermission = svc.hasLocationPermission()) }
        if (!svc.hasLocationPermission()) return
        if (gpsStreamJob?.isActive == true) return
        gpsStreamJob = viewModelScope.launch {
            svc.observeRealTimeLocation(intervalMillis = 8000L, minDistanceMeters = 15f).collect { fix ->
                _userGpsLocation.value = fix.lat to fix.lng
                _trackingState.update { it.copy(latestFix = fix, hasLocationPermission = true) }
            }
        }
    }

    fun fetchCurrentGpsLocation(context: Context) {
        val appContext = context.applicationContext
        val svc = locationService ?: RealTimeLocationService(appContext)
        _trackingState.update { it.copy(hasLocationPermission = svc.hasLocationPermission()) }
        startLiveGpsTracking(appContext)

        viewModelScope.launch {
            val fix = svc.getCurrentFix()
            if (fix != null) {
                val lat = fix.lat
                val lng = fix.lng
                _userGpsLocation.value = lat to lng
                _cameraTarget.value = lat to lng
                _trackingState.update { it.copy(latestFix = fix, hasLocationPermission = true, errorMessage = null) }
                lastCraftsmenCenter = lat to lng
                _nearbyCraftsmen.value = repository.getCraftsmenAround(lat, lng)
                _draft.update {
                    it.copy(
                        lat = lat,
                        lng = lng,
                        isGpsActive = true,
                        isResolvingAddress = true
                    )
                }
                geocodeJob?.cancel()
                geocodeJob = viewModelScope.launch {
                    val address = MapRoutingService.reverseGeocode(appContext, lat, lng)
                    _draft.update {
                        it.copy(
                            addressLabel = address,
                            isResolvingAddress = false
                        )
                    }
                }
                showMessage("تم تحديد موقعك الحقيقي عبر GPS")
            } else {
                showMessage("فعّل خدمة الموقع (GPS) على الجهاز أو حرّك الخريطة لتحديد مكانك")
            }
        }
    }

    fun updateCraftsmanSearchQuery(query: String) {
        _craftsmanSearchQuery.value = query
    }

    fun setSearchRadiusKm(radiusKm: Int) {
        _searchRadiusKm.value = radiusKm
    }

    fun setCraftsmanSort(sort: CraftsmanSortOption) {
        _craftsmanSort.value = sort
    }

    fun toggleOnlyCategoryOnMap(only: Boolean) {
        _onlySelectedCategoryOnMap.value = only
    }

    fun setOfferSort(sort: OfferSortOption) {
        _offerSort.value = sort
    }

    fun focusCraftsmanOnMap(pin: CraftsmanPin) {
        _cameraTarget.value = pin.lat to pin.lng
    }

    fun expandActiveSearchRadius(newRadiusKm: Int) {
        val req = activeClientRequest.value ?: return
        _searchRadiusKm.value = newRadiusKm
        viewModelScope.launch {
            repository.expandSearchRadius(req.id, newRadiusKm)
            showMessage("تم توسيع نطاق البحث إلى $newRadiusKm كم ودعوة حرفيين إضافيين")
        }
    }

    fun requestSpecificCraftsman(pin: CraftsmanPin) {
        _draft.update { current ->
            current.copy(
                selectedCategoryId = pin.specialtyId,
                proposedPrice = pin.basePriceDh,
                title = current.title.ifBlank { "طلب ${pin.specialtyName} مباشر - ${pin.name}" }
            )
        }
        submitServiceRequest(preferredCraftsmanId = pin.id)
    }

    fun submitServiceRequest(preferredCraftsmanId: String? = null) {
        val current = _draft.value
        val radiusKm = _searchRadiusKm.value
        viewModelScope.launch {
            val req = repository.createServiceRequest(
                categoryId = current.selectedCategoryId,
                title = current.title.ifBlank { "طلب ${HirafiCategories.byId(current.selectedCategoryId).nameAr}" },
                description = current.description,
                addressLabel = current.addressLabel,
                lat = current.lat,
                lng = current.lng,
                mapX = 0.5f,
                mapY = 0.5f,
                proposedPrice = current.proposedPrice,
                paymentMethod = current.paymentMethod,
                isUrgent = current.isUrgent,
                photoUri = current.photoUri,
                preferredCraftsmanId = preferredCraftsmanId,
                radiusKm = radiusKm
            )
            _activeClientRequestId.value = req.id
            _draft.update { it.copy(title = "", description = "", photoUri = null) }
            val msg = if (preferredCraftsmanId != null) {
                "تم إرسال طلب مباشر للحرفي المختار وللحرفيين القريبين!"
            } else {
                "تم إرسال طلبك! جاري استقبال عروض الحرفيين..."
            }
            showMessage(msg)
        }
    }

    fun raiseActiveRequestPrice(delta: Int) {
        val req = activeClientRequest.value ?: return
        val newPrice = (req.proposedPrice + delta).coerceAtLeast(50)
        viewModelScope.launch {
            repository.raiseRequestPrice(req.id, newPrice)
            showMessage("تم رفع السعر المقترح إلى $newPrice درهم")
        }
    }

    fun acceptOffer(offer: ServiceOffer) {
        viewModelScope.launch {
            repository.acceptOffer(offer)
            showMessage("تم قبول عرض ${offer.craftsmanName} بسعر ${offer.offeredPrice} درهم")
        }
    }

    fun declineOffer(offerId: String) {
        viewModelScope.launch {
            repository.declineOffer(offerId)
        }
    }

    fun counterOffer(offer: ServiceOffer, counterPrice: Int) {
        viewModelScope.launch {
            repository.counterOffer(offer, counterPrice.coerceAtLeast(40))
            showMessage("تم إرسال عرض مضاد ($counterPrice درهم) لـ ${offer.craftsmanName}")
        }
    }

    fun cancelActiveRequest() {
        val req = activeClientRequest.value ?: return
        viewModelScope.launch {
            repository.cancelRequest(req.id)
            _activeClientRequestId.value = null
            showMessage("تم إلغاء الطلب")
        }
    }

    fun advanceJobStatus(request: ServiceRequest, nextStatus: RequestStatus) {
        viewModelScope.launch {
            repository.advanceJobStatus(request.id, nextStatus)
            if (nextStatus == RequestStatus.COMPLETED) {
                if (userSession.value.role == UserRole.CLIENT) {
                    _pendingReviewRequest.value = request.copy(status = RequestStatus.COMPLETED)
                }
                if (_activeClientRequestId.value == request.id) {
                    _activeClientRequestId.value = null
                }
                showMessage("تم إتمام الخدمة بنجاح!")
            } else {
                showMessage("تم تحديث الحالة إلى: ${nextStatus.labelAr}")
            }
        }
    }

    fun dismissPendingReview() {
        _pendingReviewRequest.value = null
    }

    fun submitReview(request: ServiceRequest, rating: Int, tags: List<String>, comment: String) {
        viewModelScope.launch {
            repository.submitReview(
                requestId = request.id,
                craftsmanId = request.assignedCraftsmanId ?: "craftsman",
                rating = rating,
                tags = tags,
                comment = comment
            )
            _pendingReviewRequest.value = null
            showMessage("شكراً لتقييمك! يساعد هذا في تحسين جودة الحرفيين.")
        }
    }

    fun craftsmanAcceptAtClientPrice(requestId: String) {
        viewModelScope.launch {
            runCatching {
                repository.craftsmanDirectAcceptRequest(requestId)
            }.onSuccess {
                showMessage("تم قبول الطلب! يمكنك الآن تتبع موقع الزبون والانطلاق.")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر قبول الطلب")
            }
        }
    }

    fun craftsmanSubmitCounterOffer(requestId: String, offeredPrice: Int, etaMinutes: Int, note: String) {
        viewModelScope.launch {
            runCatching {
                repository.submitCraftsmanBid(requestId, offeredPrice, etaMinutes, note)
            }.onSuccess {
                showMessage("تم إرسال عرضك بسعر $offeredPrice درهم للزبون")
            }.onFailure { err ->
                showMessage(err.message ?: "تعذر إرسال العرض")
            }
        }
    }

    fun openChatForRequest(requestId: String) {
        _activeChatRequestId.value = requestId
    }

    fun sendChatMessage(requestId: String, text: String) {
        viewModelScope.launch {
            repository.sendChatMessage(requestId, text)
        }
    }

    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            repository.setRole(role)
            showMessage(if (role == UserRole.CRAFTSMAN) "تم التبديل إلى وضع الحرفي" else "تم التبديل إلى وضع الزبون")
        }
    }

    fun toggleCraftsmanOnline(isOnline: Boolean) {
        viewModelScope.launch {
            repository.setCraftsmanOnline(isOnline)
        }
    }

    fun setCity(city: String) {
        viewModelScope.launch {
            repository.setCity(city)
            val coord = MoroccanLocations.coordForCity(city)
            selectNeighborhood(city, coord.lat, coord.lng)
        }
    }

    fun setCraftsmanSpecialty(specialtyId: String) {
        viewModelScope.launch {
            val lockedNow = repository.setSpecialty(specialtyId)
            if (lockedNow) {
                showMessage("تم تثبيت مهنتك الوحيدة (${HirafiCategories.byId(specialtyId).nameAr}) بنجاح")
            } else {
                showMessage("تم تحديد مهنتك مسبقاً — يُسمح بمهنة واحدة فقط لكل حرفي")
            }
        }
    }

    fun updateProfile(fullName: String, phone: String, city: String) {
        viewModelScope.launch {
            repository.updateProfile(fullName, phone, city)
            val coord = MoroccanLocations.coordForCity(city)
            selectNeighborhood(city, coord.lat, coord.lng)
            showMessage("تم حفظ بيانات الملف الشخصي")
        }
    }

    fun signInSupabase(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.signInWithSupabase(email, password)
            _authUiState.value = result.fold(
                onSuccess = { AuthUiState(isLoading = false, message = it, isError = false) },
                onFailure = { AuthUiState(isLoading = false, message = it.message, isError = true) }
            )
        }
    }

    fun signUpSupabase(email: String, password: String, fullName: String, phone: String, city: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.signUpWithSupabase(email, password, fullName, phone, city)
            _authUiState.value = result.fold(
                onSuccess = { AuthUiState(isLoading = false, message = it, isError = false) },
                onFailure = { AuthUiState(isLoading = false, message = it.message, isError = true) }
            )
        }
    }

    fun signOutSupabase() {
        viewModelScope.launch {
            repository.signOutSupabase()
            _authUiState.value = AuthUiState(message = "تم تسجيل الخروج من حساب Supabase")
        }
    }

    fun syncSupabaseNow() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = repository.syncFromSupabase()
            _authUiState.value = result.fold(
                onSuccess = { count -> AuthUiState(message = "تمت المزامنة مع Supabase ($count طلبات)") },
                onFailure = { err -> AuthUiState(message = err.message, isError = true) }
            )
        }
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        trackingJob?.cancel()
        gpsStreamJob?.cancel()
        locationService?.stopForegroundJobTracking()
        super.onCleared()
    }

    companion object {
        fun factory(
            repository: HirafiRepository,
            locationService: RealTimeLocationService? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HirafiViewModel(repository, locationService) as T
                }
            }
    }
}
