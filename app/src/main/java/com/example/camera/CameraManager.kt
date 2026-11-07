package com.example.camera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.motion.AlertPlayer
import com.example.motion.OfflineMotionDetector
import com.example.networking.CameraTelemetry
import com.example.recording.Mp4VideoEncoder
import com.example.recording.SnapshotManager
import com.example.storage.FileManager
import com.example.storage.TwinGuardDatabase
import com.example.storage.entities.MotionEventEntity
import com.example.storage.entities.RecordingEntity
import com.example.streaming.AudioStreamer
import com.example.streaming.ControlSocketServer
import com.example.streaming.VideoStreamServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    val pairingPin: String
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null

    val motionDetector = OfflineMotionDetector()
    val alertPlayer = AlertPlayer(context)
    val fileManager = FileManager(context)
    val videoEncoder = Mp4VideoEncoder()
    val database = TwinGuardDatabase.getDatabase(context)

    // Streaming Servers
    val videoServer = VideoStreamServer(expectedPin = pairingPin)
    lateinit var controlServer: ControlSocketServer
    val audioStreamer = AudioStreamer(isServer = true)

    // Camera State
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _currentLens = MutableStateFlow(CameraLens.BACK)
    val currentLens: StateFlow<CameraLens> = _currentLens.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _resolution = MutableStateFlow("720p")
    val resolution: StateFlow<String> = _resolution.asStateFlow()

    private val _batteryPct = MutableStateFlow(100)
    val batteryPct: StateFlow<Int> = _batteryPct.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    var autoRecordOnMotion = true

    enum class CameraLens { BACK, FRONT }

    private var telemetryJob: Job? = null
    private var activeRecordingFile: File? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                _batteryPct.value = ((level.toFloat() / scale.toFloat()) * 100).toInt()
            }
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            _isCharging.value = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL)
        }
    }

    init {
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        controlServer = ControlSocketServer(
            expectedPin = pairingPin,
            cameraName = "TwinGuard Node",
            onTorchCommand = { toggleTorch(it) },
            onSwitchCameraCommand = { switchLens() },
            onStartRecordingCommand = { startRecording() },
            onStopRecordingCommand = { stopRecording() },
            onResolutionCommand = { setResolution(it) },
            onZoomCommand = { setZoom(it) }
        )

        // Listen for motion events from detector
        scope.launch {
            motionDetector.motionTriggerEvents.collect { motionResult ->
                onMotionTriggered(motionResult)
            }
        }
    }

    fun startServers() {
        videoServer.start()
        controlServer.start()
        audioStreamer.startServer()
        _isStreaming.value = true

        // Periodic telemetry broadcast (every 1 second)
        telemetryJob?.cancel()
        telemetryJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val stats = fileManager.getStorageStats()
                val telemetry = CameraTelemetry(
                    batteryPct = _batteryPct.value,
                    isCharging = _isCharging.value,
                    isRecording = _isRecording.value,
                    torchOn = _isTorchOn.value,
                    currentLens = if (_currentLens.value == CameraLens.BACK) "BACK" else "FRONT",
                    fps = videoServer.fps.value,
                    resolution = _resolution.value,
                    storageFreeMb = stats.availableBytes / (1024 * 1024),
                    motionDetected = motionDetector.motionState.value.isMotionDetected,
                    clientCount = videoServer.activeViewers.value
                )
                controlServer.broadcastTelemetry(telemetry)
                delay(1000)
            }
        }
    }

    fun bindToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(lifecycleOwner, previewView)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        val provider = cameraProvider ?: return
        val selector = if (_currentLens.value == CameraLens.BACK) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val targetResolution = when (_resolution.value) {
            "1080p" -> Size(1920, 1080)
            "480p" -> Size(640, 480)
            else -> Size(1280, 720)
        }

        preview = Preview.Builder()
            .setTargetResolution(targetResolution)
            .build()

        previewView?.let {
            preview?.setSurfaceProvider(it.surfaceProvider)
        }

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(targetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis?.setAnalyzer(analysisExecutor) { imageProxy ->
            processAnalysisFrame(imageProxy)
        }

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
            // Restore torch & zoom
            camera?.cameraControl?.setZoomRatio(_zoomRatio.value)
            if (_isTorchOn.value) {
                camera?.cameraControl?.enableTorch(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processAnalysisFrame(imageProxy: ImageProxy) {
        try {
            // 1. Smart Motion Detection
            motionDetector.processFrame(imageProxy)

            // 2. Compress to JPEG for live streaming
            val jpegBytes = VideoStreamServer.imageProxyToJpeg(imageProxy, quality = 70)
            if (jpegBytes != null) {
                videoServer.broadcastFrame(jpegBytes)

                // 3. Feed to VideoEncoder if currently recording
                if (_isRecording.value) {
                    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                    if (bitmap != null) {
                        videoEncoder.feedFrame(bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    private fun onMotionTriggered(motionResult: OfflineMotionDetector.MotionResult) {
        alertPlayer.playMotionChime()
        controlServer.broadcastMotionAlert()

        // Capture snapshot
        scope.launch(Dispatchers.IO) {
            val snapshotFile = fileManager.createNewMotionSnapshotFile()
            // Save motion event record
            database.motionEventsDao().insertEvent(
                MotionEventEntity(
                    timestamp = System.currentTimeMillis(),
                    snapshotPath = snapshotFile.absolutePath,
                    deltaScore = motionResult.deltaScore,
                    cameraName = "Local Camera"
                )
            )

            // Auto-trigger recording if enabled and not already recording
            if (autoRecordOnMotion && !_isRecording.value) {
                startRecording(isMotionTriggered = true)
                delay(10000) // record 10 seconds of motion event
                if (_isRecording.value) {
                    stopRecording()
                }
            }
        }
    }

    fun toggleTorch(enable: Boolean? = null) {
        val newState = enable ?: !_isTorchOn.value
        _isTorchOn.value = newState
        camera?.cameraControl?.enableTorch(newState)
    }

    fun switchLens() {
        _currentLens.value = if (_currentLens.value == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        _isTorchOn.value = false
        // Rebind needed with new selector
    }

    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(1.0f, 5.0f)
        _zoomRatio.value = clamped
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    fun setResolution(res: String) {
        _resolution.value = res
        // Rebind will take effect on next start or manual switch
    }

    fun startRecording(isMotionTriggered: Boolean = false): Boolean {
        if (_isRecording.value) return false
        val file = fileManager.createNewRecordingFile(if (isMotionTriggered) "MOTION" else "REC")
        activeRecordingFile = file

        val success = videoEncoder.startRecording(file)
        if (success) {
            _isRecording.value = true
        }
        return success
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        val result = videoEncoder.stopRecording()
        _isRecording.value = false

        result?.let { rec ->
            scope.launch(Dispatchers.IO) {
                database.recordingsDao().insertRecording(
                    RecordingEntity(
                        title = rec.file.name,
                        filePath = rec.file.absolutePath,
                        durationMs = rec.durationMs,
                        fileSizeBytes = rec.fileSizeBytes,
                        timestamp = System.currentTimeMillis(),
                        isMotionTriggered = rec.file.name.startsWith("MOTION"),
                        cameraName = "Local Camera",
                        isSavedOnCamera = true
                    )
                )
            }
        }
        val file = activeRecordingFile
        activeRecordingFile = null
        return file
    }

    fun release() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {}

        telemetryJob?.cancel()
        videoServer.stop()
        controlServer.stop()
        audioStreamer.stop()
        stopRecording()
        alertPlayer.release()
        analysisExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
