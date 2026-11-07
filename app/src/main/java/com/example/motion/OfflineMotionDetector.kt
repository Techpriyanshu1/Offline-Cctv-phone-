package com.example.motion

import android.graphics.RectF
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.math.abs

class OfflineMotionDetector {

    data class MotionResult(
        val isMotionDetected: Boolean,
        val deltaScore: Float,
        val boundingBox: RectF? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _motionState = MutableStateFlow(MotionResult(false, 0f))
    val motionState: StateFlow<MotionResult> = _motionState.asStateFlow()

    private val _motionTriggerEvents = MutableSharedFlow<MotionResult>(extraBufferCapacity = 5)
    val motionTriggerEvents: SharedFlow<MotionResult> = _motionTriggerEvents.asSharedFlow()

    // Sensitivity threshold: percentage of changed pixels to trigger alert (e.g., 5.0f for medium)
    var sensitivityThreshold: Float = 5.0f
    var cooldownMs: Long = 4000L
    private var lastTriggerTime: Long = 0L

    private val gridWidth = 32
    private val gridHeight = 24
    private var previousGrid: IntArray? = null

    fun processFrame(imageProxy: ImageProxy) {
        val yPlane = imageProxy.planes[0]
        val buffer: ByteBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height

        val currentGrid = IntArray(gridWidth * gridHeight)
        val xStep = width / gridWidth
        val yStep = height / gridHeight

        // Sample luminance into 32x24 grid
        for (gy in 0 until gridHeight) {
            val py = gy * yStep
            for (gx in 0 until gridWidth) {
                val px = gx * xStep
                val offset = py * rowStride + px * pixelStride
                if (offset < buffer.limit()) {
                    currentGrid[gy * gridWidth + gx] = buffer.get(offset).toInt() and 0xFF
                }
            }
        }

        val prev = previousGrid
        if (prev == null) {
            previousGrid = currentGrid
            return
        }

        var changedCells = 0
        val noiseThreshold = 18 // minimum luminance difference to count as motion

        var minGx = gridWidth
        var maxGx = -1
        var minGy = gridHeight
        var maxGy = -1

        for (i in 0 until gridWidth * gridHeight) {
            val delta = abs(currentGrid[i] - prev[i])
            if (delta > noiseThreshold) {
                changedCells++
                val gx = i % gridWidth
                val gy = i / gridWidth
                if (gx < minGx) minGx = gx
                if (gx > maxGx) maxGx = gx
                if (gy < minGy) minGy = gy
                if (gy > maxGy) maxGy = gy
            }
        }

        previousGrid = currentGrid

        val totalCells = (gridWidth * gridHeight).toFloat()
        val deltaPercentage = (changedCells / totalCells) * 100f
        val isDetected = deltaPercentage >= sensitivityThreshold

        val boundingBox = if (isDetected && maxGx >= minGx && maxGy >= minGy) {
            RectF(
                minGx.toFloat() / gridWidth,
                minGy.toFloat() / gridHeight,
                (maxGx + 1).toFloat() / gridWidth,
                (maxGy + 1).toFloat() / gridHeight
            )
        } else null

        val result = MotionResult(isDetected, deltaPercentage, boundingBox)
        _motionState.value = result

        val now = System.currentTimeMillis()
        if (isDetected && (now - lastTriggerTime >= cooldownMs)) {
            lastTriggerTime = now
            _motionTriggerEvents.tryEmit(result)
        }
    }

    fun reset() {
        previousGrid = null
        _motionState.value = MotionResult(false, 0f)
    }
}
