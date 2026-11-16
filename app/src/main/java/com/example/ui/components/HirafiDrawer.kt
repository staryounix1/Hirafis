package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HirafiCategories
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StarAmber
import com.example.ui.theme.VerifiedEmerald

@Composable
fun HirafiDrawerContent(
    session: UserSession,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onCityClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(305.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Authentic inDrive Clean Profile Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack)
                    ) {
                        Text(
                            text = session.fullName.take(1),
                            color = ElectricLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.fullName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = StarAmber, modifier = Modifier.size(14.dp))
                            Text(
                                text = "4.9 • ${session.role.labelAr}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (session.role == UserRole.CRAFTSMAN && session.isCraftsmanSpecialtyLocked) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "مهنة واحدة: ${HirafiCategories.byId(session.craftsmanSpecialtyId).nameAr}",
                                    color = VerifiedEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                DrawerNavItem(
                    label = if (session.role == UserRole.CRAFTSMAN) "الطلبات الواردة والخريطة" else "طلب خدمة (الخريطة)",
                    icon = Icons.Rounded.Home,
                    selected = currentRoute == "home",
                    onClick = { onNavigate("home") }
                )
                DrawerNavItem(
                    label = "سجل الطلبات",
                    icon = Icons.Rounded.History,
                    selected = currentRoute == "orders",
                    onClick = { onNavigate("orders") }
                )
                DrawerNavItem(
                    label = "الأمان والطوارئ",
                    icon = Icons.Rounded.Shield,
                    selected = currentRoute == "safety",
                    onClick = { onNavigate("safety") }
                )
                DrawerNavItem(
                    label = "المدينة: ${session.city}",
                    icon = Icons.Rounded.LocationCity,
                    selected = false,
                    onClick = onCityClick
                )
            }

            // Signature inDrive Bottom Full-Width Neon Switcher
            Column {
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                val targetRole = if (session.role == UserRole.CLIENT) UserRole.CRAFTSMAN else UserRole.CLIENT
                Button(
                    onClick = { onSwitchRole(targetRole) },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricLime,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("switch_role_button")
                ) {
                    Icon(
                        imageVector = if (targetRole == UserRole.CRAFTSMAN) Icons.Rounded.Build else Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (targetRole == UserRole.CRAFTSMAN)
                            "وضع الحرفي (السائق)"
                        else
                            "وضع الزبون (العميل)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = ElectricLime.copy(alpha = 0.35f),
            selectedIconColor = ObsidianBlack,
            selectedTextColor = ObsidianBlack
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
