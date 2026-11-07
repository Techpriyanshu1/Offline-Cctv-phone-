package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.networking.NetworkUtils
import com.example.storage.FileManager
import com.example.storage.TwinGuardDatabase
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardElevated
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.StatusConnectedGreen
import com.example.ui.theme.StatusLiveRed
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun HomeDashboardScreen(
    onNavigateCameraMode: () -> Unit,
    onNavigateMonitorMode: () -> Unit,
    onNavigateRecordings: () -> Unit,
    onNavigateMotionHistory: () -> Unit,
    onNavigateDeviceManagement: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigatePrivacy: () -> Unit,
    onConnectQr: (ip: String, port: Int, pin: String, name: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val db = remember { TwinGuardDatabase.getDatabase(context) }
    val fileManager = remember { FileManager(context) }

    val recordingsCount by db.recordingsDao().getRecordingsCount().collectAsState(initial = 0)
    val motionEventsCount by db.motionEventsDao().getEventCount().collectAsState(initial = 0)
    val pairedCameras by db.pairedCamerasDao().getAllPairedCameras().collectAsState(initial = emptyList())

    val localIp = remember { NetworkUtils.getLocalIpAddress() }
    val storageStats = remember { fileManager.getStorageStats() }

    var showGuideDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showCameraQrDialog by remember { mutableStateOf(false) }
    val cameraPairingPin = remember { NetworkUtils.generatePairingPin() }

    if (showGuideDialog) {
        com.example.ui.components.ConnectionGuideDialog(onDismiss = { showGuideDialog = false })
    }

    if (showCameraQrDialog) {
        com.example.ui.components.CameraQrCodeDialog(
            localIp = localIp,
            port = 8888,
            pin = cameraPairingPin,
            cameraName = "TwinGuard Node",
            onStartCamera = onNavigateCameraMode,
            onDismiss = { showCameraQrDialog = false }
        )
    }

    if (showQrScannerDialog) {
        com.example.pairing.QrScannerDialog(
            onDismiss = { showQrScannerDialog = false },
            onQrScanned = { ip, port, pin, name ->
                showQrScannerDialog = false
                onConnectQr(ip, port, pin, name)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("screen_dashboard")
    ) {
        // Top App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkNavyCard)
                        .border(1.dp, CyanNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "TwinGuard CCTV",
                        color = TextWhitePrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StatusConnectedGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Offline P2P Ready • $localIp",
                            color = StatusConnectedGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.testTag("btn_dashboard_guide")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Connection Guide",
                        tint = CyanNeon
                    )
                }

                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier.testTag("btn_dashboard_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextMutedSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Connect Guide Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkNavyCard)
                .border(1.dp, CyanNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { showGuideDialog = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Wifi,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "New device? Tap here for 3-step offline setup guide",
                    color = TextWhitePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Guide >",
                color = CyanNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Card 1: CAMERA MODE
        HeroOperatingCard(
            title = "CAMERA MODE",
            subtitle = "Turn this phone into a CCTV camera.",
            description = "Stream live 1080p/720p video, on-device motion detection, remote torch & audio intercom.",
            actionLabel = "Start Camera Node",
            icon = Icons.Default.Videocam,
            accentColor = CyanNeon,
            tag = "btn_start_camera_mode",
            badgeText = "CAMERA NODE",
            onClick = onNavigateCameraMode
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showCameraQrDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("btn_dashboard_show_camera_qr"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCardElevated, contentColor = CyanNeon)
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Show Camera QR Code to Pair", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Card 2: MONITOR MODE
        HeroOperatingCard(
            title = "MONITOR MODE",
            subtitle = "Connect to another phone and watch live.",
            description = "Real-time viewer, pinch-to-zoom, two-way intercom, remote controls, and MP4 recording.",
            actionLabel = "Connect & Watch Feed",
            icon = Icons.Default.Visibility,
            accentColor = CyanAccent,
            tag = "btn_start_monitor_mode",
            badgeText = "VIEWER NODE",
            onClick = onNavigateMonitorMode
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { showQrScannerDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_dashboard_scan_qr"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCardElevated, contentColor = CyanNeon)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Camera QR to Watch Instantly", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Overview Stats Section
        Text(
            text = "SYSTEM & RECENT EVENTS",
            color = TextMutedSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardMetricBox(
                modifier = Modifier.weight(1f),
                title = "Motion Alerts",
                count = motionEventsCount.toString(),
                icon = Icons.Default.History,
                accentColor = if (motionEventsCount > 0) StatusLiveRed else TextWhitePrimary,
                onClick = onNavigateMotionHistory
            )

            DashboardMetricBox(
                modifier = Modifier.weight(1f),
                title = "Recordings",
                count = "$recordingsCount clips",
                icon = Icons.Default.Folder,
                accentColor = CyanNeon,
                onClick = onNavigateRecordings
            )

            DashboardMetricBox(
                modifier = Modifier.weight(1f),
                title = "Free Storage",
                count = storageStats.availableFormatted,
                icon = Icons.Default.PhoneAndroid,
                accentColor = StatusConnectedGreen,
                onClick = onNavigateSettings
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Paired Cameras Card
        if (pairedCameras.isNotEmpty()) {
            Text(
                text = "PAIRED CAMERAS (${pairedCameras.size})",
                color = TextMutedSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            pairedCameras.take(2).forEach { cam ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkNavyCard)
                        .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
                        .clickable { onNavigateMonitorMode() }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkNavySurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(cam.name, color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${cam.ipAddress}:${cam.port}", color = TextMutedSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Text(
                        text = "Connect >",
                        color = CyanNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NavigationPill(
                modifier = Modifier.weight(1f),
                label = "Cameras",
                icon = Icons.Default.Devices,
                onClick = onNavigateDeviceManagement
            )
            NavigationPill(
                modifier = Modifier.weight(1f),
                label = "Privacy",
                icon = Icons.Default.Info,
                onClick = onNavigatePrivacy
            )
        }
    }
}

@Composable
private fun HeroOperatingCard(
    title: String,
    subtitle: String,
    description: String,
    actionLabel: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, DarkNavyBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkNavyCardElevated)
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextWhitePrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "\"$subtitle\"",
                color = CyanNeon,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = TextMutedSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    color = DarkNavyBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = DarkNavyBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardMetricBox(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavyCard)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                color = TextWhitePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = title,
                color = TextMutedSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NavigationPill(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkNavyCardElevated)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanNeon,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = TextWhitePrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
