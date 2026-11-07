package com.example.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CctvTopAppBar
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.StatusConnectedGreen
import com.example.ui.theme.StatusLiveRed
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun PrivacyAndPermissionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val hasCamera = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val hasMic = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasNearby = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    val hasNotification = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .testTag("screen_privacy_permissions")
    ) {
        CctvTopAppBar(
            title = "Privacy & Permissions",
            subtitle = "Zero-Cloud Guarantee",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Guarantee Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkNavyCard)
                    .border(1.5.dp, StatusConnectedGreen, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StatusConnectedGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = StatusConnectedGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("100% Offline Promise", color = TextWhitePrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Zero Cloud • Zero Telemetry • Zero Trackers", color = StatusConnectedGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TwinGuard CCTV operates strictly on your local device-to-device network. Your live camera feed and video recordings never leave your local Wi-Fi or Wi-Fi Direct connection. We do not use cloud databases, Firebase, external servers, or public internet routing.",
                    color = TextMutedSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ACTIVE ANDROID PERMISSIONS",
                color = TextMutedSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionStatusCard(
                name = "Camera Hardware Access",
                description = "Required to capture live video stream on Camera node.",
                isGranted = hasCamera
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionStatusCard(
                name = "Microphone Access",
                description = "Required for two-way audio intercom and ambient sound.",
                isGranted = hasMic
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionStatusCard(
                name = "Nearby Wi-Fi Devices / P2P",
                description = "Required for direct peer-to-peer Wi-Fi Direct device pairing.",
                isGranted = hasNearby
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionStatusCard(
                name = "Persistent Notifications",
                description = "Shows visible recording and streaming indicators while running.",
                isGranted = hasNotification
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Security Features Overview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkNavyCard)
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text("Security Principles", color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                SecurityBullet("No hidden recording — always displays visible recording HUD and notification.")
                SecurityBullet("Temporary 6-digit random PIN required for all viewer connections.")
                SecurityBullet("Immediate Stop Camera and Disconnect buttons with single-tap shutdown.")
                SecurityBullet("No facial recognition or biometric profiling performed.")
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    name: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavyCard)
            .border(1.dp, if (isGranted) DarkNavyBorder else StatusLiveRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = TextMutedSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isGranted) StatusConnectedGreen.copy(alpha = 0.15f) else StatusLiveRed.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isGranted) "GRANTED" else "DENIED",
                color = if (isGranted) StatusConnectedGreen else StatusLiveRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SecurityBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("• ", color = CyanNeon, fontWeight = FontWeight.Bold)
        Text(text, color = TextMutedSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
