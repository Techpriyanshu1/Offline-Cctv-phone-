package com.example.streaming

import com.example.networking.CameraTelemetry
import com.example.networking.ControlCommand
import com.example.networking.HandshakeRequest
import com.example.networking.HandshakeResponse
import com.example.networking.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class ControlSocketServer(
    private val port: Int = Protocol.CONTROL_PORT,
    var expectedPin: String = "",
    var cameraName: String = "TwinGuard Node",
    private val onTorchCommand: (Boolean) -> Unit = {},
    private val onSwitchCameraCommand: () -> Unit = {},
    private val onStartRecordingCommand: () -> Unit = {},
    private val onStopRecordingCommand: () -> Unit = {},
    private val onResolutionCommand: (String) -> Unit = {},
    private val onZoomCommand: (Float) -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val authenticatedClients = CopyOnWriteArrayList<ClientWriter>()

    private class ClientWriter(val socket: Socket, val writer: PrintWriter)

    fun start() {
        if (serverSocket != null) return
        acceptJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    scope.launch { handleClient(client) }
                }
            } catch (e: Exception) {
                // Server closed
            }
        }
    }

    private fun handleClient(socket: Socket) {
        var clientWriter: ClientWriter? = null
        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Step 1: Handshake
            val handshakeLine = reader.readLine() ?: return
            val request = HandshakeRequest.fromJson(handshakeLine)

            if (request == null || (expectedPin.isNotEmpty() && request.pairingPin != expectedPin)) {
                val reject = HandshakeResponse(false, "Invalid Security PIN").toJson()
                writer.println(reject)
                socket.close()
                return
            }

            // Accept
            val accept = HandshakeResponse(true, "Authenticated", sessionToken = "valid", cameraName = cameraName).toJson()
            writer.println(accept)

            clientWriter = ClientWriter(socket, writer)
            authenticatedClients.add(clientWriter)

            // Step 2: Command loop
            while (socket.isConnected && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val command = ControlCommand.fromJson(line) ?: continue

                when (command.action) {
                    Protocol.ACTION_SET_TORCH -> {
                        command.booleanValue?.let { onTorchCommand(it) }
                    }
                    Protocol.ACTION_SWITCH_CAMERA -> {
                        onSwitchCameraCommand()
                    }
                    Protocol.ACTION_START_RECORDING -> {
                        onStartRecordingCommand()
                    }
                    Protocol.ACTION_STOP_RECORDING -> {
                        onStopRecordingCommand()
                    }
                    Protocol.ACTION_SET_RESOLUTION -> {
                        command.stringValue?.let { onResolutionCommand(it) }
                    }
                    Protocol.ACTION_SET_ZOOM -> {
                        command.floatValue?.let { onZoomCommand(it) }
                    }
                    Protocol.ACTION_PING -> {
                        val pingTs = command.pingTimestamp ?: System.currentTimeMillis()
                        val pong = ControlCommand(Protocol.ACTION_PONG, pingTimestamp = pingTs).toJson()
                        writer.println(pong)
                    }
                    Protocol.ACTION_DISCONNECT -> {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // Client disconnected
        } finally {
            clientWriter?.let { authenticatedClients.remove(it) }
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    fun broadcastTelemetry(telemetry: CameraTelemetry) {
        val json = telemetry.toJson()
        for (client in authenticatedClients) {
            scope.launch {
                try {
                    client.writer.println(json)
                } catch (e: Exception) {
                    authenticatedClients.remove(client)
                }
            }
        }
    }

    fun broadcastMotionAlert() {
        val alert = ControlCommand(Protocol.ACTION_MOTION_ALERT).toJson()
        for (client in authenticatedClients) {
            scope.launch {
                try {
                    client.writer.println(alert)
                } catch (e: Exception) {
                    authenticatedClients.remove(client)
                }
            }
        }
    }

    fun stop() {
        acceptJob?.cancel()
        for (client in authenticatedClients) {
            try { client.socket.close() } catch (e: Exception) {}
        }
        authenticatedClients.clear()
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
    }
}
