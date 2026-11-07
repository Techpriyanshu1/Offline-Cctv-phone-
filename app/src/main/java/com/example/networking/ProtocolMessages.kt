package com.example.networking

import org.json.JSONObject
import java.util.UUID

object Protocol {
    const val VIDEO_PORT = 8888
    const val CONTROL_PORT = 8889
    const val AUDIO_PORT = 8890
    const val PROTOCOL_VERSION = "2.0"

    const val ACTION_HANDSHAKE = "HANDSHAKE"
    const val ACTION_HANDSHAKE_ACK = "HANDSHAKE_ACK"
    const val ACTION_TELEMETRY = "TELEMETRY"
    const val ACTION_SET_TORCH = "SET_TORCH"
    const val ACTION_SWITCH_CAMERA = "SWITCH_CAMERA"
    const val ACTION_START_RECORDING = "START_RECORDING"
    const val ACTION_STOP_RECORDING = "STOP_RECORDING"
    const val ACTION_SET_RESOLUTION = "SET_RESOLUTION"
    const val ACTION_SET_ZOOM = "SET_ZOOM"
    const val ACTION_PING = "PING"
    const val ACTION_PONG = "PONG"
    const val ACTION_DISCONNECT = "DISCONNECT"
    const val ACTION_MOTION_ALERT = "MOTION_ALERT"
}

data class HandshakeRequest(
    val deviceName: String,
    val role: String, // "MONITOR" or "CAMERA"
    val pairingPin: String,
    val clientToken: String = UUID.randomUUID().toString()
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("action", Protocol.ACTION_HANDSHAKE)
            put("deviceName", deviceName)
            put("role", role)
            put("pin", pairingPin)
            put("token", clientToken)
            put("version", Protocol.PROTOCOL_VERSION)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): HandshakeRequest? {
            return try {
                val obj = JSONObject(jsonStr)
                if (obj.optString("action") == Protocol.ACTION_HANDSHAKE) {
                    HandshakeRequest(
                        deviceName = obj.optString("deviceName", "Unknown"),
                        role = obj.optString("role", "MONITOR"),
                        pairingPin = obj.optString("pin", ""),
                        clientToken = obj.optString("token", "")
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class HandshakeResponse(
    val success: Boolean,
    val message: String,
    val sessionToken: String = "",
    val cameraName: String = "TwinGuard Camera"
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("action", Protocol.ACTION_HANDSHAKE_ACK)
            put("success", success)
            put("message", message)
            put("sessionToken", sessionToken)
            put("cameraName", cameraName)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): HandshakeResponse? {
            return try {
                val obj = JSONObject(jsonStr)
                if (obj.optString("action") == Protocol.ACTION_HANDSHAKE_ACK) {
                    HandshakeResponse(
                        success = obj.optBoolean("success", false),
                        message = obj.optString("message", ""),
                        sessionToken = obj.optString("sessionToken", ""),
                        cameraName = obj.optString("cameraName", "TwinGuard Camera")
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class CameraTelemetry(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val isRecording: Boolean = false,
    val torchOn: Boolean = false,
    val currentLens: String = "BACK", // "BACK" or "FRONT"
    val fps: Double = 0.0,
    val resolution: String = "720p",
    val storageFreeMb: Long = 0L,
    val motionDetected: Boolean = false,
    val clientCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("action", Protocol.ACTION_TELEMETRY)
            put("batteryPct", batteryPct)
            put("isCharging", isCharging)
            put("isRecording", isRecording)
            put("torchOn", torchOn)
            put("currentLens", currentLens)
            put("fps", fps)
            put("resolution", resolution)
            put("storageFreeMb", storageFreeMb)
            put("motionDetected", motionDetected)
            put("clientCount", clientCount)
            put("timestamp", timestamp)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): CameraTelemetry? {
            return try {
                val obj = JSONObject(jsonStr)
                if (obj.optString("action") == Protocol.ACTION_TELEMETRY) {
                    CameraTelemetry(
                        batteryPct = obj.optInt("batteryPct", 100),
                        isCharging = obj.optBoolean("isCharging", false),
                        isRecording = obj.optBoolean("isRecording", false),
                        torchOn = obj.optBoolean("torchOn", false),
                        currentLens = obj.optString("currentLens", "BACK"),
                        fps = obj.optDouble("fps", 0.0),
                        resolution = obj.optString("resolution", "720p"),
                        storageFreeMb = obj.optLong("storageFreeMb", 0L),
                        motionDetected = obj.optBoolean("motionDetected", false),
                        clientCount = obj.optInt("clientCount", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ControlCommand(
    val action: String,
    val booleanValue: Boolean? = null,
    val stringValue: String? = null,
    val floatValue: Float? = null,
    val pingTimestamp: Long? = null
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("action", action)
            booleanValue?.let { put("bool", it) }
            stringValue?.let { put("str", it) }
            floatValue?.let { put("num", it.toDouble()) }
            pingTimestamp?.let { put("pingTs", it) }
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): ControlCommand? {
            return try {
                val obj = JSONObject(jsonStr)
                ControlCommand(
                    action = obj.getString("action"),
                    booleanValue = if (obj.has("bool")) obj.getBoolean("bool") else null,
                    stringValue = if (obj.has("str")) obj.getString("str") else null,
                    floatValue = if (obj.has("num")) obj.getDouble("num").toFloat() else null,
                    pingTimestamp = if (obj.has("pingTs")) obj.getLong("pingTs") else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
