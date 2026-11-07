package com.example.streaming

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class AudioStreamer(private val isServer: Boolean) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    val isRecordingMic = AtomicBoolean(false)
    val isPlayingAudio = AtomicBoolean(true)

    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(2048)

    fun startServer(port: Int = 8890) {
        stop()
        job = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                val client = serverSocket?.accept() ?: return@launch
                socket = client
                handleAudioCommunication(client.getInputStream(), client.getOutputStream())
            } catch (e: Exception) {
                // Server closed
            }
        }
    }

    fun startClient(host: String, port: Int = 8890) {
        stop()
        job = scope.launch {
            try {
                val s = Socket(host, port)
                socket = s
                handleAudioCommunication(s.getInputStream(), s.getOutputStream())
            } catch (e: Exception) {
                // Connection failed
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleAudioCommunication(inputStream: InputStream, outputStream: OutputStream) {
        // Player setup
        val trackBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            trackBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.play()

        // Incoming audio reader loop
        val readJob = scope.launch {
            val audioBuffer = ByteArray(1024)
            while (isActive) {
                val read = inputStream.read(audioBuffer)
                if (read == -1) break
                if (isPlayingAudio.get()) {
                    audioTrack?.write(audioBuffer, 0, read)
                }
            }
        }

        // Outgoing audio recording loop
        val writeJob = scope.launch {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                audioRecord?.startRecording()
                val recordBuffer = ByteArray(1024)

                while (isActive) {
                    val read = audioRecord?.read(recordBuffer, 0, recordBuffer.size) ?: -1
                    if (read > 0) {
                        if (isRecordingMic.get()) {
                            outputStream.write(recordBuffer, 0, read)
                            outputStream.flush()
                        } else {
                            // Send silence or skip
                            kotlinx.coroutines.delay(20)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setMicEnabled(enabled: Boolean) {
        isRecordingMic.set(enabled)
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        isPlayingAudio.set(enabled)
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null

        try {
            socket?.close()
        } catch (e: Exception) {}
        socket = null

        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
    }
}
