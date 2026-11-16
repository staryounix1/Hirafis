package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.data.model.CraftsmanPin
import com.example.data.model.LiveRouteState
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.InfoCobalt
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.util.concurrent.TimeUnit

private enum class HirafiTileLayer(val labelAr: String) {
    ESRI_STREETS("شوارع"),
    OSM_STANDARD("OSM"),
    CARTO_VOYAGER("حديث"),
    CARTO_DARK("ليلي");

    fun next(): HirafiTileLayer {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

private fun resolvedCartoApiKey(): String {
    val raw = BuildConfig.CARTO_API_KEY.trim()
    return if (raw.isNotBlank() && !raw.contains("YOUR_CARTO_API_KEY")) raw else ""
}

private fun buildTileSource(layer: HirafiTileLayer, cartoApiKey: String): ITileSource {
    val cartoKeyParam = if (cartoApiKey.isNotBlank()) "?key=$cartoApiKey" else ""
    return when (layer) {
        HirafiTileLayer.ESRI_STREETS -> object : OnlineTileSourceBase(
            "EsriWorldStreetMap",
            0,
            19,
            256,
            ".jpg",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl +
                    "${MapTileIndex.getZoom(pMapTileIndex)}/" +
                    "${MapTileIndex.getY(pMapTileIndex)}/" +
                    "${MapTileIndex.getX(pMapTileIndex)}"
            }
        }

        HirafiTileLayer.OSM_STANDARD -> TileSourceFactory.MAPNIK

        HirafiTileLayer.CARTO_VOYAGER -> object : OnlineTileSourceBase(
            "CartoVoyager",
            0,
            19,
            256,
            ".png",
            arrayOf(
                "https://basemaps.cartocdn.com/rastertiles/voyager/",
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl +
                    "${MapTileIndex.getZoom(pMapTileIndex)}/" +
                    "${MapTileIndex.getX(pMapTileIndex)}/" +
                    "${MapTileIndex.getY(pMapTileIndex)}.png$cartoKeyParam"
            }
        }

        HirafiTileLayer.CARTO_DARK -> object : OnlineTileSourceBase(
            "CartoDarkMatter",
            0,
            19,
            256,
            ".png",
            arrayOf(
                "https://basemaps.cartocdn.com/dark_all/",
                "https://a.basemaps.cartocdn.com/dark_all/",
                "https://b.basemaps.cartocdn.com/dark_all/"
            )
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                return baseUrl +
                    "${MapTileIndex.getZoom(pMapTileIndex)}/" +
                    "${MapTileIndex.getX(pMapTileIndex)}/" +
                    "${MapTileIndex.getY(pMapTileIndex)}.png$cartoKeyParam"
            }
        }
    }
}

/**
 * Custom OkHttp-backed osmdroid tile loader with disk cache and multi-CDN failover.
 * Bypasses osmdroid's legacy HttpURLConnection + ConnectivityManager + SqlTileWriter issues on API 34-36.
 */
private class OkHttpTileModuleProvider(
    private val context: Context,
    private var tileSource: ITileSource?,
    private val onTileLoaded: () -> Unit,
    private val onTileError: (String) -> Unit
) : MapTileModuleProviderBase(4, 48) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val client: OkHttpClient by lazy {
        val cacheDir = File(context.cacheDir, "hirafi_tile_http_cache").apply { mkdirs() }
        OkHttpClient.Builder()
            .cache(Cache(cacheDir, 32L * 1024L * 1024L))
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun getUsesDataConnection(): Boolean = true
    override fun getName(): String = "HirafiOkHttpTiles"
    override fun getThreadGroupName(): String = "hirafi_okhttp_tiles"
    override fun getMinimumZoomLevel(): Int = tileSource?.minimumZoomLevel ?: 0
    override fun getMaximumZoomLevel(): Int = tileSource?.maximumZoomLevel ?: 19

    override fun setTileSource(pTileSource: ITileSource?) {
        tileSource = pTileSource
    }

    override fun getTileLoader(): TileLoader = OkHttpTileLoader()

    private inner class OkHttpTileLoader : TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            val source = tileSource ?: return null
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)

            val candidateUrls = buildList {
                (source as? OnlineTileSourceBase)?.getTileURLString(pMapTileIndex)?.let { add(it) }
                // Reliable fallbacks if primary CDN fails or rejects token
                add("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/$z/$y/$x")
                add("https://tile.openstreetmap.org/$z/$x/$y.png")
                add("https://basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png")
            }.distinct()

            var lastErr = "تعذر تحميل بلاطات الخريطة"
            for (url in candidateUrls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "HirafiAndroid/1.0 (+https://hirafi.app)")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bmp != null) {
                                    mainHandler.post { onTileLoaded() }
                                    return BitmapDrawable(context.resources, bmp)
                                }
                            }
                        } else {
                            lastErr = "HTTP ${response.code}"
                        }
                    }
                } catch (e: Exception) {
                    lastErr = e.javaClass.simpleName
                }
            }
            mainHandler.post { onTileError(lastErr) }
            return null
        }
    }
}

@Composable
fun HirafiRealMap(
    centerLat: Double,
    centerLng: Double,
    cameraTarget: Pair<Double, Double>?,
    onCameraTargetConsumed: () -> Unit,
    selectedCategoryId: String,
    craftsmen: List<CraftsmanPin>,
    clientRequestsOnMap: List<ServiceRequest> = emptyList(),
    activeRequest: ServiceRequest?,
    liveRoute: LiveRouteState,
    userGpsLocation: Pair<Double, Double>?,
    isCraftsmanMode: Boolean = false,
    onMapCenterChanged: (Double, Double) -> Unit,
    onCraftsmanPinClick: (CraftsmanPin) -> Unit = {},
    onClientRequestPinClick: (ServiceRequest) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    val cartoApiKey = remember { resolvedCartoApiKey() }

    var selectedLayer by remember(isDark, cartoApiKey) {
        mutableStateOf(
            when {
                isDark -> HirafiTileLayer.CARTO_DARK
                cartoApiKey.isNotBlank() -> HirafiTileLayer.CARTO_VOYAGER
                else -> HirafiTileLayer.ESRI_STREETS
            }
        )
    }

    var tilesLoadedCount by remember { mutableIntStateOf(0) }
    var tileErrorCount by remember { mutableIntStateOf(0) }
    var lastTileError by remember { mutableStateOf<String?>(null) }

    val isSearching = activeRequest?.status == RequestStatus.SEARCHING
    val hasAssigned = activeRequest != null &&
        activeRequest.status != RequestStatus.SEARCHING &&
        activeRequest.status != RequestStatus.CANCELLED &&
        activeRequest.status != RequestStatus.COMPLETED

    val showCenterDragPin = !isCraftsmanMode && activeRequest == null

    var ignoreScrollUntilMs by remember { mutableLongStateOf(0L) }
    var appliedLayer by remember { mutableStateOf(selectedLayer) }

    val currentShowCenterDragPin by rememberUpdatedState(showCenterDragPin)
    val currentOnMapCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val currentOnCraftsmanPinClick by rememberUpdatedState(onCraftsmanPinClick)
    val currentOnClientRequestPinClick by rememberUpdatedState(onClientRequestPinClick)

    val routeFolder = remember { FolderOverlay() }
    val markersFolder = remember { FolderOverlay() }
    val gpsFolder = remember { FolderOverlay() }

    val mapView = remember {
        val initialSource = buildTileSource(selectedLayer, cartoApiKey)
        val okHttpModule = OkHttpTileModuleProvider(
            context = context.applicationContext,
            tileSource = initialSource,
            onTileLoaded = {
                tilesLoadedCount += 1
                lastTileError = null
            },
            onTileError = { err ->
                tileErrorCount += 1
                lastTileError = err
            }
        )
        val tileProvider = MapTileProviderArray(
            initialSource,
            SimpleRegisterReceiver(context.applicationContext),
            arrayOf(okHttpModule)
        )

        MapView(context, tileProvider).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Crucial: force LTR so Arabic RTL locale does not mirror tiles/panning
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setUseDataConnection(true)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            isHorizontalMapRepetitionEnabled = true
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            overlayManager.tilesOverlay.loadingBackgroundColor = android.graphics.Color.parseColor(
                if (isDark) "#161B22" else "#EAEFF4"
            )
            overlayManager.tilesOverlay.loadingLineColor = android.graphics.Color.parseColor(
                if (isDark) "#26303D" else "#D5DEE7"
            )
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(centerLat, centerLng))
            post {
                ignoreScrollUntilMs = System.currentTimeMillis() + 700L
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(centerLat, centerLng))
                invalidate()
            }

            overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        if (p != null && currentShowCenterDragPin) {
                            ignoreScrollUntilMs = System.currentTimeMillis() + 650L
                            controller.animateTo(p)
                            currentOnMapCenterChanged(p.latitude, p.longitude)
                            return true
                        }
                        return false
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                })
            )
            overlays.add(routeFolder)
            overlays.add(markersFolder)
            overlays.add(gpsFolder)

            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    if (currentShowCenterDragPin && System.currentTimeMillis() > ignoreScrollUntilMs) {
                        val center = mapCenter
                        currentOnMapCenterChanged(center.latitude, center.longitude)
                    }
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean = false
            })
        }
    }

    // Only swap tile source when user actually toggles layer (skip redundant reset on mount)
    LaunchedEffect(selectedLayer) {
        if (appliedLayer != selectedLayer) {
            appliedLayer = selectedLayer
            mapView.setTileSource(buildTileSource(selectedLayer, cartoApiKey))
            val darkTiles = selectedLayer == HirafiTileLayer.CARTO_DARK
            mapView.overlayManager.tilesOverlay.loadingBackgroundColor = android.graphics.Color.parseColor(
                if (darkTiles) "#161B22" else "#EAEFF4"
            )
            mapView.overlayManager.tilesOverlay.loadingLineColor = android.graphics.Color.parseColor(
                if (darkTiles) "#26303D" else "#D5DEE7"
            )
            mapView.invalidate()
        }
    }

    // Smoothly animate camera when requested (GPS button, city change, search result)
    LaunchedEffect(cameraTarget) {
        if (cameraTarget != null) {
            ignoreScrollUntilMs = System.currentTimeMillis() + 950L
            mapView.controller.animateTo(GeoPoint(cameraTarget.first, cameraTarget.second), 15.8, 700L)
            onCameraTargetConsumed()
        }
    }

    // Fit bounding box when live route starts or request changes status
    LaunchedEffect(activeRequest?.id, activeRequest?.status) {
        if (hasAssigned && activeRequest != null && liveRoute.craftsmanLat != null && liveRoute.craftsmanLng != null) {
            val pts = buildList {
                add(GeoPoint(activeRequest.lat, activeRequest.lng))
                add(GeoPoint(liveRoute.craftsmanLat, liveRoute.craftsmanLng))
                liveRoute.routePoints.forEach { add(GeoPoint(it.first, it.second)) }
            }
            if (pts.size >= 2) {
                runCatching {
                    val box = BoundingBox.fromGeoPoints(pts).increaseByScale(1.35f)
                    ignoreScrollUntilMs = System.currentTimeMillis() + 950L
                    mapView.zoomToBoundingBox(box, true, 120)
                }
            }
        }
    }

    // Update GPS Dot folder only when user GPS location changes
    LaunchedEffect(userGpsLocation) {
        gpsFolder.items.clear()
        if (userGpsLocation != null) {
            val gpsMarker = Marker(mapView).apply {
                position = GeoPoint(userGpsLocation.first, userGpsLocation.second)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createGpsDotDrawable(context)
                setInfoWindow(null)
            }
            gpsFolder.add(gpsMarker)
        }
        mapView.invalidate()
    }

    // Update Route Polyline folder only when liveRoute changes
    LaunchedEffect(hasAssigned, liveRoute.routePoints) {
        routeFolder.items.clear()
        if (hasAssigned && liveRoute.routePoints.size >= 2) {
            val geoPoints = liveRoute.routePoints.map { GeoPoint(it.first, it.second) }
            val borderLine = Polyline(mapView).apply {
                setPoints(geoPoints)
                outlinePaint.color = ObsidianBlack.toArgb()
                outlinePaint.strokeWidth = 18f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
            }
            val limeLine = Polyline(mapView).apply {
                setPoints(geoPoints)
                outlinePaint.color = ElectricLime.toArgb()
                outlinePaint.strokeWidth = 11f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
            }
            routeFolder.add(borderLine)
            routeFolder.add(limeLine)
        }
        mapView.invalidate()
    }

    // Update Markers folder only when pins, activeRequest, or live craftsman position change
    LaunchedEffect(
        isCraftsmanMode,
        clientRequestsOnMap,
        activeRequest?.id,
        activeRequest?.status,
        hasAssigned,
        isSearching,
        liveRoute.craftsmanLat,
        liveRoute.craftsmanLng,
        liveRoute.etaMinutes,
        craftsmen,
        selectedCategoryId
    ) {
        markersFolder.items.clear()

        if (isCraftsmanMode) {
            clientRequestsOnMap.forEach { req ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(req.lat, req.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createBadgeDrawable(
                        context = context,
                        primaryText = "${req.proposedPrice} د.م",
                        secondaryText = req.categoryName,
                        highlighted = true
                    )
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ ->
                        currentOnClientRequestPinClick(req)
                        true
                    }
                }
                markersFolder.add(marker)
            }
            if (hasAssigned && liveRoute.craftsmanLat != null && liveRoute.craftsmanLng != null) {
                val liveCraftsmanPin = Marker(mapView).apply {
                    position = GeoPoint(liveRoute.craftsmanLat, liveRoute.craftsmanLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createBadgeDrawable(
                        context = context,
                        primaryText = "موقعك",
                        secondaryText = if (liveRoute.etaMinutes > 0) "${liveRoute.etaMinutes} د" else "وصلت",
                        highlighted = true
                    )
                    setInfoWindow(null)
                }
                markersFolder.add(liveCraftsmanPin)
            }
        } else {
            if (activeRequest != null) {
                val clientPin = Marker(mapView).apply {
                    position = GeoPoint(activeRequest.lat, activeRequest.lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createClientPinDrawable(context, if (isSearching) "جاري البحث" else "موقعك")
                    setInfoWindow(null)
                }
                markersFolder.add(clientPin)
            }

            if (hasAssigned && liveRoute.craftsmanLat != null && liveRoute.craftsmanLng != null) {
                val movingMarker = Marker(mapView).apply {
                    position = GeoPoint(liveRoute.craftsmanLat, liveRoute.craftsmanLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createBadgeDrawable(
                        context = context,
                        primaryText = activeRequest?.assignedCraftsmanName?.split(" ")?.firstOrNull() ?: "الحرفي",
                        secondaryText = if (liveRoute.etaMinutes > 0) "${liveRoute.etaMinutes} د" else "وصل",
                        highlighted = true
                    )
                    setInfoWindow(null)
                }
                markersFolder.add(movingMarker)
            } else if (!hasAssigned) {
                craftsmen.forEach { pin ->
                    val highlighted = pin.specialtyId == selectedCategoryId
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(pin.lat, pin.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = createBadgeDrawable(
                            context = context,
                            primaryText = "${pin.basePriceDh} د.م",
                            secondaryText = "★${pin.rating}",
                            highlighted = highlighted
                        )
                        setInfoWindow(null)
                        setOnMarkerClickListener { _, _ ->
                            currentOnCraftsmanPinClick(pin)
                            true
                        }
                    }
                    markersFolder.add(marker)
                }
            }
        }

        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onResume()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("hirafi_real_map")) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Pulsing Radar Overlay during active search
        if (isSearching) {
            SearchRadarOverlay()
        }

        // Only shown if all tile servers fail so user can see why and retry
        if (tilesLoadedCount == 0 && tileErrorCount >= 4 && lastTileError != null) {
            Surface(
                color = UrgentCoral.copy(alpha = 0.92f),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .clickable {
                        tileErrorCount = 0
                        lastTileError = null
                        selectedLayer = selectedLayer.next()
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "إعادة تحميل الخريطة",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "تعذر تحميل الخريطة ($lastTileError) • اضغط للتبديل",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // inDrive Center Crosshair Pin when choosing pickup/service location
        if (showCenterDragPin) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-26).dp)
            ) {
                Surface(
                    color = ObsidianBlack,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = "موقع الخدمة",
                        color = ElectricLime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ElectricLime)
                        .border(3.dp, ObsidianBlack, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "موقع الخدمة",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(ObsidianBlack.copy(alpha = 0.45f))
                )
            }
        }

        // Map Zoom (+ / -) and Layer Switcher Controls
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp, bottom = 90.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { mapView.controller.zoomIn() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_in_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "تكبير الخريطة")
                    }
                    HorizontalDivider(modifier = Modifier.width(32.dp))
                    IconButton(
                        onClick = { mapView.controller.zoomOut() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "تصغير الخريطة")
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianBlack,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .clickable { selectedLayer = selectedLayer.next() }
                    .testTag("map_layer_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = "تبديل نمط الخريطة",
                        tint = ElectricLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = selectedLayer.labelAr,
                        color = ElectricLime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchRadarOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxR = size.minDimension * 0.28f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.44f)
        drawCircle(
            color = ElectricLime.copy(alpha = radarAlpha),
            radius = maxR * radarScale,
            center = center
        )
        drawCircle(
            color = ElectricLime.copy(alpha = 0.35f),
            radius = maxR * 0.45f,
            center = center,
            style = Stroke(width = 4f)
        )
    }
}

private fun createGpsDotDrawable(context: Context): BitmapDrawable {
    val size = 56
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = InfoCobalt.copy(alpha = 0.25f).toArgb()
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = InfoCobalt.toArgb()
    }
    canvas.drawCircle(size / 2f, size / 2f, 26f, haloPaint)
    canvas.drawCircle(size / 2f, size / 2f, 15f, borderPaint)
    canvas.drawCircle(size / 2f, size / 2f, 11f, dotPaint)
    return BitmapDrawable(context.resources, bmp)
}

private fun createClientPinDrawable(context: Context, label: String): BitmapDrawable {
    val width = 180
    val height = 100
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ObsidianBlack.toArgb() }
    val limePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ElectricLime.toArgb() }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ElectricLime.toArgb()
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawRoundRect(RectF(10f, 6f, width - 10f, 52f), 20f, 20f, bgPaint)
    canvas.drawText(label, width / 2f, 37f, textPaint)
    canvas.drawCircle(width / 2f, 76f, 18f, bgPaint)
    canvas.drawCircle(width / 2f, 76f, 12f, limePaint)
    return BitmapDrawable(context.resources, bmp)
}

private fun createBadgeDrawable(
    context: Context,
    primaryText: String,
    secondaryText: String,
    highlighted: Boolean
): BitmapDrawable {
    val width = 190
    val height = 84
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (highlighted) ObsidianBlack.toArgb() else android.graphics.Color.WHITE
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = if (highlighted) ElectricLime.toArgb() else VerifiedEmerald.toArgb()
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (highlighted) ElectricLime.toArgb() else ObsidianBlack.toArgb()
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (highlighted) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
        textSize = 19f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val rect = RectF(6f, 6f, width - 6f, height - 18f)
    canvas.drawRoundRect(rect, 24f, 24f, bgPaint)
    canvas.drawRoundRect(rect, 24f, 24f, strokePaint)
    canvas.drawText(primaryText, width / 2f, 34f, titlePaint)
    canvas.drawText(secondaryText, width / 2f, 58f, subPaint)
    canvas.drawCircle(width / 2f, height - 10f, 8f, strokePaint)
    return BitmapDrawable(context.resources, bmp)
}
