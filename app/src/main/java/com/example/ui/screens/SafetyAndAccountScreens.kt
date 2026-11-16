package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UrgentCoral
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مركز الأمان والطوارئ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = ElectricLime, modifier = Modifier.size(34.dp))
                    Text("أمانك أولوية في حرفي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "على غرار معايير الأمان في inDrive، يتم التحقق من هوية الحرفيين وتقييماتهم قبل إتاحة العروض، مع أزرار اتصال سريع للطوارئ.",
                        color = Color(0xFFB5BFC9),
                        fontSize = 13.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                EmergencyCard(
                    title = "الشرطة (19)",
                    icon = Icons.Rounded.LocalPolice,
                    color = UrgentCoral,
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:19"))) },
                    modifier = Modifier.weight(1f)
                )
                EmergencyCard(
                    title = "الوقاية المدنية (150)",
                    icon = Icons.Rounded.MedicalServices,
                    color = UrgentCoral,
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:150"))) },
                    modifier = Modifier.weight(1f)
                )
            }

            SafetyTipRow(
                icon = Icons.Rounded.VerifiedUser,
                title = "تحقق من شارة التوثيق",
                body = "تأكد من تطابق اسم الحرفي وصفته مع ما يظهر لك في بطاقة العرض المقبول."
            )
            SafetyTipRow(
                icon = Icons.Rounded.Lock,
                title = "اتفق على السعر قبل البدء",
                body = "السعر الذي قبلته في التطبيق هو السعر المعتمد. أي تعديل إضافي يتم بالاتفاق المباشر."
            )
            SafetyTipRow(
                icon = Icons.Rounded.CheckCircle,
                title = "قيّم الخدمة بعد الانتهاء",
                body = "تقييمك الصادق يحافظ على جودة وموثوقية الحرفيين في شبكتنا."
            )
        }
    }
}

@Composable
private fun EmergencyCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            Spacer(Modifier.size(6.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Call, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("اتصال سريع", fontSize = 11.sp, color = color)
            }
        }
    }
}

@Composable
private fun SafetyTipRow(icon: ImageVector, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
