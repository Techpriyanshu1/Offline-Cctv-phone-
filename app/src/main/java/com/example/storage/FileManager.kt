package com.example.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManager(private val context: Context) {

    private val baseDir: File by lazy {
        val external = context.getExternalFilesDir(null)
        val dir = File(external ?: context.filesDir, "TwinGuard")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val recordingsDir: File by lazy {
        File(baseDir, "Camera_Recordings").apply { if (!exists()) mkdirs() }
    }

    val motionEventsDir: File by lazy {
        File(baseDir, "Motion_Events").apply { if (!exists()) mkdirs() }
    }

    val screenshotsDir: File by lazy {
        File(baseDir, "Screenshots").apply { if (!exists()) mkdirs() }
    }

    fun createNewRecordingFile(prefix: String = "REC"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(recordingsDir, "${prefix}_${timeStamp}.mp4")
    }

    fun createNewMotionSnapshotFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(motionEventsDir, "MOTION_${timeStamp}.jpg")
    }

    fun createNewScreenshotFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(screenshotsDir, "SHOT_${timeStamp}.jpg")
    }

    fun getStorageStats(): StorageStats {
        return try {
            val stat = StatFs(baseDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val usedBytes = totalBytes - availableBytes

            val appRecordedBytes = calculateDirSize(recordingsDir) +
                    calculateDirSize(motionEventsDir) +
                    calculateDirSize(screenshotsDir)

            StorageStats(
                availableBytes = availableBytes,
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                appRecordingsBytes = appRecordedBytes,
                isLowStorage = availableBytes < (500L * 1024L * 1024L) // < 500 MB
            )
        } catch (e: Exception) {
            StorageStats(0L, 0L, 0L, 0L, false)
        }
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    data class StorageStats(
        val availableBytes: Long,
        val totalBytes: Long,
        val usedBytes: Long,
        val appRecordingsBytes: Long,
        val isLowStorage: Boolean
    ) {
        val availableFormatted: String get() = formatBytes(availableBytes)
        val totalFormatted: String get() = formatBytes(totalBytes)
        val appUsedFormatted: String get() = formatBytes(appRecordingsBytes)
        val freePercentage: Int get() = if (totalBytes > 0) ((availableBytes * 100) / totalBytes).toInt() else 0
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }
    }
}
