package com.example

import com.example.networking.CameraTelemetry
import com.example.networking.ControlCommand
import com.example.networking.HandshakeRequest
import com.example.networking.HandshakeResponse
import com.example.networking.NetworkUtils
import com.example.networking.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testHandshakeRequestSerialization() {
        val req = HandshakeRequest(
            deviceName = "Monitor Tablet",
            role = "MONITOR",
            pairingPin = "482915"
        )
        val json = req.toJson()
        val parsed = HandshakeRequest.fromJson(json)

        assertNotNull(parsed)
        assertEquals("Monitor Tablet", parsed?.deviceName)
        assertEquals("482915", parsed?.pairingPin)
        assertEquals("MONITOR", parsed?.role)
    }

    @Test
    fun testHandshakeResponseSerialization() {
        val resp = HandshakeResponse(
            success = true,
            message = "Authenticated",
            cameraName = "Living Room CCTV"
        )
        val json = resp.toJson()
        val parsed = HandshakeResponse.fromJson(json)

        assertNotNull(parsed)
        assertEquals(true, parsed?.success)
        assertEquals("Living Room CCTV", parsed?.cameraName)
    }

    @Test
    fun testCameraTelemetrySerialization() {
        val telemetry = CameraTelemetry(
            batteryPct = 94,
            isCharging = false,
            isRecording = true,
            torchOn = true,
            currentLens = "BACK",
            fps = 30.0,
            resolution = "1080p",
            storageFreeMb = 48500,
            motionDetected = true,
            clientCount = 2
        )
        val json = telemetry.toJson()
        val parsed = CameraTelemetry.fromJson(json)

        assertNotNull(parsed)
        assertEquals(94, parsed?.batteryPct)
        assertEquals(true, parsed?.isRecording)
        assertEquals(true, parsed?.torchOn)
        assertEquals("1080p", parsed?.resolution)
        assertEquals(true, parsed?.motionDetected)
        assertEquals(2, parsed?.clientCount)
    }

    @Test
    fun testControlCommandSerialization() {
        val cmd = ControlCommand(
            action = Protocol.ACTION_SET_TORCH,
            booleanValue = true
        )
        val json = cmd.toJson()
        val parsed = ControlCommand.fromJson(json)

        assertNotNull(parsed)
        assertEquals(Protocol.ACTION_SET_TORCH, parsed?.action)
        assertEquals(true, parsed?.booleanValue)
    }

    @Test
    fun testNetworkUtilsPinAndQr() {
        val pin = NetworkUtils.generatePairingPin()
        assertEquals(6, pin.length)
        assertTrue(pin.toInt() in 100000..999999)

        val qr = NetworkUtils.generateQrPayload("192.168.49.1", 8888, pin, "Front Door")
        val parsed = NetworkUtils.parseQrPayload(qr)

        assertNotNull(parsed)
        assertEquals("192.168.49.1", parsed?.ip)
        assertEquals(8888, parsed?.port)
        assertEquals(pin, parsed?.pin)
        assertEquals("Front Door", parsed?.deviceName)
    }
}
