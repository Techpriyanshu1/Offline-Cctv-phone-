package com.example.streaming

import com.example.networking.CameraTelemetry
import com.example.networking.ControlCommand
import com.example.networking.HandshakeRequest
import com.example.networking.HandshakeResponse
import com.example.networking.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class ControlSocketClient {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var clientJob: Job? = null
    private var pingJob: Job? = null
    private var activeSocket: Socket? = null
    private var printWriter: PrintWriter? = null

    private val _telemetry = MutableStateFlow(CameraTelemetry())
    val telemetry: StateFlow<CameraTelemetry> = _telemetry.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _motionAlerts = MutableSharedFlow<Unit>(extraBufferCapacity = 5)
    val motionAlerts: SharedFlow<Unit> = _motionAlerts.asSharedFlow()

    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Idle)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    sealed class AuthStatus {
        object Idle : AuthStatus()
        object Authenticating : AuthStatus()
        data class Authenticated(val cameraName: String) : AuthStatus()
        data class Rejected(val reason: String) : AuthStatus()
        data class Error(val message: String) : AuthStatus()
    }

    fun connect(host: String, pin: String, monitorName: String = "Monitor Phone", port: Int = Protocol.CONTROL_PORT) {
        disconnect()
        _authStatus.value = AuthStatus.Authenticating

        clientJob = scope.launch {
            while (isActive) {
                try {
                    val socket = Socket()
                    activeSocket = socket
                    socket.tcpNoDelay = true
                    socket.soTimeout = 12000
                    socket.connect(InetSocketAddress(host, port), 4000)

                    val writer = PrintWriter(socket.getOutputStream(), true)
                    printWriter = writer
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    // 1. Handshake
                    val req = HandshakeRequest(
                        deviceName = monitorName,
                        role = "MONITOR",
                        pairingPin = pin
                    ).toJson()
                    writer.println(req)

                    val responseLine = reader.readLine() ?: throw Exception("Empty handshake response")
                    val response = HandshakeResponse.fromJson(responseLine)

                    if (response == null || !response.success) {
                        val msg = response?.message ?: "Handshake failed"
                        _authStatus.value = AuthStatus.Rejected(msg)
                        socket.close()
                        return@launch
                    }

                    _authStatus.value = AuthStatus.Authenticated(response.cameraName)

                    // Start ping loop
                    startPingLoop()

                    // 2. Read messages loop
                    while (isActive && socket.isConnected && !socket.isClosed) {
                        val line = reader.readLine() ?: break
                        handleIncomingMessage(line)
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        _authStatus.value = AuthStatus.Error("Connection dropped. Reconnecting...")
                        delay(2000)
                    }
                }
            }
        }
    }

    private fun handleIncomingMessage(line: String) {
        val telemetryObj = CameraTelemetry.fromJson(line)
        if (telemetryObj != null) {
            _telemetry.value = telemetryObj
            return
        }

        val cmd = ControlCommand.fromJson(line)
        if (cmd != null) {
            when (cmd.action) {
                Protocol.ACTION_PONG -> {
                    cmd.pingTimestamp?.let { sentTs ->
                        val roundTrip = System.currentTimeMillis() - sentTs
                        if (roundTrip >= 0) {
                            _latencyMs.value = roundTrip
                        }
                    }
                }
                Protocol.ACTION_MOTION_ALERT -> {
                    _motionAlerts.tryEmit(Unit)
                }
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                sendCommand(ControlCommand(Protocol.ACTION_PING, pingTimestamp = System.currentTimeMillis()))
                delay(1200)
            }
        }
    }

    fun setTorch(enabled: Boolean) {
        sendCommand(ControlCommand(Protocol.ACTION_SET_TORCH, booleanValue = enabled))
    }

    fun switchCamera() {
        sendCommand(ControlCommand(Protocol.ACTION_SWITCH_CAMERA))
    }

    fun startRecording() {
        sendCommand(ControlCommand(Protocol.ACTION_START_RECORDING))
    }

    fun stopRecording() {
        sendCommand(ControlCommand(Protocol.ACTION_STOP_RECORDING))
    }

    fun setResolution(res: String) {
        sendCommand(ControlCommand(Protocol.ACTION_SET_RESOLUTION, stringValue = res))
    }

    fun setZoom(ratio: Float) {
        sendCommand(ControlCommand(Protocol.ACTION_SET_ZOOM, floatValue = ratio))
    }

    fun sendCommand(cmd: ControlCommand) {
        scope.launch {
            try {
                printWriter?.println(cmd.toJson())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        clientJob?.cancel()
        clientJob = null
        try {
            printWriter?.println(ControlCommand(Protocol.ACTION_DISCONNECT).toJson())
        } catch (ignored: Exception) {}
        try {
            activeSocket?.close()
        } catch (e: Exception) {}
        activeSocket = null
        printWriter = null
        _authStatus.value = AuthStatus.Idle
        _latencyMs.value = 0L
    }
}
