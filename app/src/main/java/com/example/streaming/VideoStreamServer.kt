package com.example.streaming

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class VideoStreamServer(
    private val port: Int = 8888,
    var expectedPin: String = ""
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val connectedClients = ConcurrentHashMap<String, ClientSession>()
    private val _activeViewers = MutableStateFlow(0)
    val activeViewers: StateFlow<Int> = _activeViewers.asStateFlow()

    private val _fps = MutableStateFlow(0.0)
    val fps: StateFlow<Double> = _fps.asStateFlow()

    private val frameCounter = AtomicInteger(0)
    private var lastFpsCalculationTime = System.currentTimeMillis()

    class ClientSession(
        val socket: Socket,
        val outputStream: OutputStream,
        val remoteAddress: String,
        val isWriting: AtomicInteger = AtomicInteger(0)
    )

    fun start() {
        if (serverSocket != null) return
        acceptJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    client.tcpNoDelay = true
                    client.sendBufferSize = 65536
                    val clientAddr = client.remoteSocketAddress.toString()
                    val out = client.getOutputStream()

                    // Send HTTP multipart header
                    val header = ("HTTP/1.0 200 OK\r\n" +
                            "Server: TwinGuardCCTV\r\n" +
                            "Connection: close\r\n" +
                            "Max-Age: 0\r\n" +
                            "Expires: 0\r\n" +
                            "Cache-Control: no-cache, private\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: multipart/x-mixed-replace; boundary=--TwinGuardBoundary\r\n\r\n").toByteArray()
                    out.write(header)
                    out.flush()

                    val session = ClientSession(client, out, clientAddr)
                    connectedClients[clientAddr] = session
                    _activeViewers.value = connectedClients.size
                }
            } catch (e: Exception) {
                // Server closed
            }
        }
    }

    fun broadcastFrame(jpegBytes: ByteArray) {
        if (connectedClients.isEmpty()) return

        val count = frameCounter.incrementAndGet()
        val now = System.currentTimeMillis()
        if (now - lastFpsCalculationTime >= 1000) {
            val elapsedSec = (now - lastFpsCalculationTime) / 1000.0
            _fps.value = count / elapsedSec
            frameCounter.set(0)
            lastFpsCalculationTime = now
        }

        val frameHeader = ("--TwinGuardBoundary\r\n" +
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: ${jpegBytes.size}\r\n\r\n").toByteArray()

        val deadClients = mutableListOf<String>()

        connectedClients.forEach { (id, session) ->
            // Drop frame if previous write is still pending to ensure zero latency
            if (session.isWriting.compareAndSet(0, 1)) {
                scope.launch {
                    try {
                        session.outputStream.write(frameHeader)
                        session.outputStream.write(jpegBytes)
                        session.outputStream.write("\r\n".toByteArray())
                        session.outputStream.flush()
                    } catch (e: Exception) {
                        deadClients.add(id)
                        try { session.socket.close() } catch (ignored: Exception) {}
                    } finally {
                        session.isWriting.set(0)
                    }
                }
            }
        }

        if (deadClients.isNotEmpty()) {
            deadClients.forEach { connectedClients.remove(it) }
            _activeViewers.value = connectedClients.size
        }
    }

    fun stop() {
        acceptJob?.cancel()
        connectedClients.values.forEach {
            try { it.socket.close() } catch (e: Exception) {}
        }
        connectedClients.clear()
        _activeViewers.value = 0
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
    }

    companion object {
        fun imageProxyToJpeg(imageProxy: ImageProxy, quality: Int = 75): ByteArray? {
            return try {
                val image = imageProxy.image ?: return null
                val planes = image.planes
                val yBuffer = planes[0].buffer
                val uBuffer = planes[1].buffer
                val vBuffer = planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(
                    nv21,
                    android.graphics.ImageFormat.NV21,
                    imageProxy.width,
                    imageProxy.height,
                    null
                )
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(
                    Rect(0, 0, imageProxy.width, imageProxy.height),
                    quality,
                    out
                )
                out.toByteArray()
            } catch (e: Exception) {
                null
            }
        }
    }
}
