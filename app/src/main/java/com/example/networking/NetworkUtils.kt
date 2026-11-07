package com.example.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Collections
import java.util.Locale

object NetworkUtils {

    fun generatePairingPin(): String {
        val random = SecureRandom()
        val num = 100000 + random.nextInt(900000)
        return num.toString()
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First look for Wi-Fi Direct p2p interface
            for (intf in interfaces) {
                if (intf.name.contains("p2p") || intf.name.contains("wlan") || intf.name.contains("ap")) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress ?: ""
                            if (host.isNotEmpty()) return host
                        }
                    }
                }
            }
            // Fallback: any non-loopback IPv4
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty()) return host
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.49.1" // Standard Wi-Fi Direct GO fallback
    }

    fun generateQrPayload(ip: String, port: Int, pin: String, deviceName: String): String {
        return "twinguard://$ip:$port?pin=$pin&name=${deviceName.replace(" ", "%20")}"
    }

    data class QrParsedData(
        val ip: String,
        val port: Int,
        val pin: String,
        val deviceName: String
    )

    fun parseQrPayload(payload: String): QrParsedData? {
        return try {
            if (!payload.startsWith("twinguard://")) return null
            val withoutScheme = payload.removePrefix("twinguard://")
            val parts = withoutScheme.split("?")
            val hostPort = parts[0].split(":")
            val ip = hostPort[0]
            val port = if (hostPort.size > 1) hostPort[1].toIntOrNull() ?: 8888 else 8888

            var pin = ""
            var name = "TwinGuard Camera"
            if (parts.size > 1) {
                val queryParams = parts[1].split("&")
                for (param in queryParams) {
                    val kv = param.split("=")
                    if (kv.size == 2) {
                        when (kv[0]) {
                            "pin" -> pin = kv[1]
                            "name" -> name = kv[1].replace("%20", " ")
                        }
                    }
                }
            }
            QrParsedData(ip, port, pin, name)
        } catch (e: Exception) {
            null
        }
    }
}
