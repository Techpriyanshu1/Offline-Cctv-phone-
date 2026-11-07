package com.example.recording

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Mp4VideoEncoder {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var encodingJob: Job? = null

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    private var trackIndex = -1
    private var muxerStarted = false

    private val isRecording = AtomicBoolean(false)
    private val frameQueue = ArrayBlockingQueue<Bitmap>(30)

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private var recordingStartTime = 0L
    private var outputFile: File? = null

    fun startRecording(file: File, width: Int = 1280, height: Int = 720, bitRate: Int = 2_500_000): Boolean {
        if (isRecording.get()) return false

        try {
            outputFile = file
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = mediaCodec?.createInputSurface()
            mediaCodec?.start()

            mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            isRecording.set(true)
            recordingStartTime = System.currentTimeMillis()
            frameQueue.clear()

            encodingJob = scope.launch {
                val bufferInfo = MediaCodec.BufferInfo()
                var frameIndex = 0L

                while (isActive && isRecording.get()) {
                    // Update duration
                    _recordingDurationMs.value = System.currentTimeMillis() - recordingStartTime

                    val bitmap = frameQueue.poll(20, TimeUnit.MILLISECONDS)
                    if (bitmap != null && inputSurface != null && inputSurface!!.isValid) {
                        try {
                            val canvas = inputSurface!!.lockCanvas(null)
                            val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                            val dstRect = android.graphics.Rect(0, 0, width, height)
                            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
                            inputSurface!!.unlockCanvasAndPost(canvas)
                            frameIndex++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    drainEncoder(bufferInfo, false)
                }

                // Final drain
                drainEncoder(bufferInfo, true)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            return false
        }
    }

    fun feedFrame(bitmap: Bitmap) {
        if (!isRecording.get()) return
        if (frameQueue.remainingCapacity() > 0) {
            frameQueue.offer(bitmap)
        }
    }

    private fun drainEncoder(bufferInfo: MediaCodec.BufferInfo, endOfStream: Boolean) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return

        if (endOfStream) {
            try {
                codec.signalEndOfInputStream()
            } catch (ignored: Exception) {}
        }

        while (true) {
            val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) throw RuntimeException("Format changed twice")
                val newFormat = codec.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
            } else if (encoderStatus >= 0) {
                val encodedData = codec.getOutputBuffer(encoderStatus)
                    ?: throw RuntimeException("Encoder outputBuffer $encoderStatus was null")

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw RuntimeException("Muxer hasn't started yet")
                    }
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }

                codec.releaseOutputBuffer(encoderStatus, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    fun stopRecording(): RecordingResult? {
        if (!isRecording.get()) return null
        isRecording.set(false)
        val duration = System.currentTimeMillis() - recordingStartTime

        try {
            encodingJob?.cancel()
            drainEncoder(MediaCodec.BufferInfo(), true)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cleanup()
        }

        val file = outputFile ?: return null
        return RecordingResult(
            file = file,
            durationMs = duration,
            fileSizeBytes = file.length()
        )
    }

    private fun cleanup() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (ignored: Exception) {}
        mediaCodec = null

        try {
            if (muxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (ignored: Exception) {}
        mediaMuxer = null

        inputSurface?.release()
        inputSurface = null
        muxerStarted = false
        trackIndex = -1
        _recordingDurationMs.value = 0L
    }

    data class RecordingResult(
        val file: File,
        val durationMs: Long,
        val fileSizeBytes: Long
    )
}
