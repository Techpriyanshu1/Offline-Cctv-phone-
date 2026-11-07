package com.example.ui.screens.monitor

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.networking.NetworkServiceDiscovery
import com.example.networking.NetworkUtils
import com.example.networking.WifiP2pHelper
import com.example.storage.TwinGuardDatabase
import com.example.ui.components.CctvTopAppBar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardElevated
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.StatusConnectedGreen
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun MonitorModeScreen(
    onConnectToCamera: (ip: String, port: Int, pin: String, name: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { TwinGuardDatabase.getDatabase(context) }
    val pairedCameras by db.pairedCamerasDao().getAllPairedCameras().collectAsState(initial = emptyList())

    val p2pHelper = remember { WifiP2pHelper(context) }
    val nsd = remember { NetworkServiceDiscovery(context) }

    val discoveredP2pPeers by p2pHelper.discoveredPeers.collectAsState()
    val discoveredServices by nsd.discoveredServices.collectAsState()
    val p2pStatus by p2pHelper.statusMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Direct IP & PIN, 1: Nearby Discovery
    var ipAddressInput by remember { mutableStateOf("192.168.49.1") }
    var portInput by remember { mutableStateOf("8888") }
    var pinInput by remember { mutableStateOf("") }
    var cameraNameInput by remember { mutableStateOf("TwinGuard Camera") }

    var isScanning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        p2pHelper.init()
        nsd.startDiscovery()
        onDispose {
            p2pHelper.cleanup()
            nsd.stop()
        }
    }

    var showGuideDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    if (showGuideDialog) {
        com.example.ui.components.ConnectionGuideDialog(onDismiss = { showGuideDialog = false })
    }

    if (showQrScannerDialog) {
        com.example.pairing.QrScannerDialog(
            onDismiss = { showQrScannerDialog = false },
            onQrScanned = { ip, port, pin, name ->
                showQrScannerDialog = false
                onConnectToCamera(ip, port, pin, name)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .testTag("screen_monitor_mode")
    ) {
        CctvTopAppBar(
            title = "Monitor Mode",
            subtitle = "Connect to Camera Node",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.testTag("btn_monitor_guide")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Info,
                        contentDescription = "Connection Guide",
                        tint = CyanNeon
                    )
                }
            }
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkNavySurface,
            contentColor = CyanNeon
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Direct PIN / IP", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    isScanning = true
                    p2pHelper.discoverPeers { isScanning = false }
                },
                text = { Text("Nearby Discovery", fontWeight = FontWeight.Bold) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            if (selectedTab == 0) {
                // Direct IP & PIN Entry
                CardDirectPairing(
                    ip = ipAddressInput,
                    onIpChange = { ipAddressInput = it },
                    port = portInput,
                    onPortChange = { portInput = it },
                    pin = pinInput,
                    onPinChange = { pinInput = it },
                    cameraName = cameraNameInput,
                    onCameraNameChange = { cameraNameInput = it },
                    onScanQr = { showQrScannerDialog = true },
                    onConnect = {
                        val port = portInput.toIntOrNull() ?: 8888
                        onConnectToCamera(ipAddressInput.trim(), port, pinInput.trim(), cameraNameInput.trim())
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Paired Cameras quick connect
                if (pairedCameras.isNotEmpty()) {
                    Text(
                        text = "SAVED CAMERAS",
                        color = TextMutedSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    pairedCameras.forEach { cam ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkNavyCard)
                                .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    onConnectToCamera(cam.ipAddress, cam.port, cam.pairingPin, cam.name)
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cam.name, color = TextWhitePrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("${cam.ipAddress}:${cam.port}", color = CyanNeon, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Button(
                                onClick = { onConnectToCamera(cam.ipAddress, cam.port, cam.pairingPin, cam.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCardElevated, contentColor = CyanNeon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Connect")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Nearby Wi-Fi Direct and mDNS Discovery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DISCOVERED CAMERAS",
                            color = TextMutedSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = p2pStatus,
                            color = StatusConnectedGreen,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            isScanning = true
                            p2pHelper.discoverPeers { isScanning = false }
                        },
                        modifier = Modifier.testTag("btn_refresh_discovery")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CyanNeon, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = CyanNeon)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (discoveredServices.isEmpty() && discoveredP2pPeers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkNavyCard)
                            .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = TextMutedSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Searching local Wi-Fi & Wi-Fi Direct...", color = TextWhitePrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Make sure Camera phone has Camera Mode active.", color = TextMutedSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { selectedTab = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCardElevated, contentColor = CyanNeon)
                            ) {
                                Text("Switch to Direct IP / PIN")
                            }
                        }
                    }
                } else {
                    discoveredServices.forEach { service ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkNavyCard)
                                .border(1.dp, CyanNeon, RoundedCornerShape(12.dp))
                                .clickable {
                                    ipAddressInput = service.ipAddress
                                    portInput = service.port.toString()
                                    cameraNameInput = service.name
                                    selectedTab = 0
                                }
                                .padding(14.dp),
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
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(service.name, color = TextWhitePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("${service.ipAddress}:${service.port}", color = StatusConnectedGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text("Select >", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardDirectPairing(
    ip: String,
    onIpChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    cameraName: String,
    onCameraNameChange: (String) -> Unit,
    onScanQr: () -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkNavyCard)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        // Fast QR Scan Button
        Button(
            onClick = onScanQr,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_scan_camera_qr"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkNavyBackground)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Camera QR Code (Instant)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(DarkNavyBorder))
            Text(
                text = " OR ENTER MANUALLY ",
                color = TextMutedSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(DarkNavyBorder))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Direct IP & Security PIN",
            color = TextWhitePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = ip,
            onValueChange = onIpChange,
            label = { Text("Camera IP Address") },
            placeholder = { Text("e.g. 192.168.49.1") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_camera_ip"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = DarkNavyBorder,
                focusedTextColor = TextWhitePrimary,
                unfocusedTextColor = TextWhitePrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) onPinChange(it) },
                label = { Text("6-Digit PIN") },
                placeholder = { Text("123456") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("input_camera_pin"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = DarkNavyBorder,
                    focusedTextColor = TextWhitePrimary,
                    unfocusedTextColor = TextWhitePrimary
                )
            )

            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(0.7f)
                    .testTag("input_camera_port"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = DarkNavyBorder,
                    focusedTextColor = TextWhitePrimary,
                    unfocusedTextColor = TextWhitePrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_connect_camera_feed"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkNavyBackground)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect & Start Monitoring", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
