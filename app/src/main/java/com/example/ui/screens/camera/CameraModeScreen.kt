package com.example.ui.screens.camera

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.CameraForegroundService
import com.example.camera.CameraManager
import com.example.networking.NetworkUtils
import com.example.ui.components.LiveBadge
import com.example.ui.components.SecurityMetricChip
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardElevated
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.MotionBoxColor
import com.example.ui.theme.StatusConnectedGreen
import com.example.ui.theme.StatusLiveRed
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun CameraModeScreen(
    onExitCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val pairingPin = remember { NetworkUtils.generatePairingPin() }
    val localIp = remember { NetworkUtils.getLocalIpAddress() }

    val cameraManager = remember { CameraManager(context, pairingPin) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showZoomSlider by remember { mutableStateOf(false) }

    val isStreaming by cameraManager.isStreaming.collectAsState()
    val isRecording by cameraManager.isRecording.collectAsState()
    val isTorchOn by cameraManager.isTorchOn.collectAsState()
    val currentLens by cameraManager.currentLens.collectAsState()
    val zoomRatio by cameraManager.zoomRatio.collectAsState()
    val resolution by cameraManager.resolution.collectAsState()
    val batteryPct by cameraManager.batteryPct.collectAsState()
    val isCharging by cameraManager.isCharging.collectAsState()
    val activeClients by cameraManager.videoServer.activeViewers.collectAsState()
    val fps by cameraManager.videoServer.fps.collectAsState()
    val motionResult by cameraManager.motionDetector.motionState.collectAsState()

    BackHandler {
        CameraForegroundService.stop(context)
        cameraManager.release()
        onExitCamera()
    }

    DisposableEffect(Unit) {
        CameraForegroundService.start(context)
        cameraManager.startServers()

        onDispose {
            CameraForegroundService.stop(context)
            cameraManager.release()
        }
    }

    LaunchedEffect(currentLens, resolution) {
        cameraManager.bindToLifecycle(lifecycleOwner, previewView)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("screen_camera_mode")
    ) {
        // CameraX Live Preview View
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    previewView = pv
                    cameraManager.bindToLifecycle(lifecycleOwner, pv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Motion Detection Bounding Box Overlay
        if (motionResult.isMotionDetected && motionResult.boundingBox != null) {
            val box = motionResult.boundingBox!!
            Canvas(modifier = Modifier.fillMaxSize()) {
                val left = box.left * size.width
                val top = box.top * size.height
                val right = box.right * size.width
                val bottom = box.bottom * size.height

                drawRect(
                    color = MotionBoxColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Top Status HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge(isRecording = isRecording)
                    Spacer(modifier = Modifier.width(8.dp))
                    SecurityMetricChip(
                        label = "FPS",
                        value = String.format("%.1f", fps)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    SecurityMetricChip(
                        label = "VIEWERS",
                        value = activeClients.toString(),
                        accentColor = if (activeClients > 0) StatusConnectedGreen else TextMutedSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick PIN / QR Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC000000))
                            .border(1.dp, CyanNeon, RoundedCornerShape(8.dp))
                            .clickable { showPairingDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("btn_camera_show_pin")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Pairing Code",
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PIN: $pairingPin",
                                color = TextWhitePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Close / Stop Camera button
                    IconButton(
                        onClick = {
                            CameraForegroundService.stop(context)
                            cameraManager.release()
                            onExitCamera()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA000000))
                            .testTag("btn_stop_camera_exit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Camera",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-HUD info (IP, battery, resolution, motion state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$localIp:8888 • $resolution",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (motionResult.isMotionDetected) {
                        Text(
                            text = "MOTION DETECTED",
                            color = MotionBoxColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x99000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = "BAT: $batteryPct%${if (isCharging) "⚡" else ""}",
                        color = if (batteryPct > 20) TextWhitePrimary else StatusLiveRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x66000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Floating Zoom Slider (collapsible)
        AnimatedVisibility(
            visible = showZoomSlider,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD0B1220))
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${String.format("%.1f", zoomRatio)}x",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = zoomRatio,
                    onValueChange = { cameraManager.setZoom(it) },
                    valueRange = 1.0f..5.0f,
                    modifier = Modifier.height(140.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = CyanNeon,
                        activeTrackColor = CyanNeon
                    )
                )
            }
        }

        // Bottom Controls Toolbar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tap to Show QR Code Pill Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xEE090D16))
                    .border(1.dp, CyanNeon, RoundedCornerShape(20.dp))
                    .clickable { showPairingDialog = true }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .testTag("btn_camera_qr_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to Show Camera QR Code",
                    color = TextWhitePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD090D16))
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch toggle
                IconButton(
                    onClick = { cameraManager.toggleTorch() },
                    modifier = Modifier.testTag("btn_camera_torch")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) Color.Yellow else TextMutedSecondary
                    )
                }

                // Switch Lens (Front/Back)
                IconButton(
                    onClick = { cameraManager.switchLens() },
                    modifier = Modifier.testTag("btn_camera_switch_lens")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = CyanNeon
                    )
                }

                // RECORD BUTTON (Big Center Button)
                IconButton(
                    onClick = {
                        if (isRecording) {
                            cameraManager.stopRecording()
                        } else {
                            cameraManager.startRecording()
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) StatusLiveRed else DarkNavyCard)
                        .border(2.dp, if (isRecording) Color.White else StatusLiveRed, CircleShape)
                        .testTag("btn_camera_record_toggle")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = "Record",
                        tint = if (isRecording) Color.White else StatusLiveRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Zoom Toggle
                IconButton(
                    onClick = { showZoomSlider = !showZoomSlider },
                    modifier = Modifier.testTag("btn_camera_zoom_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom",
                        tint = if (showZoomSlider) CyanNeon else TextMutedSecondary
                    )
                }

                // Show QR Code Button
                IconButton(
                    onClick = { showPairingDialog = true },
                    modifier = Modifier.testTag("btn_camera_show_qr_bottom")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Pairing QR Code",
                        tint = CyanNeon
                    )
                }

                // Resolution selector
                IconButton(
                    onClick = { showResolutionDialog = true },
                    modifier = Modifier.testTag("btn_camera_resolution")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Resolution Settings",
                        tint = TextMutedSecondary
                    )
                }
            }
        }
    }

    // Pairing Info Dialog
    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = { showPairingDialog = false },
            title = {
                Text("Camera Security Pairing", color = TextWhitePrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Point the Monitor phone's scanner at this QR code to connect instantly without typing:",
                        color = TextMutedSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val qrPayload = NetworkUtils.generateQrPayload(localIp, 8888, pairingPin, "TwinGuard Node")
                        com.example.pairing.QrCodeView(
                            payload = qrPayload,
                            size = 180.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkNavyCard)
                            .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MANUAL PAIRING PIN", color = TextMutedSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pairingPin,
                                color = CyanNeon,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("IP: $localIp • Port: 8888", color = TextWhitePrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Connections are encrypted directly over your local Wi-Fi / Wi-Fi Direct. Zero internet required.",
                        color = StatusConnectedGreen,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPairingDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkNavyBackground)
                ) {
                    Text("Done")
                }
            },
            containerColor = DarkNavySurface
        )
    }

    // Resolution Selector Dialog
    if (showResolutionDialog) {
        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            title = {
                Text("Stream Resolution", color = TextWhitePrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    listOf("1080p (Full HD)", "720p (HD Balanced)", "480p (Low Bandwidth)").forEach { option ->
                        val code = option.take(5).trim()
                        val isSelected = resolution == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkNavyCardElevated else Color.Transparent)
                                .clickable {
                                    cameraManager.setResolution(code)
                                    showResolutionDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(option, color = if (isSelected) CyanNeon else TextWhitePrimary, fontSize = 14.sp)
                            if (isSelected) {
                                Text("ACTIVE", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResolutionDialog = false }) {
                    Text("Close", color = CyanNeon)
                }
            },
            containerColor = DarkNavySurface
        )
    }
}
