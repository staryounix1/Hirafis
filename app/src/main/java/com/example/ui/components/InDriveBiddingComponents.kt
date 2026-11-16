package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveRouteState
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimeSoft
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft

@Composable
fun PriceProposalBar(
    priceDh: Int,
    recommendedMin: Int,
    recommendedMax: Int,
    onAdjust: (Int) -> Unit,
    onSetExact: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ElectricLime)
                )
                Text(
                    text = "سعرك المقترح (inDrive)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "الموصى به: $recommendedMin–$recommendedMax د.م",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAdjust(-10) }
                    .testTag("price_minus_10")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "نقص 10", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("10", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            Surface(
                color = ObsidianBlack,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showCustomDialog = true }
                    .testTag("price_display_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "$priceDh د.م",
                        color = ElectricLime,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "تعديل السعر يدوياً",
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAdjust(10) }
                    .testTag("price_plus_10")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "زيد 10", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("10", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showCustomDialog) {
        var input by remember { mutableStateOf(priceDh.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("تحديد السعر المقترح", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) input = it },
                    label = { Text("السعر بالدرهم (DH)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        input.toIntOrNull()?.let(onSetExact)
                        showCustomDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack)
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun IncomingOfferCard(
    offer: ServiceOffer,
    clientProposedPrice: Int,
    nowMillis: Long,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCounterOffer: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightBadge: String? = null
) {
    var showCounterInput by remember { mutableStateOf(false) }
    var counterPrice by remember(offer.offeredPrice) {
        mutableIntStateOf(((offer.offeredPrice + clientProposedPrice) / 2).coerceAtLeast(50))
    }

    val totalWindow = (offer.expiresAtMillis - offer.createdAtMillis).coerceAtLeast(1L)
    val remaining = (offer.expiresAtMillis - nowMillis).coerceAtLeast(0L)
    val remainingSeconds = (remaining / 1000L).coerceAtLeast(0L)
    val progress = (remaining.toFloat() / totalWindow.toFloat()).coerceIn(0f, 1f)

    val priceDiff = offer.offeredPrice - clientProposedPrice
    val diffBadgeText = when {
        priceDiff == 0 -> "بنفس سعرك"
        priceDiff < 0 -> "أقل بـ ${-priceDiff} د.م"
        else -> "+$priceDiff د.م"
    }
    val diffBadgeBg = if (priceDiff <= 0) VerifiedEmeraldSoft else StarAmber.copy(alpha = 0.14f)
    val diffBadgeFg = if (priceDiff <= 0) VerifiedEmerald else Color(0xFFB36700)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (highlightBadge != null) 2.dp else 1.dp,
                color = if (highlightBadge != null) ElectricLime else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = ElectricLime,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                if (highlightBadge != null) {
                    Surface(
                        color = ObsidianBlack,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = highlightBadge,
                            color = ElectricLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Surface(
                    color = if (remainingSeconds <= 10L) UrgentCoral.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${remainingSeconds} ث",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingSeconds <= 10L) UrgentCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack)
                    ) {
                        Text(
                            text = offer.craftsmanName.take(1),
                            color = ElectricLime,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = offer.craftsmanName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "موثق",
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = StarAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = " ${offer.craftsmanRating} (${offer.completedJobs}) • ${offer.etaMinutes} د • ${offer.distanceKm} كم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${offer.offeredPrice} د.م",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = diffBadgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = diffBadgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffBadgeFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (offer.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = offer.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showCounterInput) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    IconButton(onClick = { counterPrice = (counterPrice - 10).coerceAtLeast(40) }) {
                        Icon(Icons.Rounded.Remove, contentDescription = "نقص")
                    }
                    Text(
                        text = "$counterPrice د.م",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { counterPrice += 10 }) {
                        Icon(Icons.Rounded.Add, contentDescription = "زيد")
                    }
                    Button(
                        onClick = {
                            onCounterOffer(counterPrice)
                            showCounterInput = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = ElectricLime),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("إرسال", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.75f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = UrgentCoral)
                    Spacer(Modifier.width(4.dp))
                    Text("رفض", color = UrgentCoral, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { showCounterInput = !showCounterInput },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مفاوضة", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("accept_offer_${offer.id}")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("قبول", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun ActiveJobSheet(
    request: ServiceRequest,
    liveRoute: LiveRouteState,
    onCallClick: (String) -> Unit,
    onChatClick: () -> Unit,
    onSafetyClick: () -> Unit,
    onAdvanceStatus: (RequestStatus) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        RequestStatus.ACCEPTED,
        RequestStatus.EN_ROUTE,
        RequestStatus.ARRIVED,
        RequestStatus.IN_PROGRESS,
        RequestStatus.COMPLETED
    )
    val currentIndex = steps.indexOf(request.status).coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = ElectricLimeSoft,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = request.status.labelAr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                        }
                    }

                    if ((request.status == RequestStatus.ACCEPTED || request.status == RequestStatus.EN_ROUTE) &&
                        liveRoute.etaMinutes > 0
                    ) {
                        Surface(
                            color = ObsidianBlack,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${liveRoute.etaMinutes} د • ${liveRoute.remainingDistanceKm} كم",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricLime,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${request.acceptedPrice ?: request.proposedPrice} د.م",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = VerifiedEmerald
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                steps.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (idx <= currentIndex) ElectricLime
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack)
                    ) {
                        Text(
                            text = (request.assignedCraftsmanName ?: "ح").take(1),
                            color = ElectricLime,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = request.assignedCraftsmanName ?: "الحرفي المعتمد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${request.categoryName} • ★ ${request.assignedCraftsmanRating ?: 4.9}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onCallClick(request.assignedCraftsmanPhone ?: "+212 600-000000") },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(VerifiedEmeraldSoft)
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = "اتصال", tint = VerifiedEmerald)
                    }
                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack)
                            .testTag("open_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "محادثة", tint = ElectricLime)
                    }
                    IconButton(
                        onClick = onSafetyClick,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(UrgentCoral.copy(alpha = 0.14f))
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = "الأمان SOS", tint = UrgentCoral)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            val nextAction: Pair<String, RequestStatus>? = when (request.status) {
                RequestStatus.ACCEPTED -> "تأكيد انطلاق الحرفي" to RequestStatus.EN_ROUTE
                RequestStatus.EN_ROUTE -> "تأكيد وصول الحرفي" to RequestStatus.ARRIVED
                RequestStatus.ARRIVED -> "بدء العمل الآن" to RequestStatus.IN_PROGRESS
                RequestStatus.IN_PROGRESS -> "إنهاء الخدمة وتقييم الحرفي" to RequestStatus.COMPLETED
                else -> null
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (request.status == RequestStatus.ACCEPTED || request.status == RequestStatus.EN_ROUTE) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.75f)
                            .height(50.dp)
                    ) {
                        Text("إلغاء", color = UrgentCoral, fontWeight = FontWeight.Bold)
                    }
                }

                if (nextAction != null) {
                    Button(
                        onClick = { onAdvanceStatus(nextAction.second) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("advance_job_status_button")
                    ) {
                        Text(nextAction.first, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingDialog(
    request: ServiceRequest,
    onDismiss: () -> Unit,
    onSubmit: (Int, List<String>, String) -> Unit
) {
    var stars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    val availableTags = listOf("عمل متقن", "في الموعد", "سعر مناسب", "لباقة واحترام", "نظافة المكان")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "قيّم خدمة ${request.assignedCraftsmanName ?: "الحرفي"}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { value ->
                        IconButton(onClick = { stars = value }) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "$value نجوم",
                                tint = if (value <= stars) StarAmber else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableTags.forEach { tag ->
                        val selected = selectedTags.contains(tag)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedTags.remove(tag) else selectedTags.add(tag)
                            },
                            label = { Text(tag, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricLime,
                                selectedLabelColor = ObsidianBlack
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("ملاحظاتك (اختياري)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(stars, selectedTags.toList(), comment) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack)
            ) {
                Text("إرسال التقييم", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لاحقاً") }
        }
    )
}
