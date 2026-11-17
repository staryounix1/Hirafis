package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CraftsmanPin
import com.example.data.model.HirafiCategories
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferSortOption
import com.example.data.model.PaymentMethod
import com.example.data.model.RequestStatus
import com.example.data.model.UserRole
import com.example.ui.components.ActiveJobSheet
import com.example.ui.components.CraftsmanDetailDialog
import com.example.ui.components.HirafiRealMap
import com.example.ui.components.IncomingOfferCard
import com.example.ui.components.LiveDispatchSonarOverlay
import com.example.ui.components.NearbyCraftsmenSearchSheet
import com.example.ui.components.PriceProposalBar
import com.example.ui.components.RatingDialog
import com.example.ui.components.RealtimeBiddingBottomSheet
import com.example.ui.components.SheetDragHandleBar
import com.example.ui.components.SmartTriageCarousel
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimeSoft
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.viewmodel.HirafiViewModel
import com.example.ui.viewmodel.RealtimeBiddingViewModel

@Composable
fun ClientHomeScreen(
    viewModel: HirafiViewModel,
    biddingViewModel: RealtimeBiddingViewModel,
    onOpenDrawer: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenSafety: () -> Unit
) {
    val context = LocalContext.current
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val filteredCraftsmen by viewModel.filteredNearbyCraftsmen.collectAsStateWithLifecycle()
    val craftsmanSearchQuery by viewModel.craftsmanSearchQuery.collectAsStateWithLifecycle()
    val searchRadiusKm by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
    val craftsmanSort by viewModel.craftsmanSort.collectAsStateWithLifecycle()
    val onlySelectedCategoryOnMap by viewModel.onlySelectedCategoryOnMap.collectAsStateWithLifecycle()
    val offerSort by viewModel.offerSort.collectAsStateWithLifecycle()
    val userGps by viewModel.userGpsLocation.collectAsStateWithLifecycle()
    val cameraTarget by viewModel.cameraTarget.collectAsStateWithLifecycle()
    val liveRoute by viewModel.liveRoute.collectAsStateWithLifecycle()
    val activeRequest by viewModel.activeClientRequest.collectAsStateWithLifecycle()
    val sortedOffers by viewModel.sortedActiveOffers.collectAsStateWithLifecycle()
    val nowMillis by viewModel.tickerMillis.collectAsStateWithLifecycle()
    val pendingReview by viewModel.pendingReviewRequest.collectAsStateWithLifecycle()
    val placeResults by viewModel.placeSearchResults.collectAsStateWithLifecycle()
    val isSearchingPlaces by viewModel.isSearchingPlaces.collectAsStateWithLifecycle()

    var showLocationPicker by remember { mutableStateOf(false) }
    var showCraftsmenSheet by remember { mutableStateOf(false) }
    var showRealtimeBiddingSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPinPreview by remember { mutableStateOf<CraftsmanPin?>(null) }
    var isDetailsExpanded by remember { mutableStateOf(false) }
    var isPassengerSheetExpanded by remember { mutableStateOf(true) }
    var isSearchingSheetExpanded by remember { mutableStateOf(true) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.fetchCurrentGpsLocation(context)
        } else {
            viewModel.showMessage("يرجى السماح بصلاحية الموقع أو تحريك الخريطة لتحديد مكانك")
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted && userGps == null) {
            viewModel.fetchCurrentGpsLocation(context)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.setDraftPhotoUri(uri?.toString())
    }

    val selectedCategory = HirafiCategories.byId(draft.selectedCategoryId)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxSheetBodyHeight = (maxHeight * 0.64f).coerceAtLeast(280.dp)
        // 1. Live inDrive-style real CARTO / OSM map
        HirafiRealMap(
            centerLat = activeRequest?.lat ?: draft.lat,
            centerLng = activeRequest?.lng ?: draft.lng,
            cameraTarget = cameraTarget,
            onCameraTargetConsumed = { viewModel.consumeCameraTarget() },
            selectedCategoryId = draft.selectedCategoryId,
            craftsmen = filteredCraftsmen,
            activeRequest = activeRequest,
            liveRoute = liveRoute,
            userGpsLocation = userGps,
            isCraftsmanMode = false,
            onMapCenterChanged = { lat, lng -> viewModel.onMapPinMoved(context, lat, lng) },
            onCraftsmanPinClick = { pin -> selectedPinPreview = pin },
            modifier = Modifier.fillMaxSize()
        )

        if (activeRequest?.status == RequestStatus.SEARCHING) {
            val req = activeRequest!!
            LiveDispatchSonarOverlay(
                createdAtMillis = req.createdAtMillis,
                nowMillis = nowMillis,
                categoryName = req.categoryName,
                radiusKm = searchRadiusKm,
                offersCount = sortedOffers.size
            )
        }

        // 2. Iconic inDrive Passenger Top Floating HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_drawer_button")
                    ) {
                        Icon(
                            Icons.Rounded.Menu,
                            contentDescription = "القائمة",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Pickup Location Pill with glowing Emerald inDrive dot
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .clickable { showLocationPicker = true }
                        .testTag("location_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(VerifiedEmeraldSoft)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(VerifiedEmerald)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "موقع الخدمة",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    activeRequest != null -> activeRequest!!.addressLabel
                                    draft.isResolvingAddress -> "جاري تحديد العنوان..."
                                    else -> draft.addressLabel
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // GPS Locate Pill on Map
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.fetchCurrentGpsLocation(context)
                            } else {
                                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("gps_locate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "تحديد موقعي عبر GPS",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Bottom inDrive Interactive Sheets
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            when {
                activeRequest == null -> {
                    // STATE A: Authentic inDrive Passenger Service Sheet (Draggable & Clickable Up/Down)
                    Card(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 14.dp)
                        ) {
                            SheetDragHandleBar(
                                isExpanded = isPassengerSheetExpanded,
                                onToggle = { isPassengerSheetExpanded = !isPassengerSheetExpanded },
                                onExpandChange = { isPassengerSheetExpanded = it }
                            )

                            if (!isPassengerSheetExpanded) {
                                // Compact Peek Dock when scrolled/collapsed down
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = ObsidianBlack,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { isPassengerSheetExpanded = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = selectedCategory.icon,
                                                contentDescription = null,
                                                tint = ElectricLime,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = selectedCategory.nameAr,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.submitServiceRequest() },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricLime,
                                            contentColor = ObsidianBlack
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("collapsed_find_craftsman_button")
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "ابحث بـ ${draft.proposedPrice} د.م",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isPassengerSheetExpanded,
                                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = maxSheetBodyHeight)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ما الخدمة التي تحتاجها؟",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        TextButton(onClick = { isDetailsExpanded = !isDetailsExpanded }) {
                                            Text(
                                                if (isDetailsExpanded) "إخفاء التفاصيل" else "صورة وتفاصيل",
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Icon(
                                                imageVector = if (isDetailsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                            // inDrive Service Tiles Strip
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(HirafiCategories.all) { cat ->
                                    val selected = cat.id == draft.selectedCategoryId
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) ObsidianBlack else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) ElectricLime else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { viewModel.selectCategory(cat.id) }
                                            .testTag("category_${cat.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = cat.nameAr,
                                                tint = if (selected) ElectricLime else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = cat.nameAr,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Clean Nearby Craftsmen Pill
                            val radiusText = if (searchRadiusKm >= 20) "الكل" else "$searchRadiusKm كم"
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ElectricLimeSoft,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ElectricLime, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showCraftsmenSheet = true }
                                    .testTag("browse_nearby_craftsmen_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PersonSearch,
                                            contentDescription = null,
                                            tint = ObsidianBlack,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "متاح ${filteredCraftsmen.size} حرفيين ($radiusText)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ObsidianBlack,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "ابحث بالاسم أو صفِّ حسب المسافة والتقييم",
                                                fontSize = 11.sp,
                                                color = ObsidianBlack.copy(alpha = 0.72f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = ObsidianBlack,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = null,
                                                tint = ElectricLime,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "تصفح",
                                                color = ElectricLime,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            SmartTriageCarousel(
                                selectedCategoryId = draft.selectedCategoryId,
                                currentTitle = draft.title,
                                onSelectScenario = { scenario ->
                                    viewModel.updateDraftTitle(scenario.titleAr)
                                    viewModel.updateDraftDescription(scenario.descriptionAr)
                                    viewModel.setDraftPrice(scenario.recommendedPriceDh)
                                    viewModel.toggleUrgent(scenario.isUrgentSuggested)
                                }
                            )

                            OutlinedTextField(
                                value = draft.title,
                                onValueChange = { viewModel.updateDraftTitle(it) },
                                placeholder = { Text("صف المشكلة بإيجاز (مثلاً: إصلاح تسرب صنبور)") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("request_title_input")
                            )

                            AnimatedVisibility(visible = isDetailsExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = draft.description,
                                        onValueChange = { viewModel.updateDraftDescription(it) },
                                        placeholder = { Text("تفاصيل إضافية للحرفي (اختياري)...") },
                                        maxLines = 2,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (draft.photoUri == null) "إرفاق صورة" else "تغيير الصورة")
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.Bolt,
                                                contentDescription = null,
                                                tint = UrgentCoral,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text("طلب عاجل", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Switch(
                                                checked = draft.isUrgent,
                                                onCheckedChange = { viewModel.toggleUrgent(it) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBlack, checkedTrackColor = ElectricLime)
                                            )
                                        }
                                    }

                                    if (draft.photoUri != null) {
                                        AsyncImage(
                                            model = draft.photoUri,
                                            contentDescription = "صورة العطل",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PaymentMethod.entries.forEach { method ->
                                            FilterChip(
                                                selected = draft.paymentMethod == method,
                                                onClick = { viewModel.selectPaymentMethod(method) },
                                                label = { Text(method.labelAr, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ObsidianBlack,
                                                    selectedLabelColor = ElectricLime
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            PriceProposalBar(
                                priceDh = draft.proposedPrice,
                                recommendedMin = selectedCategory.minRecommendedDh,
                                recommendedMax = selectedCategory.maxRecommendedDh,
                                onAdjust = { viewModel.adjustDraftPrice(it) },
                                onSetExact = { viewModel.setDraftPrice(it) }
                            )

                                    Button(
                                        onClick = { viewModel.submitServiceRequest() },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricLime,
                                            contentColor = ObsidianBlack
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("find_craftsman_button")
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "البحث عن حرفي بـ ${draft.proposedPrice} د.م",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                activeRequest?.status == RequestStatus.SEARCHING -> {
                    val req = activeRequest!!
                    val bestPriceId = sortedOffers.minByOrNull { it.offeredPrice }?.id
                    val fastestId = sortedOffers.minByOrNull { it.etaMinutes }?.id
                    val topRatedId = sortedOffers.maxByOrNull { it.craftsmanRating }?.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (sortedOffers.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 310.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(sortedOffers, key = { it.id }) { offer ->
                                    val badge = when (offerSort) {
                                        OfferSortOption.BEST_PRICE -> if (offer.id == bestPriceId) "الأقل سعراً" else null
                                        OfferSortOption.FASTEST_ETA -> if (offer.id == fastestId) "الأسرع وصولاً" else null
                                        OfferSortOption.TOP_RATED -> if (offer.id == topRatedId) "الأعلى تقييماً" else null
                                    }
                                    IncomingOfferCard(
                                        offer = offer,
                                        clientProposedPrice = req.proposedPrice,
                                        nowMillis = nowMillis,
                                        onAccept = { viewModel.acceptOffer(offer) },
                                        onDecline = { viewModel.declineOffer(offer.id) },
                                        onCounterOffer = { price -> viewModel.counterOffer(offer, price) },
                                        highlightBadge = badge
                                    )
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                SheetDragHandleBar(
                                    isExpanded = isSearchingSheetExpanded,
                                    onToggle = { isSearchingSheetExpanded = !isSearchingSheetExpanded },
                                    onExpandChange = { isSearchingSheetExpanded = it }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (sortedOffers.isEmpty())
                                                "جاري إرسال طلبك لحرفيي ${req.categoryName}..."
                                            else
                                                "وصلك ${sortedOffers.size} عروض — اختر الأنسب لك",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        val radiusLabel = if (searchRadiusKm >= 20) "كامل المدينة" else "$searchRadiusKm كم"
                                        Text(
                                            text = "${req.categoryName} • السعر: ${req.proposedPrice} د.م • النطاق: $radiusLabel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.cancelActiveRequest() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("cancel_searching_button")
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "إلغاء الطلب", tint = UrgentCoral)
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isSearchingSheetExpanded,
                                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                    exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                ) {
                                    Column {

                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item {
                                        FilterChip(
                                            selected = true,
                                            onClick = { showRealtimeBiddingSheet = true },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.Gavel,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    "غرفة المزايدة",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ObsidianBlack,
                                                selectedLabelColor = ElectricLime,
                                                selectedLeadingIconColor = ElectricLime
                                            ),
                                            modifier = Modifier.testTag("open_realtime_bidding_sheet")
                                        )
                                    }
                                    if (sortedOffers.size > 1) {
                                        items(OfferSortOption.entries) { sortOpt ->
                                            FilterChip(
                                                selected = offerSort == sortOpt,
                                                onClick = { viewModel.setOfferSort(sortOpt) },
                                                label = { Text(sortOpt.labelAr, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ObsidianBlack,
                                                    selectedLabelColor = ElectricLime
                                                ),
                                                modifier = Modifier.testTag("offer_sort_${sortOpt.name.lowercase()}")
                                            )
                                        }
                                    }
                                    if (searchRadiusKm < 20) {
                                        val nextRadius = if (searchRadiusKm < 10) 10 else 20
                                        val nextLabel = if (nextRadius >= 20) "توسيع لكل المدينة" else "توسيع النطاق (+$nextRadius كم)"
                                        item {
                                            FilterChip(
                                                selected = false,
                                                onClick = { viewModel.expandActiveSearchRadius(nextRadius) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Rounded.NearMe,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp),
                                                        tint = VerifiedEmerald
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        nextLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = VerifiedEmerald
                                                    )
                                                },
                                                modifier = Modifier.testTag("expand_search_radius_button")
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.raiseActiveRequestPrice(-10) },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("-10 د.م", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.raiseActiveRequestPrice(10) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(48.dp)
                                            .testTag("raise_fare_plus_10")
                                    ) {
                                        Text("رفع السعر +10", fontWeight = FontWeight.ExtraBold)
                                    }
                                    Button(
                                        onClick = { viewModel.raiseActiveRequestPrice(20) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = ElectricLime),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("+20 د.م", fontWeight = FontWeight.Bold)
                                    }
                                }
                                    }
                                }
                            }
                        }
                    }
                }

                activeRequest != null -> {
                    val req = activeRequest!!
                    ActiveJobSheet(
                        request = req,
                        liveRoute = liveRoute,
                        onCallClick = { phone ->
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(dialIntent)
                        },
                        onChatClick = { onOpenChat(req.id) },
                        onSafetyClick = onOpenSafety,
                        onAdvanceStatus = { nextStatus -> viewModel.advanceJobStatus(req, nextStatus) },
                        onCancel = { viewModel.cancelActiveRequest() }
                    )
                }
            }
        }
    }

    if (showLocationPicker) {
        AlertDialog(
            onDismissRequest = {
                showLocationPicker = false
                viewModel.clearPlaceSearchResults()
            },
            title = { Text("ابحث عن عنوان أو اختر حياً", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن شارع أو حي...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.searchPlaces(context, searchQuery) }) {
                            Icon(Icons.Rounded.Search, contentDescription = "بحث")
                        }
                    }

                    if (isSearchingPlaces) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                    }

                    if (placeResults.isNotEmpty()) {
                        Text("نتائج البحث:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        placeResults.forEach { res ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectNeighborhood(res.displayName, res.lat, res.lng)
                                        viewModel.clearPlaceSearchResults()
                                        showLocationPicker = false
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = ObsidianBlack)
                                    Spacer(Modifier.width(8.dp))
                                    Text(res.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    Text("أحياء مشهورة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoroccanLocations.neighborhoods.forEach { n ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectNeighborhood(n.nameAr, n.lat, n.lng)
                                    showLocationPicker = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(11.dp)
                            ) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = VerifiedEmerald)
                                Spacer(Modifier.width(8.dp))
                                Text(n.nameAr, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationPicker = false
                        viewModel.clearPlaceSearchResults()
                    }
                ) { Text("إغلاق") }
            }
        )
    }

    if (showCraftsmenSheet) {
        NearbyCraftsmenSearchSheet(
            craftsmen = filteredCraftsmen,
            selectedCategoryId = draft.selectedCategoryId,
            searchQuery = craftsmanSearchQuery,
            searchRadiusKm = searchRadiusKm,
            selectedSort = craftsmanSort,
            onlySelectedCategory = onlySelectedCategoryOnMap,
            onSearchQueryChange = { viewModel.updateCraftsmanSearchQuery(it) },
            onSelectRadiusKm = { viewModel.setSearchRadiusKm(it) },
            onSelectSort = { viewModel.setCraftsmanSort(it) },
            onToggleOnlyCategory = { viewModel.toggleOnlyCategoryOnMap(it) },
            onPreviewCraftsman = { pin ->
                showCraftsmenSheet = false
                viewModel.focusCraftsmanOnMap(pin)
                selectedPinPreview = pin
            },
            onDirectRequestCraftsman = { pin ->
                showCraftsmenSheet = false
                viewModel.requestSpecificCraftsman(pin)
            },
            onDismiss = { showCraftsmenSheet = false }
        )
    }

    selectedPinPreview?.let { pin ->
        CraftsmanDetailDialog(
            pin = pin,
            onDirectRequest = { target ->
                selectedPinPreview = null
                viewModel.requestSpecificCraftsman(target)
            },
            onSelectSpecialtyOnly = { target ->
                viewModel.selectCategory(target.specialtyId)
                viewModel.setDraftPrice(target.basePriceDh)
                selectedPinPreview = null
            },
            onDismiss = { selectedPinPreview = null }
        )
    }

    val biddingRequest = activeRequest
    if (showRealtimeBiddingSheet && biddingRequest != null && biddingRequest.status == RequestStatus.SEARCHING) {
        RealtimeBiddingBottomSheet(
            viewModel = biddingViewModel,
            request = biddingRequest,
            roleOverride = UserRole.CLIENT,
            onDismiss = { showRealtimeBiddingSheet = false },
            onCompletedOrAccepted = { showRealtimeBiddingSheet = false }
        )
    }

    pendingReview?.let { completedReq ->
        RatingDialog(
            request = completedReq,
            onDismiss = { viewModel.dismissPendingReview() },
            onSubmit = { stars, tags, comment ->
                viewModel.submitReview(completedReq, stars, tags, comment)
            }
        )
    }
}
