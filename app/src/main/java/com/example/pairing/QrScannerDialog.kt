package com.example.pairing

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.motion.AlertPlayer
import com.example.networking.NetworkUtils
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.StatusConnectedGreen
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onQrScanned: (ip: String, port: Int, pin: String, name: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alertPlayer = remember { AlertPlayer(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_sweep"
    )

    DisposableEffect(Unit) {
        onDispose {
            alertPlayer.release()
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("dialog_qr_scanner")
        ) {
            // Live CameraX Viewfinder
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val analyzer = QrCodeAnalyzer { scannedPayload ->
                            alertPlayer.playMotionChime()
                            val parsed = NetworkUtils.parseQrPayload(scannedPayload)
                            if (parsed != null) {
                                onQrScanned(parsed.ip, parsed.port, parsed.pin, parsed.deviceName)
                            } else {
                                // Direct text payload
                                onQrScanned(scannedPayload, 8888, "", "Camera Node")
                            }
                        }

                        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                        try {
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning Reticle & Animated Laser Sweep Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = size.width * 0.70f
                val left = (size.width - boxSize) / 2
                val top = (size.height - boxSize) / 2.2f
                val right = left + boxSize
                val bottom = top + boxSize

                // Draw corner brackets
                val cornerLen = 32.dp.toPx()
                val strokeW = 4.dp.toPx()

                // Top-Left
                drawLine(CyanNeon, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                drawLine(CyanNeon, Offset(left, top), Offset(left, top + cornerLen), strokeW)
                // Top-Right
                drawLine(CyanNeon, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                drawLine(CyanNeon, Offset(right, top), Offset(right, top + cornerLen), strokeW)
                // Bottom-Left
                drawLine(CyanNeon, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                drawLine(CyanNeon, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)
                // Bottom-Right
                drawLine(CyanNeon, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                drawLine(CyanNeon, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

                // Laser Sweep Line
                val laserY = top + (boxSize * laserProgress)
                drawLine(
                    color = CyanNeon.copy(alpha = 0.85f),
                    start = Offset(left + 8.dp.toPx(), laserY),
                    end = Offset(right - 8.dp.toPx(), laserY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Scan Camera QR Code", color = TextWhitePrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Instant Auto-Connect", color = CyanNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            camera?.cameraControl?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchOn) Color.Yellow else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .testTag("btn_close_qr_scanner")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // Bottom Instructions & Quick Direct Fill
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Point camera at the QR code on the Camera phone screen.",
                    color = TextWhitePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Detects IP address, port, and security PIN automatically.",
                    color = TextMutedSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fast Direct Connect / Local IP quick fill button for instant testing
                Button(
                    onClick = {
                        val localIp = NetworkUtils.getLocalIpAddress()
                        onQrScanned(localIp, 8888, "", "Local Camera Node")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = CyanNeon),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_qr_quick_test")
                ) {
                    val localIp = NetworkUtils.getLocalIpAddress()
                    Text("Or Connect Directly to $localIp:8888", fontSize = 12.sp)
                }
            }
        }
    }
}
