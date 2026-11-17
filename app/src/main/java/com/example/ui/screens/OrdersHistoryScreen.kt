package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.HirafiCategories
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.ui.components.WarrantyCertificateDialog
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.HirafiViewModel

private enum class OrderFilterTab(val labelAr: String) {
    ALL("الكل"),
    ACTIVE("جارية"),
    COMPLETED("مكتملة"),
    CANCELLED("ملغاة")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersHistoryScreen(
    viewModel: HirafiViewModel,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val context = LocalContext.current
    val allRequests by viewModel.allRequests.collectAsStateWithLifecycle()
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(OrderFilterTab.ALL) }
    var warrantyTarget by remember { mutableStateOf<ServiceRequest?>(null) }

    val roleRequests = if (session.role == UserRole.CRAFTSMAN) {
        allRequests.filter { req ->
            req.categoryId == session.craftsmanSpecialtyId &&
                (req.assignedCraftsmanId == session.userId || req.status == RequestStatus.SEARCHING)
        }
    } else {
        allRequests.filter { it.clientId == session.userId || it.assignedCraftsmanId != null }
    }

    val filteredRequests = when (selectedFilter) {
        OrderFilterTab.ALL -> roleRequests
        OrderFilterTab.ACTIVE -> roleRequests.filter {
            it.status != RequestStatus.COMPLETED && it.status != RequestStatus.CANCELLED
        }
        OrderFilterTab.COMPLETED -> roleRequests.filter { it.status == RequestStatus.COMPLETED }
        OrderFilterTab.CANCELLED -> roleRequests.filter { it.status == RequestStatus.CANCELLED }
    }

    val completedList = roleRequests.filter { it.status == RequestStatus.COMPLETED }
    val totalAmountDh = completedList.sumOf { it.acceptedPrice ?: it.proposedPrice }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سجل الطلبات والضمانات", fontWeight = FontWeight.Bold)
                        if (session.role == UserRole.CRAFTSMAN) {
                            Text(
                                text = "مهنتك: ${HirafiCategories.byId(session.craftsmanSpecialtyId).nameAr} فقط",
                                fontSize = 11.sp,
                                color = VerifiedEmerald
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hirafi_hero),
                        contentDescription = "شعار خدمات حرفي",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                    )
                }
            }

            // Obsidian Smart Ledger Summary
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("orders_analytics_ledger")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LedgerStatTile(
                            label = if (session.role == UserRole.CRAFTSMAN) "مجموع الأرباح" else "إجمالي الخدمات",
                            value = "$totalAmountDh د.م",
                            modifier = Modifier.weight(1.2f)
                        )
                        LedgerStatTile(
                            label = "خدمات منجزة",
                            value = "${completedList.size}",
                            modifier = Modifier.weight(0.9f)
                        )
                        LedgerStatTile(
                            label = "شهادات ضمان",
                            value = "${completedList.size}",
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(OrderFilterTab.entries) { tab ->
                        FilterChip(
                            selected = selectedFilter == tab,
                            onClick = { selectedFilter = tab },
                            label = { Text(tab.labelAr, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ObsidianBlack,
                                selectedLabelColor = ElectricLime
                            ),
                            modifier = Modifier.testTag("order_filter_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredRequests.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("لا توجد طلبات في هذا التصنيف حالياً", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                items(filteredRequests, key = { it.id }) { req ->
                    val category = HirafiCategories.byId(req.categoryId)
                    val statusColor = when (req.status) {
                        RequestStatus.COMPLETED -> VerifiedEmerald
                        RequestStatus.CANCELLED -> UrgentCoral
                        else -> ObsidianBlack
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(category.accentColor.copy(alpha = 0.14f))
                                    ) {
                                        Icon(category.icon, contentDescription = null, tint = category.accentColor, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = req.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(req.categoryName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    color = if (req.status == RequestStatus.SEARCHING) ElectricLime else statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = req.status.labelAr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (req.status == RequestStatus.SEARCHING) ObsidianBlack else statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(req.addressLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "السعر: ${req.acceptedPrice ?: req.proposedPrice} درهم",
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedEmerald
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (req.status == RequestStatus.COMPLETED) {
                                        OutlinedButton(
                                            onClick = { warrantyTarget = req },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("open_warranty_${req.id}")
                                        ) {
                                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("شهادة الضمان", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (req.assignedCraftsmanName != null) {
                                        Button(
                                            onClick = { onOpenChat(req.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = ElectricLime),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("المحادثة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    warrantyTarget?.let { target ->
        WarrantyCertificateDialog(
            request = target,
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "شهادة ضمان خدمة حرفي")
                    putExtra(Intent.EXTRA_TEXT, JobHandshakeSecurity.buildWarrantyShareText(target))
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة شهادة الضمان"))
            },
            onDismiss = { warrantyTarget = null }
        )
    }
}

@Composable
private fun LedgerStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF20242A),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = label,
                color = Color(0xFF98A2B3),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = ElectricLime,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
