package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.AdminAuthorizationPolicy
import com.example.data.model.HirafiCategories
import com.example.data.model.LoginValidator
import com.example.data.model.MoroccanLocations
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.ui.auth.GoogleAuthAccount
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ElectricLimeSoft
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldSoft

@Composable
fun RoleRegistrationScreen(
    initialSession: UserSession,
    isCloudSaving: Boolean = false,
    currentFirebaseUid: String? = null,
    currentFirebaseEmail: String? = null,
    currentFirebaseDisplayName: String? = null,
    onGoogleAuthSuccess: (
        account: GoogleAuthAccount,
        preferredDisplayName: String,
        city: String,
        role: UserRole,
        specialtyId: String?
    ) -> Unit,
    onSwitchGoogleAccount: (() -> Unit)? = null,
    onShowError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedRole by remember { mutableStateOf(UserRole.CLIENT) }
    var selectedSpecialtyId by remember { mutableStateOf(initialSession.craftsmanSpecialtyId) }
    var fullName by remember(currentFirebaseUid) {
        mutableStateOf(
            when {
                initialSession.isRoleLocked -> initialSession.fullName
                !currentFirebaseDisplayName.isNullOrBlank() -> currentFirebaseDisplayName
                else -> ""
            }
        )
    }
    var selectedCity by remember { mutableStateOf(initialSession.city) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val isBusy = isGoogleLoading || isCloudSaving
    val isNameValid = LoginValidator.isValidFullName(fullName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("role_registration_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Obsidian Hero Header
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = ElectricLime,
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                tint = ObsidianBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "التسجيل السحابي في حرفي",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "مربوط مباشرة بقاعدة بيانات Firebase لتلاقي الزبائن والحرفيين",
                            color = ElectricLime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = Color(0xFF20242A),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تنبيه: نوع الحساب (زبون أو حرفي) يُحفظ ويُقفل لكل حساب غوغل في قاعدة البيانات. لاختبار الزبون والحرفي معاً، سجّل بحساب غوغل مختلف لكل دور.",
                        color = StarAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }

        if (!currentFirebaseUid.isNullOrBlank()) {
            Surface(
                color = VerifiedEmeraldSoft,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = VerifiedEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تم التحقق من حساب غوغل بنجاح",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VerifiedEmerald
                        )
                        if (!currentFirebaseEmail.isNullOrBlank()) {
                            Text(
                                text = currentFirebaseEmail,
                                fontSize = 12.sp,
                                color = ObsidianBlack
                            )
                        }
                    }
                }
            }
        }

        // 1. Account Role Selection (Client vs Single-Profession Craftsman)
        Text(
            text = "1. اختر نوع الحساب",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RoleSelectionCard(
                title = "زبون",
                subtitle = "طلب الخدمات",
                icon = Icons.Rounded.Person,
                selected = selectedRole == UserRole.CLIENT,
                onClick = { selectedRole = UserRole.CLIENT },
                tag = "register_role_client",
                modifier = Modifier.weight(1f)
            )
            RoleSelectionCard(
                title = "حرفي",
                subtitle = "مهنة واحدة",
                icon = Icons.Rounded.Build,
                selected = selectedRole == UserRole.CRAFTSMAN,
                onClick = { selectedRole = UserRole.CRAFTSMAN },
                tag = "register_role_craftsman",
                modifier = Modifier.weight(1f)
            )
            RoleSelectionCard(
                title = "أدمين",
                subtitle = "إدارة المنصة",
                icon = Icons.Rounded.AdminPanelSettings,
                selected = selectedRole == UserRole.ADMIN,
                onClick = { selectedRole = UserRole.ADMIN },
                tag = "register_role_admin",
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(visible = selectedRole == UserRole.ADMIN) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ElectricLimeSoft,
                border = BorderStroke(1.dp, ObsidianBlack.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "دخول الأدمين متاح لبريد الإدارة المعتمد (${AdminAuthorizationPolicy.BOOTSTRAP_ADMIN_EMAIL}) أو البريد المُضاف من لوحة الأدمين.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianBlack,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        AnimatedVisibility(visible = selectedRole == UserRole.CRAFTSMAN) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "اختر مهنتك الوحيدة الدائمة:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = VerifiedEmerald
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(HirafiCategories.all, key = { it.id }) { category ->
                        val isPicked = category.id == selectedSpecialtyId
                        Surface(
                            color = if (isPicked) ObsidianBlack else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .border(
                                    width = if (isPicked) 2.dp else 1.dp,
                                    color = if (isPicked) ElectricLime else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedSpecialtyId = category.id }
                                .testTag("register_specialty_${category.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isPicked) ElectricLime else category.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = category.nameAr,
                                    color = if (isPicked) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Display Name & City Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. الاسم الظاهر والمدينة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { if (it.length <= 80) fullName = it },
                    label = {
                        Text(
                            if (selectedRole == UserRole.CRAFTSMAN)
                                "الاسم الظاهر للزبائن (اختياري)"
                            else
                                "الاسم الكامل الظاهر للحرفيين (اختياري)"
                        )
                    },
                    placeholder = {
                        Text(
                            if (selectedRole == UserRole.CRAFTSMAN)
                                "مثال: أحمد السباك (أو يُؤخذ من غوغل)"
                            else
                                "مثال: يونس العلمي (أو يُؤخذ من غوغل)"
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    trailingIcon = if (isNameValid) {
                        { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald) }
                    } else null,
                    supportingText = {
                        Text("سيتم حفظ هذا الاسم ودورك مباشرة في قاعدة بيانات Firestore.")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_full_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                        Text("المدينة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MoroccanLocations.cities) { cityName ->
                            FilterChip(
                                selected = selectedCity == cityName,
                                onClick = { selectedCity = cityName },
                                label = { Text(cityName, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ObsidianBlack,
                                    selectedLabelColor = ElectricLime
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Primary Firebase Auth + Firestore Registration CTA
        val roleLabel = when (selectedRole) {
            UserRole.CLIENT -> "زبون"
            UserRole.CRAFTSMAN -> "حرفي (${HirafiCategories.byId(selectedSpecialtyId).nameAr})"
            UserRole.ADMIN -> "أدمين"
        }

        Button(
            onClick = {
                val chosenSpecialty = if (selectedRole == UserRole.CRAFTSMAN) selectedSpecialtyId else null
                if (!currentFirebaseUid.isNullOrBlank()) {
                    onGoogleAuthSuccess(
                        GoogleAuthAccount(
                            uid = currentFirebaseUid,
                            displayName = currentFirebaseDisplayName.orEmpty(),
                            email = currentFirebaseEmail.orEmpty()
                        ),
                        fullName.trim(),
                        selectedCity,
                        selectedRole,
                        chosenSpecialty
                    )
                } else {
                    isGoogleLoading = true
                    GoogleAuthHelper.launchGoogleSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onAuthSuccess = { account ->
                            isGoogleLoading = false
                            onGoogleAuthSuccess(
                                account,
                                fullName.trim(),
                                selectedCity,
                                selectedRole,
                                chosenSpecialty
                            )
                        },
                        onAuthError = { msg ->
                            isGoogleLoading = false
                            onShowError(msg)
                        },
                        onAuthCancelled = {
                            isGoogleLoading = false
                        }
                    )
                }
            },
            enabled = !isBusy,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricLime,
                contentColor = ObsidianBlack
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("google_sign_in_button")
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = ObsidianBlack,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isCloudSaving) "جاري حفظ الحساب في قاعدة البيانات..." else "جاري الاتصال بحساب غوغل...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            } else if (!currentFirebaseUid.isNullOrBlank()) {
                Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "تأكيد التسجيل كـ $roleLabel في قاعدة البيانات",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            } else {
                GoogleLogoBadge(modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "إنشاء حساب $roleLabel عبر غوغل",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }

        if (!currentFirebaseUid.isNullOrBlank() && onSwitchGoogleAccount != null) {
            OutlinedButton(
                onClick = onSwitchGoogleAccount,
                enabled = !isBusy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("switch_google_account_button")
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("استخدام حساب غوغل آخر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GoogleLogoBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.20f, cap = StrokeCap.Butt)
        val inset = stroke.width / 2f
        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -35f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 150f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 210f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.50f),
            end = Offset(size.width * 0.92f, size.height * 0.50f),
            strokeWidth = stroke.width
        )
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) ElectricLimeSoft else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ObsidianBlack else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (selected) ObsidianBlack else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) ElectricLime else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (selected) ObsidianBlack else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (selected) ObsidianBlack.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) ObsidianBlack else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
