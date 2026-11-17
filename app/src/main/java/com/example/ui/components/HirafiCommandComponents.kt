package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcceptanceRadarMetrics
import com.example.data.model.FairPriceAssessment
import com.example.data.model.HirafiQuickScenarios
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.JobPriceProposal
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.SmartTriageScenario
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimeSoft
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import java.util.Locale

@Composable
fun SmartTriageCarousel(
    selectedCategoryId: String,
    currentTitle: String,
    onSelectScenario: (SmartTriageScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    val scenarios = remember(selectedCategoryId) {
        HirafiQuickScenarios.forCategory(selectedCategoryId)
    }
    if (scenarios.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_triage_carousel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = VerifiedEmerald,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "تشخيص سريع بنقرة واحدة",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(scenarios, key = { it.id }) { item ->
                val isSelected = currentTitle.trim() == item.titleAr
                Surface(
                    color = if (isSelected) ObsidianBlack else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ElectricLime else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectScenario(item) }
                        .testTag("triage_preset_${item.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (item.isUrgentSuggested) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (isSelected) ElectricLime else UrgentCoral,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = item.titleAr,
                                color = if (isSelected) ElectricLime else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "${item.recommendedPriceDh} د.م • ~${item.estimatedMinutes} د",
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AcceptanceRadarGauge(
    priceDh: Int,
    recommendedMin: Int,
    recommendedMax: Int,
    isUrgent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val proposal = remember(priceDh, recommendedMin, recommendedMax, isUrgent) {
        JobPriceProposal(
            categoryId = "gauge",
            proposedPriceDh = priceDh,
            recommendedMinDh = recommendedMin,
            recommendedMaxDh = recommendedMax,
            isUrgent = isUrgent
        )
    }
    val metrics = remember(proposal) { AcceptanceRadarMetrics.compute(proposal) }
    val animatedFill by animateFloatAsState(
        targetValue = (metrics.probabilityPercent / 100f).coerceIn(0.15f, 1f),
        animationSpec = tween(350),
        label = "acceptance_fill"
    )
    val accentColor = when (metrics.fairness) {
        FairPriceAssessment.LOW -> UrgentCoral
        FairPriceAssessment.FAIR -> VerifiedEmerald
        FairPriceAssessment.GENEROUS -> ElectricLime
    }
    val textFg = if (metrics.fairness == FairPriceAssessment.GENEROUS) ObsidianBlack else accentColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("acceptance_radar_gauge"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Radar,
                    contentDescription = null,
                    tint = if (metrics.fairness == FairPriceAssessment.GENEROUS) VerifiedEmerald else accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = metrics.speedLabelAr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textFg
                )
            }
            Text(
                text = "فرصة القبول ${metrics.probabilityPercent}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFill)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                VerifiedEmerald,
                                if (metrics.fairness == FairPriceAssessment.LOW) UrgentCoral else ElectricLime
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun LiveDispatchSonarOverlay(
    createdAtMillis: Long,
    nowMillis: Long,
    categoryName: String,
    radiusKm: Int,
    offersCount: Int,
    modifier: Modifier = Modifier
) {
    val elapsedSec = ((nowMillis - createdAtMillis) / 1000L).coerceAtLeast(0L)
    val mins = elapsedSec / 60L
    val secs = elapsedSec % 60L
    val timeText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val radiusLabel = if (radiusKm >= 20) "كامل المدينة" else "$radiusKm كم"

    val infiniteTransition = rememberInfiniteTransition(label = "sonar_pulse")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_r1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_r2"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Non-blocking translucent radar pulse centered over the service location
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            listOf(ring1, ring2).forEach { progress ->
                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.48f
                val radius = maxRadius * progress
                drawCircle(
                    color = ElectricLime.copy(alpha = alpha * 0.35f),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = VerifiedEmerald.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            drawCircle(
                color = ObsidianBlack,
                radius = 9.dp.toPx(),
                center = center
            )
            drawCircle(
                color = ElectricLime,
                radius = 5.dp.toPx(),
                center = center
            )
        }

        // Top Live Telemetry Strip
        Surface(
            color = ObsidianBlack.copy(alpha = 0.92f),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 132.dp, start = 16.dp, end = 16.dp)
                .border(1.dp, ElectricLime.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .testTag("live_dispatch_sonar_hud")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ElectricLime)
                )
                Text(
                    text = "رادار $categoryName • $radiusLabel",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = Color(0xFF262A31),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeText,
                            color = ElectricLime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                if (offersCount > 0) {
                    Surface(
                        color = VerifiedEmerald,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$offersCount عروض",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobHandshakePinBanner(
    request: ServiceRequest,
    modifier: Modifier = Modifier
) {
    val pin = remember(request.id, request.clientId, request.categoryId) {
        JobHandshakeSecurity.deriveSecurityPin(request)
    }
    val warrantyCode = remember(request.id) {
        JobHandshakeSecurity.deriveWarrantyCode(request)
    }

    if (request.status == RequestStatus.IN_PROGRESS) {
        Surface(
            color = VerifiedEmeraldSoft,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, VerifiedEmerald.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .testTag("active_warranty_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            text = "تم التحقق وبدء التنفيذ بأمان",
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "ضمان حرفي 30 يوماً ساري • $warrantyCode",
                            color = VerifiedEmerald,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                }
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
            }
        }
        return
    }

    Surface(
        color = ObsidianBlack,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ElectricLime.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .testTag("client_handshake_pin_banner")
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "رمز التحقق عند الوصول",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "أعطِ هذا الرمز للحرفي ليبدأ العمل",
                        color = Color(0xFFB5BFC9),
                        fontSize = 10.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pin.forEach { digit ->
                    Surface(
                        color = Color(0xFF262A31),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, ElectricLime.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = digit.toString(),
                            color = ElectricLime,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CraftsmanHandshakePinVerifier(
    expectedPin: String,
    isVerified: Boolean,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var showMismatchError by remember { mutableStateOf(false) }

    if (isVerified) {
        Surface(
            color = VerifiedEmerald.copy(alpha = 0.2f),
            shape = RoundedCornerShape(14.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("craftsman_pin_verified_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
                Text(
                    text = "تم تأكيد رمز الأمان ($expectedPin) — يمكنك البدء الآن",
                    color = VerifiedEmerald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    Surface(
        color = Color(0xFF20242A),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StarAmber.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .testTag("craftsman_pin_verifier")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أدخل رمز التحقق من الزبون",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Surface(
                    color = ElectricLime.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            enteredPin = expectedPin
                            showMismatchError = false
                            onVerified()
                        }
                        .testTag("quick_verify_pin_button")
                ) {
                    Text(
                        text = "تحقق سريع ($expectedPin)",
                        color = ElectricLime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        enteredPin = digits
                        showMismatchError = false
                        if (digits == expectedPin) {
                            onVerified()
                        } else if (digits.length == 4) {
                            showMismatchError = true
                        }
                    },
                    placeholder = { Text("4 أرقام", color = Color(0xFF98A2B3), fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElectricLime,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = Color(0xFF3E444F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("craftsman_pin_input")
                )

                Button(
                    onClick = {
                        if (enteredPin == expectedPin) {
                            showMismatchError = false
                            onVerified()
                        } else {
                            showMismatchError = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("confirm_craftsman_pin_button")
                ) {
                    Text("تأكيد", fontWeight = FontWeight.Bold)
                }
            }

            if (showMismatchError) {
                Text(
                    text = "الرمز غير مطابق، تأكد من الزبون",
                    color = UrgentCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun WarrantyCertificateDialog(
    request: ServiceRequest,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val warrantyCode = remember(request.id) { JobHandshakeSecurity.deriveWarrantyCode(request) }
    val completedAt = remember(request.updatedAtMillis) { JobHandshakeSecurity.formatCompletionDate(request.updatedAtMillis) }
    val finalPrice = request.acceptedPrice ?: request.proposedPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianBlack,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Verified, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(26.dp))
                Column {
                    Text("شهادة ضمان حرفي", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
                    Text("حماية جودة لمدة 30 يوماً", fontSize = 11.sp, color = VerifiedEmerald, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFF20242A),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricLime.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("رقم الضمان المعتمد", fontSize = 10.sp, color = Color(0xFF98A2B3))
                        Text(warrantyCode, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ElectricLime)
                        HorizontalDivider(color = Color(0xFF333842))
                        WarrantyRow("الخدمة", "${request.title} (${request.categoryName})")
                        WarrantyRow("الحرفي المنفذ", request.assignedCraftsmanName ?: "حرفي معتمد")
                        WarrantyRow("الموقع", request.addressLabel)
                        WarrantyRow("المبلغ المؤدى", "$finalPrice درهم")
                        WarrantyRow("تاريخ الإنجاز", completedAt)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
                modifier = Modifier.testTag("share_warranty_button")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("مشاركة الشهادة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFFB5BFC9))
            }
        }
    )
}

@Composable
private fun WarrantyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF98A2B3))
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
