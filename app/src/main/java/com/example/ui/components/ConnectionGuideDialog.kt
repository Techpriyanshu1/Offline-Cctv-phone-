package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.StatusConnectedGreen
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun ConnectionGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "How to Connect (Offline)",
                        color = TextWhitePrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMutedSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "TwinGuard works completely offline without internet or mobile data. Choose any of these 3 easy local connection methods:",
                    color = TextMutedSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // METHOD 1: Phone Hotspot (RECOMMENDED)
                MethodCard(
                    badge = "RECOMMENDED • ZERO DATA",
                    icon = Icons.Default.WifiTethering,
                    title = "Method 1: Phone Hotspot (Easiest)",
                    step1 = "1. Turn ON 'Portable Hotspot' on Phone A (Camera). You can turn OFF mobile data — no SIM/internet needed!",
                    step2 = "2. On Phone B (Monitor), turn ON Wi-Fi and connect to Phone A's hotspot.",
                    step3 = "3. Open TwinGuard: Start 'Camera Mode' on Phone A. Note the IP & 6-digit PIN shown on screen.",
                    step4 = "4. On Phone B, open 'Monitor Mode', enter the IP & PIN, and tap 'Connect'!"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // METHOD 2: Same Local Wi-Fi
                MethodCard(
                    badge = "OFFLINE ROUTER / HOME WI-FI",
                    icon = Icons.Default.Router,
                    title = "Method 2: Same Local Wi-Fi Network",
                    step1 = "1. Connect both phones to the same Wi-Fi network (public internet is not used).",
                    step2 = "2. Open TwinGuard on Phone A and tap 'Camera Mode'.",
                    step3 = "3. Open TwinGuard on Phone B and tap 'Monitor Mode'.",
                    step4 = "4. Phone B will auto-discover the camera in 'Nearby Discovery', or enter the IP & PIN."
                )

                Spacer(modifier = Modifier.height(12.dp))

                // METHOD 3: Direct Wi-Fi Direct (P2P)
                MethodCard(
                    badge = "DIRECT DEVICE-TO-DEVICE",
                    icon = Icons.Default.Wifi,
                    title = "Method 3: Wi-Fi Direct (P2P)",
                    step1 = "1. Turn ON Wi-Fi on both phones (no Wi-Fi router needed).",
                    step2 = "2. On Phone A, start 'Camera Mode' (creates direct P2P access point).",
                    step3 = "3. On Phone B, go to 'Monitor Mode' -> 'Nearby Discovery' tab.",
                    step4 = "4. Tap the detected camera or connect using default IP 192.168.49.1."
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusConnectedGreen.copy(alpha = 0.15f))
                        .border(1.dp, StatusConnectedGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🔒 Security Note: All video streams require the 6-digit PIN displayed on the Camera phone screen. No unauthorized devices can watch.",
                        color = StatusConnectedGreen,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_close_guide"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkNavyBackground)
            ) {
                Text("Got It, Let's Connect!", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkNavySurface
    )
}

@Composable
private fun MethodCard(
    badge: String,
    icon: ImageVector,
    title: String,
    step1: String,
    step2: String,
    step3: String,
    step4: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavyCard)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyanNeon.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    color = CyanNeon,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(imageVector = icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(text = title, color = TextWhitePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(6.dp))

        StepLine(step1)
        StepLine(step2)
        StepLine(step3)
        StepLine(step4)
    }
}

@Composable
private fun StepLine(text: String) {
    Text(
        text = text,
        color = TextMutedSecondary,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
