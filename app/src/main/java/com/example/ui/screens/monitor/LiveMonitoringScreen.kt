package com.example.ui.screens.monitor

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monitor.MonitorForegroundService
import com.example.monitor.MonitorManager
import com.example.streaming.ControlSocketClient
import com.example.streaming.VideoStreamClient
import com.example.ui.components.LiveBadge
import com.example.ui.components.PinchZoomBox
import com.example.ui.components.SecurityMetricChip
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
fun LiveMonitoringScreen(
    cameraIp: String,
    cameraPort: Int,
    cameraPin: String,
    cameraName: String,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    val monitorManager = remember { MonitorManager(context) }

    val latestFrame by monitorManager.videoClient.latestFrame.collectAsState()
    val videoFps by monitorManager.videoClient.fps.collectAsState()
    val bitrateKbps by monitorManager.videoClient.bitrateKbps.collectAsState()
    val connectionState by monitorManager.videoClient.connectionState.collectAsState()

    val telemetry by monitorManager.controlClient.telemetry.collectAsState()
    val latencyMs by monitorManager.controlClient.latencyMs.collectAsState()
    val authStatus by monitorManager.controlClient.authStatus.collectAsState()

    val isLocalRecording by monitorManager.isLocalRecording.collectAsState()
    val isIntercomActive by monitorManager.isIntercomActive.collectAsState()
    val isSpeakerMuted by monitorManager.isSpeakerMuted.collectAsState()

    var showDisconnectDialog by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    BackHandler {
        showDisconnectDialog = true
    }

    DisposableEffect(cameraIp, cameraPort, cameraPin) {
        MonitorForegroundService.start(context)
        monitorManager.connectToCamera(cameraIp, cameraPort, cameraPin, cameraName)

        onDispose {
            MonitorForegroundService.stop(context)
            monitorManager.disconnect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
            }
            .testTag("screen_live_monitoring")
    ) {
        // Video Viewport (with Pinch-to-Zoom & Pan)
        PinchZoomBox(modifier = Modifier.fillMaxSize()) {
            if (latestFrame != null) {
                Image(
                    bitmap = latestFrame!!.asImageBitmap(),
                    contentDescription = "Live CCTV Camera Feed",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanNeon)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (connectionState) {
                                is VideoStreamClient.ConnectionState.Connecting -> "Connecting to $cameraIp..."
                                is VideoStreamClient.ConnectionState.Error -> "Connection dropped. Reconnecting..."
                                else -> "Waiting for video stream..."
                            },
                            color = TextWhitePrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Direct Offline P2P • Zero Internet",
                            color = TextMutedSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Top HUD Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC090D16))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showDisconnectDialog = true },
                            modifier = Modifier.size(36.dp).testTag("btn_monitor_back")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(cameraName, color = TextWhitePrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("$cameraIp:$cameraPort", color = CyanNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveBadge(isRecording = telemetry.isRecording || isLocalRecording)
                        Spacer(modifier = Modifier.width(6.dp))

                        // Picture in Picture button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    val activity = context as? Activity
                                    activity?.enterPictureInPictureMode()
                                },
                                modifier = Modifier.size(36.dp).testTag("btn_pip_mode")
                            ) {
                                Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = TextWhitePrimary)
                            }
                        }

                        // Fullscreen toggle
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.size(36.dp).testTag("btn_fullscreen_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = TextWhitePrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Telemetry Metrics Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecurityMetricChip(label = "LATENCY", value = "${latencyMs}ms")
                    SecurityMetricChip(label = "FPS", value = String.format("%.1f", videoFps))
                    SecurityMetricChip(label = "RATE", value = "${bitrateKbps}kbps")
                    SecurityMetricChip(
                        label = "BAT",
                        value = "${telemetry.batteryPct}%${if (telemetry.isCharging) "⚡" else ""}",
                        accentColor = if (telemetry.batteryPct > 20) StatusConnectedGreen else StatusLiveRed
                    )
                }
            }
        }

        // Bottom Controls Toolbar Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC090D16))
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Row of Remote Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remote Torch toggle
                    IconButton(
                        onClick = { monitorManager.toggleRemoteTorch() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkNavyCard)
                            .testTag("btn_remote_torch")
                    ) {
                        Icon(
                            imageVector = if (telemetry.torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (telemetry.torchOn) Color.Yellow else TextMutedSecondary
                        )
                    }

                    // Remote Camera Lens Switch
                    IconButton(
                        onClick = { monitorManager.switchRemoteCamera() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkNavyCard)
                            .testTag("btn_remote_switch_camera")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera",
                            tint = CyanNeon
                        )
                    }

                    // Snapshot / Screenshot
                    IconButton(
                        onClick = {
                            val shot = monitorManager.takeScreenshot()
                            if (shot != null) {
                                Toast.makeText(context, "Screenshot saved to TwinGuard/Screenshots/", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkNavyCard)
                            .testTag("btn_monitor_snapshot")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Screenshot", tint = CyanNeon)
                    }

                    // Local Record (MP4 on Monitor phone)
                    IconButton(
                        onClick = { monitorManager.toggleLocalMonitorRecording() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isLocalRecording) StatusLiveRed else DarkNavyCard)
                            .border(2.dp, if (isLocalRecording) Color.White else StatusLiveRed, CircleShape)
                            .testTag("btn_monitor_local_record")
                    ) {
                        Icon(
                            imageVector = if (isLocalRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = "Local Record",
                            tint = if (isLocalRecording) Color.White else StatusLiveRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Speaker Mute/Unmute
                    IconButton(
                        onClick = { monitorManager.toggleSpeakerMute() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkNavyCard)
                            .testTag("btn_monitor_mute_speaker")
                    ) {
                        Icon(
                            imageVector = if (isSpeakerMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute Speaker",
                            tint = if (isSpeakerMuted) TextMutedSecondary else CyanNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Intercom Push-to-Talk Button
                Button(
                    onClick = {
                        val active = !isIntercomActive
                        monitorManager.setIntercomTalking(active)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_monitor_intercom"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIntercomActive) StatusConnectedGreen else DarkNavyCard,
                        contentColor = if (isIntercomActive) DarkNavyBackground else CyanNeon
                    )
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isIntercomActive) "Intercom Active: TALKING..." else "Hold to Talk (Intercom)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Disconnect Confirmation Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect from Camera?", color = TextWhitePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Live video monitoring and telemetry will be stopped.", color = TextMutedSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        MonitorForegroundService.stop(context)
                        monitorManager.disconnect()
                        onDisconnect()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusLiveRed, contentColor = Color.White)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Stay Connected", color = CyanNeon)
                }
            },
            containerColor = DarkNavySurface
        )
    }
}
