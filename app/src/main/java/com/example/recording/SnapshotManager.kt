package com.example.recording

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object SnapshotManager {

    fun saveSnapshot(bitmap: Bitmap, targetFile: File, quality: Int = 90): Boolean {
        return try {
            val parent = targetFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val fos = FileOutputStream(targetFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            fos.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
