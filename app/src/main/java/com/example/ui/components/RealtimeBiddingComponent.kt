package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimeSoft
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.BiddingUiState
import com.example.ui.viewmodel.CraftsmanBidDraft
import com.example.ui.viewmodel.RealtimeBiddingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeBiddingBottomSheet(
    viewModel: RealtimeBiddingViewModel,
    request: ServiceRequest,
    roleOverride: UserRole? = null,
    onDismiss: () -> Unit,
    onCompletedOrAccepted: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        RealtimeBiddingComponent(
            viewModel = viewModel,
            request = request,
            roleOverride = roleOverride,
            onCompletedOrAccepted = onCompletedOrAccepted,
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun RealtimeBiddingComponent(
    viewModel: RealtimeBiddingViewModel,
    request: ServiceRequest,
    modifier: Modifier = Modifier,
    roleOverride: UserRole? = null,
    onCompletedOrAccepted: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(request.id) {
        viewModel.bindRequest(request)
    }

    DisposableEffect(request.id) {
        onDispose {
            viewModel.unbindRequest()
        }
    }

    LaunchedEffect(uiState) {
        val active = uiState as? BiddingUiState.Active ?: return@LaunchedEffect
        if (active.request.status != RequestStatus.SEARCHING) {
            onCompletedOrAccepted()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "live_bidding_pulse")
    val liveDotColor by pulseTransition.animateColor(
        initialValue = VerifiedEmerald,
        targetValue = ElectricLime,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_bidding_component"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = ObsidianBlack,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Gavel,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(liveDotColor)
                        )
                        Text(
                            text = "غرفة المزايدة المباشرة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${request.categoryName} • السعر الابتدائي ${request.proposedPrice} د.م",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val syncing = (uiState as? BiddingUiState.Active)?.isSyncingRealtime == true
                IconButton(
                    onClick = { viewModel.refreshOffersNow() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_bidding_button")
                ) {
                    if (syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "تحديث العروض")
                    }
                }
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_bidding_sheet_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                    }
                }
            }
        }

        // Request Brief Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${request.clientName} • ${request.addressLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = ObsidianBlack,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(uiState as? BiddingUiState.Active)?.request?.proposedPrice ?: request.proposedPrice} د.م",
                        color = ElectricLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        when (val state = uiState) {
            is BiddingUiState.Idle, is BiddingUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    CircularProgressIndicator(color = ElectricLime)
                }
            }

            is BiddingUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is BiddingUiState.Active -> {
                val effectiveRole = roleOverride ?: state.session.role
                if (effectiveRole == UserRole.CRAFTSMAN) {
                    CraftsmanBidSubmissionView(
                        request = state.request,
                        draft = state.bidDraft,
                        myOffer = state.mySubmittedOffer,
                        nowMillis = state.nowMillis,
                        onAdjustPrice = viewModel::adjustDraftPrice,
                        onSetPrice = viewModel::setDraftPrice,
                        onSelectEta = viewModel::setDraftEtaMinutes,
                        onNoteChange = viewModel::updateDraftNote,
                        onSubmitBid = viewModel::submitCraftsmanBid,
                        onAcceptAtClientPrice = viewModel::acceptAtClientPrice
                    )
                } else {
                    ClientRealtimeOffersView(
                        request = state.request,
                        offers = state.offers,
                        sortOption = state.sortOption,
                        nowMillis = state.nowMillis,
                        onSelectSort = viewModel::setOfferSort,
                        onAcceptOffer = viewModel::acceptOffer,
                        onDeclineOffer = viewModel::declineOffer,
                        onCounterOffer = viewModel::counterOffer,
                        onAdjustClientPrice = viewModel::adjustClientProposedPrice
                    )
                }
            }
        }
    }
}

@Composable
private fun CraftsmanBidSubmissionView(
    request: ServiceRequest,
    draft: CraftsmanBidDraft,
    myOffer: ServiceOffer?,
    nowMillis: Long,
    onAdjustPrice: (Int) -> Unit,
    onSetPrice: (Int) -> Unit,
    onSelectEta: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmitBid: () -> Unit,
    onAcceptAtClientPrice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (myOffer != null) {
            val remainingSec = ((myOffer.expiresAtMillis - nowMillis) / 1000L).coerceAtLeast(0L)
            Surface(
                color = if (myOffer.status == OfferStatus.COUNTERED) StarAmber.copy(alpha = 0.16f) else ElectricLimeSoft,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (myOffer.status == OfferStatus.COUNTERED) StarAmber else VerifiedEmerald.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (myOffer.status == OfferStatus.COUNTERED)
                                "عرض مضاد من الزبون: ${myOffer.offeredPrice} د.م"
                            else
                                "عرضك المرسل حالياً: ${myOffer.offeredPrice} د.م",
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${myOffer.status.labelAr} • الوصول خلال ${myOffer.etaMinutes} د • متبقي ${remainingSec} ث",
                            fontSize = 11.sp,
                            color = ObsidianBlack.copy(alpha = 0.75f)
                        )
                    }
                    Surface(
                        color = ObsidianBlack,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "عرضك الخاص",
                            color = ElectricLime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Price Stepper Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حدّد سعر عرضك", fontWeight = FontWeight.Bold)
                    Text(
                        text = "سعر الزبون: ${request.proposedPrice} د.م",
                        fontSize = 12.sp,
                        color = VerifiedEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onAdjustPrice(-10) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("bidding_minus_10")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "نقص 10", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("10", fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        color = ObsidianBlack,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "${draft.offeredPrice} درهم",
                            color = ElectricLime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = { onAdjustPrice(10) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("bidding_plus_10")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "زيد 10", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("10", fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Price Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf(
                        request.proposedPrice to "سعر الزبون (${request.proposedPrice})",
                        (request.proposedPrice + 15) to "+15 د.م",
                        (request.proposedPrice + 30) to "+30 د.م",
                        (request.proposedPrice + 50) to "+50 د.م"
                    )
                    items(presets) { (price, label) ->
                        FilterChip(
                            selected = draft.offeredPrice == price,
                            onClick = { onSetPrice(price) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ObsidianBlack,
                                selectedLabelColor = ElectricLime
                            )
                        )
                    }
                }
            }
        }

        // ETA Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text("وقت الوصول:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(5, 10, 15, 20, 30)) { mins ->
                    FilterChip(
                        selected = draft.etaMinutes == mins,
                        onClick = { onSelectEta(mins) },
                        label = { Text("$mins د", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricLime,
                            selectedLabelColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("eta_chip_$mins")
                    )
                }
            }
        }

        OutlinedTextField(
            value = draft.note,
            onValueChange = onNoteChange,
            label = { Text("ملاحظة أو تفاصيل للزبون (اختياري)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("craftsman_bid_note_input")
        )

        Button(
            onClick = onSubmitBid,
            enabled = !draft.isSubmitting,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_craftsman_bid_button")
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (draft.isSubmitting) "جاري الإرسال..." else "إرسال عرض السعر (${draft.offeredPrice} د.م)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        OutlinedButton(
            onClick = onAcceptAtClientPrice,
            enabled = !draft.isSubmitting,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("bidding_accept_client_price_button")
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "قبول فوري بسعر الزبون (${request.proposedPrice} د.م)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ClientRealtimeOffersView(
    request: ServiceRequest,
    offers: List<ServiceOffer>,
    sortOption: OfferSortOption,
    nowMillis: Long,
    onSelectSort: (OfferSortOption) -> Unit,
    onAcceptOffer: (ServiceOffer) -> Unit,
    onDeclineOffer: (String) -> Unit,
    onCounterOffer: (ServiceOffer, Int) -> Unit,
    onAdjustClientPrice: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (offers.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(OfferSortOption.entries) { opt ->
                    FilterChip(
                        selected = sortOption == opt,
                        onClick = { onSelectSort(opt) },
                        label = { Text(opt.labelAr, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ObsidianBlack,
                            selectedLabelColor = ElectricLime
                        ),
                        modifier = Modifier.testTag("bidding_sort_${opt.name.lowercase()}")
                    )
                }
            }
        }

        if (offers.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = VerifiedEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "في انتظار عروض الحرفيين المباشرة...",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "ستظهر العروض فور وصولها ويمكنك قبولها أو إرسال عرض مضاد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val bestPriceId = offers.minByOrNull { it.offeredPrice }?.id
            val fastestId = offers.minByOrNull { it.etaMinutes }?.id
            val topRatedId = offers.maxByOrNull { it.craftsmanRating }?.id

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(offers, key = { it.id }) { offer ->
                    val badge = when (sortOption) {
                        OfferSortOption.BEST_PRICE -> if (offer.id == bestPriceId) "الأقل سعراً" else null
                        OfferSortOption.FASTEST_ETA -> if (offer.id == fastestId) "الأسرع وصولاً" else null
                        OfferSortOption.TOP_RATED -> if (offer.id == topRatedId) "الأعلى تقييماً" else null
                    }
                    IncomingOfferCard(
                        offer = offer,
                        clientProposedPrice = request.proposedPrice,
                        nowMillis = nowMillis,
                        onAccept = { onAcceptOffer(offer) },
                        onDecline = { onDeclineOffer(offer.id) },
                        onCounterOffer = { price -> onCounterOffer(offer, price) },
                        highlightBadge = badge
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onAdjustClientPrice(-10) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("-10 د.م", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onAdjustClientPrice(10) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
            ) {
                Text("رفع السعر +10 د.م", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onAdjustClientPrice(20) },
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
