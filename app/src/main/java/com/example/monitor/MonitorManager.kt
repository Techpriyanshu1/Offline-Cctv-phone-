package com.example.monitor

import android.content.Context
import android.graphics.Bitmap
import com.example.motion.AlertPlayer
import com.example.recording.Mp4VideoEncoder
import com.example.recording.SnapshotManager
import com.example.storage.FileManager
import com.example.storage.TwinGuardDatabase
import com.example.storage.entities.PairedCameraEntity
import com.example.storage.entities.RecordingEntity
import com.example.streaming.AudioStreamer
import com.example.streaming.ControlSocketClient
import com.example.streaming.VideoStreamClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MonitorManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    val videoClient = VideoStreamClient()
    val controlClient = ControlSocketClient()
    val audioStreamer = AudioStreamer(isServer = false)
    val alertPlayer = AlertPlayer(context)
    val fileManager = FileManager(context)
    val localVideoEncoder = Mp4VideoEncoder()
    val database = TwinGuardDatabase.getDatabase(context)

    private val _isLocalRecording = MutableStateFlow(false)
    val isLocalRecording: StateFlow<Boolean> = _isLocalRecording.asStateFlow()

    private val _isIntercomActive = MutableStateFlow(false)
    val isIntercomActive: StateFlow<Boolean> = _isIntercomActive.asStateFlow()

    private val _isSpeakerMuted = MutableStateFlow(false)
    val isSpeakerMuted: StateFlow<Boolean> = _isSpeakerMuted.asStateFlow()

    private var activeHost: String = ""
    private var activePin: String = ""
    private var activeCameraName: String = "TwinGuard Camera"

    init {
        // Collect motion alerts from remote camera
        scope.launch {
            controlClient.motionAlerts.collect {
                alertPlayer.playMotionChime()
            }
        }

        // Feed incoming frames to local encoder if recording on monitor
        scope.launch(Dispatchers.IO) {
            videoClient.latestFrame.collect { frame ->
                if (_isLocalRecording.value && frame != null) {
                    localVideoEncoder.feedFrame(frame)
                }
            }
        }
    }

    fun connectToCamera(host: String, port: Int = 8888, pin: String, cameraName: String = "TwinGuard Camera") {
        activeHost = host
        activePin = pin
        activeCameraName = cameraName

        videoClient.connect(host, port)
        controlClient.connect(host, pin, "Monitor Phone", port = 8889)
        audioStreamer.startClient(host, port = 8890)

        // Save camera to paired cameras database
        scope.launch(Dispatchers.IO) {
            database.pairedCamerasDao().insertOrUpdate(
                PairedCameraEntity(
                    id = "$host:$port",
                    name = cameraName,
                    ipAddress = host,
                    port = port,
                    pairingPin = pin,
                    lastSeenTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun takeScreenshot(): File? {
        val currentBitmap = videoClient.latestFrame.value ?: return null
        val shotFile = fileManager.createNewScreenshotFile()
        val success = SnapshotManager.saveSnapshot(currentBitmap, shotFile)
        return if (success) shotFile else null
    }

    fun toggleRemoteTorch() {
        val currentTorch = controlClient.telemetry.value.torchOn
        controlClient.setTorch(!currentTorch)
    }

    fun switchRemoteCamera() {
        controlClient.switchCamera()
    }

    fun toggleRemoteRecording() {
        val isRec = controlClient.telemetry.value.isRecording
        if (isRec) {
            controlClient.stopRecording()
        } else {
            controlClient.startRecording()
        }
    }

    fun toggleLocalMonitorRecording(): Boolean {
        if (_isLocalRecording.value) {
            val result = localVideoEncoder.stopRecording()
            _isLocalRecording.value = false
            result?.let { rec ->
                scope.launch(Dispatchers.IO) {
                    database.recordingsDao().insertRecording(
                        RecordingEntity(
                            title = rec.file.name,
                            filePath = rec.file.absolutePath,
                            durationMs = rec.durationMs,
                            fileSizeBytes = rec.fileSizeBytes,
                            timestamp = System.currentTimeMillis(),
                            isMotionTriggered = false,
                            cameraName = activeCameraName,
                            isSavedOnCamera = false
                        )
                    )
                }
            }
            return false
        } else {
            val file = fileManager.createNewRecordingFile("MONITOR_REC")
            val success = localVideoEncoder.startRecording(file)
            if (success) {
                _isLocalRecording.value = true
            }
            return success
        }
    }

    fun setIntercomTalking(isTalking: Boolean) {
        _isIntercomActive.value = isTalking
        audioStreamer.setMicEnabled(isTalking)
    }

    fun toggleSpeakerMute() {
        val newMute = !_isSpeakerMuted.value
        _isSpeakerMuted.value = newMute
        audioStreamer.setSpeakerEnabled(!newMute)
    }

    fun setRemoteResolution(res: String) {
        controlClient.setResolution(res)
    }

    fun disconnect() {
        if (_isLocalRecording.value) {
            toggleLocalMonitorRecording()
        }
        videoClient.disconnect()
        controlClient.disconnect()
        audioStreamer.stop()
    }
}
