package com.example.streaming

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

class VideoStreamClient {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var streamJob: Job? = null
    private var activeSocket: Socket? = null

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private val _fps = MutableStateFlow(0.0)
    val fps: StateFlow<Double> = _fps.asStateFlow()

    private val _bitrateKbps = MutableStateFlow(0)
    val bitrateKbps: StateFlow<Int> = _bitrateKbps.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val host: String, val port: Int) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val frameCount = AtomicInteger(0)
    private var bytesReceivedInSecond = AtomicInteger(0)
    private var lastMetricTime = System.currentTimeMillis()

    fun connect(host: String, port: Int = 8888) {
        disconnect()
        _connectionState.value = ConnectionState.Connecting

        streamJob = scope.launch {
            while (isActive) {
                try {
                    val socket = Socket()
                    activeSocket = socket
                    socket.tcpNoDelay = true
                    socket.soTimeout = 8000
                    socket.connect(InetSocketAddress(host, port), 5000)

                    _connectionState.value = ConnectionState.Connected(host, port)
                    val input = BufferedInputStream(socket.getInputStream(), 65536)

                    val buffer = ByteArray(16384)
                    val frameBuffer = ByteArrayOutputStream(131072)
                    var prevByte = -1
                    var inJpeg = false

                    while (isActive) {
                        val count = input.read(buffer)
                        if (count == -1) break

                        bytesReceivedInSecond.addAndGet(count)

                        for (i in 0 until count) {
                            val currByte = buffer[i].toInt() and 0xFF

                            if (!inJpeg) {
                                // Search for JPEG SOI (0xFF, 0xD8)
                                if (prevByte == 0xFF && currByte == 0xD8) {
                                    inJpeg = true
                                    frameBuffer.reset()
                                    frameBuffer.write(0xFF)
                                    frameBuffer.write(0xD8)
                                }
                            } else {
                                frameBuffer.write(currByte)
                                // Search for JPEG EOI (0xFF, 0xD9)
                                if (prevByte == 0xFF && currByte == 0xD9) {
                                    inJpeg = false
                                    val jpegData = frameBuffer.toByteArray()
                                    val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
                                    if (bitmap != null) {
                                        _latestFrame.value = bitmap
                                        frameCount.incrementAndGet()
                                    }
                                }
                            }
                            prevByte = currByte
                        }

                        // Periodic calculation of metrics
                        val now = System.currentTimeMillis()
                        if (now - lastMetricTime >= 1000) {
                            val elapsedSec = (now - lastMetricTime) / 1000.0
                            _fps.value = frameCount.getAndSet(0) / elapsedSec
                            val kbps = ((bytesReceivedInSecond.getAndSet(0) * 8) / (1000 * elapsedSec)).toInt()
                            _bitrateKbps.value = kbps
                            lastMetricTime = now
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        _connectionState.value = ConnectionState.Error("Connection lost: ${e.message}. Reconnecting...")
                        _latestFrame.value = null
                        kotlinx.coroutines.delay(2000)
                    }
                }
            }
        }
    }

    fun disconnect() {
        streamJob?.cancel()
        streamJob = null
        try {
            activeSocket?.close()
        } catch (e: Exception) {}
        activeSocket = null
        _connectionState.value = ConnectionState.Disconnected
        _latestFrame.value = null
        _fps.value = 0.0
        _bitrateKbps.value = 0
    }
}
