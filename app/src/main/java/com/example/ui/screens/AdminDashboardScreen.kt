package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AdminAuthorizationPolicy
import com.example.data.model.AdminUserRecord
import com.example.data.model.HirafiCategories
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.VerificationSubmission
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimeSoft
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.UrgentCoralSoft
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft
import com.example.ui.viewmodel.HirafiViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: HirafiViewModel,
    onOpenDrawer: () -> Unit
) {
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val runtimeConfig by viewModel.appRuntimeConfig.collectAsStateWithLifecycle()
    val verifications by viewModel.adminVerifications.collectAsStateWithLifecycle()
    val users by viewModel.adminUsers.collectAsStateWithLifecycle()
    val requests by viewModel.adminAllRequests.collectAsStateWithLifecycle()
    val authorizedAdmins by viewModel.authorizedAdmins.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var userRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }
    var rejectTarget by remember { mutableStateOf<VerificationSubmission?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var newAdminEmail by remember { mutableStateOf("") }

    val pendingKycCount = verifications.count { it.status == IdentityVerificationStatus.PENDING_REVIEW }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        topBar = {
            Surface(
                color = ObsidianBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Surface(
                        color = Color(0xFF232830),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("admin_open_drawer_button")
                        ) {
                            Icon(Icons.Rounded.Menu, contentDescription = "القائمة", tint = Color.White)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "لوحة تحكم الأدمين",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = session.email.ifBlank { AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL },
                            color = ElectricLime,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = if (runtimeConfig.productionMode) VerifiedEmerald else ElectricLimeSoft,
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text(
                            text = if (runtimeConfig.productionMode) "إنتاج حقيقي" else "وضع مختلط",
                            color = if (runtimeConfig.productionMode) Color.White else ObsidianBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                    label = { Text("الإدارة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_overview")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingKycCount > 0) {
                                    Badge(containerColor = UrgentCoral) {
                                        Text("$pendingKycCount", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null)
                        }
                    },
                    label = { Text("التوثيق", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_kyc")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Group, contentDescription = null) },
                    label = { Text("المستخدمون", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_users")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null) },
                    label = { Text("الطلبات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_requests")
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> AdminOverviewTab(
                productionMode = runtimeConfig.productionMode,
                usersCount = users.size,
                craftsmenCount = users.count { it.role == UserRole.CRAFTSMAN },
                pendingKycCount = pendingKycCount,
                activeRequestsCount = requests.count {
                    it.status != RequestStatus.COMPLETED && it.status != RequestStatus.CANCELLED
                },
                authorizedAdmins = authorizedAdmins.map { it.email },
                onToggleProductionMode = { viewModel.setProductionMode(it) },
                onOpenAddAdmin = { showAddAdminDialog = true },
                onRemoveAdmin = { viewModel.removeAuthorizedAdmin(it) },
                modifier = Modifier.padding(innerPadding)
            )

            1 -> AdminKycTab(
                submissions = verifications,
                onPreviewPhoto = { previewPhotoUrl = it },
                onApprove = { sub -> viewModel.reviewVerification(sub.craftsmanId, approved = true) },
                onRejectClick = { sub ->
                    rejectTarget = sub
                    rejectionReasonText = ""
                },
                modifier = Modifier.padding(innerPadding)
            )

            2 -> AdminUsersTab(
                users = users,
                roleFilter = userRoleFilter,
                onRoleFilterChange = { userRoleFilter = it },
                onToggleSuspended = { u ->
                    viewModel.toggleUserSuspended(
                        userId = u.userId,
                        isCraftsman = u.role == UserRole.CRAFTSMAN,
                        suspended = !u.isSuspended
                    )
                },
                modifier = Modifier.padding(innerPadding)
            )

            else -> AdminRequestsTab(
                requests = requests,
                onCancelRequest = { viewModel.cancelRequestAsAdmin(it) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    previewPhotoUrl?.let { uri ->
        Dialog(onDismissRequest = { previewPhotoUrl = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { previewPhotoUrl = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                    AsyncImage(
                        model = uri,
                        contentDescription = "معاينة الوثيقة",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
            }
        }
    }

    rejectTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("سبب رفض التوثيق", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل سبب الرفض ليظهر للحرفي (${target.craftsmanName}):", fontSize = 13.sp)
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = { rejectionReasonText = it },
                        placeholder = { Text("مثال: صورة البطاقة الخلفية غير واضحة") },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reviewVerification(
                            craftsmanId = target.craftsmanId,
                            approved = false,
                            rejectionReason = rejectionReasonText
                        )
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentCoral, contentColor = Color.White)
                ) {
                    Text("تأكيد الرفض", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("إلغاء") }
            }
        )
    }

    if (showAddAdminDialog) {
        AlertDialog(
            onDismissRequest = { showAddAdminDialog = false },
            title = { Text("إضافة أدمين جديد", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل البريد الإلكتروني المخول له تسجيل الدخول كأدمين:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        placeholder = { Text("admin@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addAuthorizedAdmin(newAdminEmail)
                        newAdminEmail = ""
                        showAddAdminDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack)
                ) {
                    Text("إضافة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAdminDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    productionMode: Boolean,
    usersCount: Int,
    craftsmenCount: Int,
    pendingKycCount: Int,
    activeRequestsCount: Int,
    authorizedAdmins: List<String>,
    onToggleProductionMode: (Boolean) -> Unit,
    onOpenAddAdmin: () -> Unit,
    onRemoveAdmin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdminStatCard("المستخدمون", "$usersCount", ObsidianBlack, ElectricLime, Modifier.weight(1f))
                AdminStatCard("الحرفيون", "$craftsmenCount", VerifiedEmeraldSoft, VerifiedEmerald, Modifier.weight(1f))
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdminStatCard("ملفات KYC معلقة", "$pendingKycCount", UrgentCoralSoft, UrgentCoral, Modifier.weight(1f))
                AdminStatCard("طلبات نشطة", "$activeRequestsCount", ElectricLimeSoft, ObsidianBlack, Modifier.weight(1f))
            }
        }

        // Production Real Data Mode Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, if (productionMode) VerifiedEmerald else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        color = if (productionMode) VerifiedEmeraldSoft else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.RocketLaunch,
                                contentDescription = null,
                                tint = if (productionMode) VerifiedEmerald else ObsidianBlack
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("وضع البيانات الحقيقية بالكامل", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(
                            text = if (productionMode)
                                "مفعّل: يظهر الحرفيون والطلبات المسجلون في السحابة فقط بدون بيانات تجريبية."
                            else
                                "معطّل: تظهر بيانات العرض التجريبية بجانب الحسابات الحقيقية.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = productionMode,
                        onCheckedChange = onToggleProductionMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VerifiedEmerald
                        ),
                        modifier = Modifier.testTag("admin_production_mode_switch")
                    )
                }
            }
        }

        // Authorized Admins Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = VerifiedEmerald)
                            Text("حسابات الأدمين المخولة", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        OutlinedButton(onClick = onOpenAddAdmin, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Surface(color = ObsidianBlack, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text("أدمين رئيسي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    authorizedAdmins.forEach { email ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(email, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { onRemoveAdmin(email) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "حذف الأدمين", tint = UrgentCoral, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Release Checklist Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = ElectricLime)
                        Text("جاهزية الإطلاق والإنتاج", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                    Text("• المعرف الرسمي للتطبيق: com.hirafi.app", color = Color(0xFFD6DDE6), fontSize = 12.sp)
                    Text("• الخادم السحابي: Supabase Auth + PostgreSQL (ملف supabase/schema.sql)", color = VerifiedEmerald, fontSize = 12.sp)
                    Text("• Supabase Storage: تأكد من تشغيل schema.sql لإنشاء Bucket: hirafi-media", color = ElectricLime, fontSize = 12.sp)
                    Text("• الإشعارات المحلية: قنوات تنبيهات العروض والرسائل وتوثيق الهوية مدمجة", color = Color(0xFFD6DDE6), fontSize = 12.sp)
                    Text("• قبل النشر: أضف مفاتيح SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets", color = Color(0xFFD6DDE6), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    container: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = accent, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = label,
                color = if (container == ObsidianBlack) Color.White else ObsidianBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AdminKycTab(
    submissions: List<VerificationSubmission>,
    onPreviewPhoto: (String) -> Unit,
    onApprove: (VerificationSubmission) -> Unit,
    onRejectClick: (VerificationSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    if (submissions.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا توجد طلبات توثيق هوية حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(submissions, key = { it.craftsmanId }) { sub ->
            val cat = HirafiCategories.byId(sub.specialtyId)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.craftsmanName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("${cat.nameAr} • ${sub.city} • CIN: ${sub.nationalIdNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = when (sub.status) {
                                IdentityVerificationStatus.VERIFIED -> VerifiedEmeraldSoft
                                IdentityVerificationStatus.REJECTED -> UrgentCoralSoft
                                else -> ElectricLimeSoft
                            },
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = sub.status.labelAr,
                                color = when (sub.status) {
                                    IdentityVerificationStatus.VERIFIED -> VerifiedEmerald
                                    IdentityVerificationStatus.REJECTED -> UrgentCoral
                                    else -> ObsidianBlack
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KycThumb("الأمامية", sub.frontPhotoUri, { onPreviewPhoto(sub.frontPhotoUri) }, Modifier.weight(1f))
                        KycThumb("الخلفية", sub.backPhotoUri, { onPreviewPhoto(sub.backPhotoUri) }, Modifier.weight(1f))
                        KycThumb("السيلفي", sub.selfiePhotoUri, { onPreviewPhoto(sub.selfiePhotoUri) }, Modifier.weight(1f))
                    }

                    if (sub.status == IdentityVerificationStatus.REJECTED && sub.rejectionReason.isNotBlank()) {
                        Text("سبب الرفض: ${sub.rejectionReason}", color = UrgentCoral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onApprove(sub) },
                            colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اعتماد وتوثيق", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onRejectClick(sub) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, UrgentCoral),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentCoral),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("رفض", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KycThumb(
    label: String,
    uri: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(enabled = uri.isNotBlank(), onClick = onClick)
        ) {
            if (uri.isNotBlank()) {
                AsyncImage(
                    model = uri,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("غير مرفقة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AdminUsersTab(
    users: List<AdminUserRecord>,
    roleFilter: UserRole?,
    onRoleFilterChange: (UserRole?) -> Unit,
    onToggleSuspended: (AdminUserRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = users.filter { roleFilter == null || it.role == roleFilter }
    Column(modifier = modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            item {
                FilterChip(
                    selected = roleFilter == null,
                    onClick = { onRoleFilterChange(null) },
                    label = { Text("الكل (${users.size})") }
                )
            }
            items(UserRole.entries) { role ->
                FilterChip(
                    selected = roleFilter == role,
                    onClick = { onRoleFilterChange(role) },
                    label = { Text("${role.labelAr} (${users.count { it.role == role }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ObsidianBlack,
                        selectedLabelColor = ElectricLime
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.userId }) { u ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (u.isSuspended) UrgentCoral else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(u.fullName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Surface(color = ElectricLimeSoft, shape = RoundedCornerShape(99.dp)) {
                                    Text(u.role.labelAr, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                }
                            }
                            Text("${u.email} • ${u.city}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (u.fcmTokenPresent) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(13.dp))
                                        Text("إشعارات مفعلة", fontSize = 10.sp, color = VerifiedEmerald, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (u.role == UserRole.CRAFTSMAN) {
                                    Text(u.verificationStatus.labelAr, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (u.role != UserRole.ADMIN) {
                            OutlinedButton(
                                onClick = { onToggleSuspended(u) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (u.isSuspended) VerifiedEmerald else UrgentCoral),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (u.isSuspended) VerifiedEmerald else UrgentCoral
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (u.isSuspended) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (u.isSuspended) "تفعيل" else "تعليق", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRequestsTab(
    requests: List<ServiceRequest>,
    onCancelRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (requests.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا توجد طلبات مسجلة في السحابة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(requests, key = { it.id }) { req ->
            val isTerminal = req.status == RequestStatus.COMPLETED || req.status == RequestStatus.CANCELLED
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(req.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Surface(
                            color = if (isTerminal) MaterialTheme.colorScheme.surfaceVariant else ElectricLimeSoft,
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(req.status.labelAr, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
                        }
                    }
                    Text("${req.categoryName} • ${req.proposedPrice} درهم • ${req.addressLabel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("الزبون: ${req.clientName}" + (req.assignedCraftsmanName?.let { " • الحرفي: $it" } ?: ""), fontSize = 12.sp)

                    if (!isTerminal) {
                        OutlinedButton(
                            onClick = { onCancelRequest(req.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, UrgentCoral),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentCoral),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("إلغاء الطلب إدارياً", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
