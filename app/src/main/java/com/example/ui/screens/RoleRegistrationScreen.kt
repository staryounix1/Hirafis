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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
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

@Composable
fun RoleRegistrationScreen(
    initialSession: UserSession,
    onRegisterWithEmail: (
        fullName: String,
        email: String,
        password: String,
        city: String,
        role: UserRole,
        specialtyId: String?
    ) -> Unit,
    onGoogleAuthSuccess: (
        account: GoogleAuthAccount,
        city: String,
        role: UserRole,
        specialtyId: String?
    ) -> Unit,
    onShowError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedRole by remember { mutableStateOf(UserRole.CLIENT) }
    var selectedSpecialtyId by remember { mutableStateOf(initialSession.craftsmanSpecialtyId) }
    var fullName by remember { mutableStateOf(if (initialSession.isRoleLocked) initialSession.fullName else "") }
    var email by remember { mutableStateOf(initialSession.email) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf(initialSession.city) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val isEmailValid = LoginValidator.isValidEmail(email)
    val isNameValid = LoginValidator.isValidFullName(fullName)
    val isPasswordValid = LoginValidator.isValidPassword(password)

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = "تسجيل الدخول إلى حرفي",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "سجّل عبر بريدك الإلكتروني أو مباشرة بحساب غوغل",
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
                        text = "تنبيه: نوع الحساب (زبون أو حرفي) يُقفل من التسجيل الأول ولا يمكن التبديل بينهما لاحقاً.",
                        color = StarAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }

        // 1. Account Role Selection (Client vs Single-Profession Craftsman)
        Text(
            text = "1. نوع الحساب",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                subtitle = "مهنة واحدة دائمة",
                icon = Icons.Rounded.Build,
                selected = selectedRole == UserRole.CRAFTSMAN,
                onClick = { selectedRole = UserRole.CRAFTSMAN },
                tag = "register_role_craftsman",
                modifier = Modifier.weight(1f)
            )
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

        // 2. Email, Full Name & Password Credentials Card
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
                    text = "2. بيانات الدخول",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل") },
                    placeholder = { Text("مثال: يونس العلمي") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    trailingIcon = if (isNameValid) {
                        { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_full_name_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني (Email)") },
                    placeholder = { Text("name@example.com") },
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                    trailingIcon = if (isEmailValid) {
                        { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerifiedEmerald) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة السر") },
                    placeholder = { Text("6 أحرف على الأقل") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.testTag("toggle_password_visibility")
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (passwordVisible) "إخفاء كلمة السر" else "إظهار كلمة السر"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = {
                        if (password.isNotEmpty() && !isPasswordValid) {
                            Text("يجب أن تحتوي كلمة السر على 6 أحرف على الأقل", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                // City selector
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

                Button(
                    onClick = {
                        onRegisterWithEmail(
                            fullName,
                            email,
                            password,
                            selectedCity,
                            selectedRole,
                            if (selectedRole == UserRole.CRAFTSMAN) selectedSpecialtyId else null
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime, contentColor = ObsidianBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_registration_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedRole == UserRole.CLIENT)
                            "تسجيل الدخول كزبون"
                        else
                            "تسجيل الدخول كحرفي (${HirafiCategories.byId(selectedSpecialtyId).nameAr})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Divider ("OR")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "أو المتابعة السريعة",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        // 3. Google Sign-In Button
        OutlinedButton(
            onClick = {
                isGoogleLoading = true
                GoogleAuthHelper.launchGoogleSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    scope = scope,
                    onAuthSuccess = { account ->
                        isGoogleLoading = false
                        onGoogleAuthSuccess(
                            account,
                            selectedCity,
                            selectedRole,
                            if (selectedRole == UserRole.CRAFTSMAN) selectedSpecialtyId else null
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
            },
            enabled = !isGoogleLoading,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("google_sign_in_button")
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = ObsidianBlack,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("جاري الاتصال بحساب غوغل...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            } else {
                GoogleLogoBadge(modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "تسجيل الدخول بواسطة غوغل",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
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
