package com.example.ui.screens.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.networking.NetworkUtils
import com.example.storage.FileManager
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
fun AppSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fileManager = remember { FileManager(context) }
    var storageStats by remember { mutableStateOf(fileManager.getStorageStats()) }

    var sensitivitySlider by remember { mutableFloatStateOf(6.0f) }
    var autoRecordEnabled by remember { mutableStateOf(true) }
    var chimeAlertEnabled by remember { mutableStateOf(true) }
    var cooldownIndex by remember { mutableStateOf(1) } // 0: 3s, 1: 5s, 2: 10s, 3: 30s
    val cooldownOptions = listOf("3s", "5s", "10s", "30s")

    val localIp = remember { NetworkUtils.getLocalIpAddress() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .testTag("screen_app_settings")
    ) {
        CctvTopAppBar(
            title = "App Settings",
            subtitle = "Security & Storage Diagnostics",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // MOTION DETECTION SETTINGS SECTION
            SettingsSectionHeader(title = "MOTION DETECTION CONFIGURATION")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkNavyCard)
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sensitivity Threshold", color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = when {
                                sensitivitySlider <= 3.5f -> "High (Detects minor movement: 2.5%)"
                                sensitivitySlider <= 8f -> "Medium (Standard human motion: 6.0%)"
                                else -> "Low (Large motion only: 12.0%)"
                            },
                            color = CyanNeon,
                            fontSize = 12.sp
                        )
                    }
                }

                Slider(
                    value = sensitivitySlider,
                    onValueChange = { sensitivitySlider = it },
                    valueRange = 2.0f..15.0f,
                    colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon),
                    modifier = Modifier.testTag("slider_motion_sensitivity")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-record toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Record on Motion", color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically records a 10-second MP4 clip when motion is detected.", color = TextMutedSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoRecordEnabled,
                        onCheckedChange = { autoRecordEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = DarkNavySurface),
                        modifier = Modifier.testTag("switch_auto_record")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alert Chime toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audible Alert Tone & Vibration", color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Plays offline warning chime and haptic feedback on motion trigger.", color = TextMutedSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = chimeAlertEnabled,
                        onCheckedChange = { chimeAlertEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = DarkNavySurface),
                        modifier = Modifier.testTag("switch_alert_chime")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cooldown Selector
                Text("Alert Cooldown Timer", color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cooldownOptions.forEachIndexed { index, option ->
                        val isSelected = cooldownIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyanNeon else DarkNavySurface)
                                .border(1.dp, if (isSelected) CyanNeon else DarkNavyBorder, RoundedCornerShape(8.dp))
                                .clickable { cooldownIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) DarkNavyBackground else TextWhitePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // STORAGE MANAGEMENT SECTION
            SettingsSectionHeader(title = "LOCAL STORAGE MANAGER")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkNavyCard)
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SdStorage, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("TwinGuard Storage Usage", color = TextWhitePrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("App recordings: ${storageStats.appUsedFormatted} of ${storageStats.totalFormatted}", color = TextMutedSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Free Storage Available:", color = TextMutedSecondary, fontSize = 13.sp)
                    Text(storageStats.availableFormatted, color = StatusConnectedGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                if (storageStats.isLowStorage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusLiveRed.copy(alpha = 0.2f))
                            .border(1.dp, StatusLiveRed, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Warning: Device storage is below 500 MB. Clear recordings to avoid stopping video.",
                            color = StatusLiveRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // NETWORK & PROTOCOL SECTION
            SettingsSectionHeader(title = "LOCAL NETWORK DIAGNOSTICS")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkNavyCard)
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                DiagnosticRow(label = "Local IP Address", value = localIp)
                DiagnosticRow(label = "Video Streaming Port", value = "8888 (TCP / MJPEG)")
                DiagnosticRow(label = "Control Telemetry Port", value = "8889 (TCP / JSON)")
                DiagnosticRow(label = "Two-Way Audio Port", value = "8890 (TCP / PCM 16kHz)")
                DiagnosticRow(label = "P2P Architecture", value = "Direct Device-to-Device")
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextMutedSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMutedSecondary, fontSize = 13.sp)
        Text(value, color = TextWhitePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
