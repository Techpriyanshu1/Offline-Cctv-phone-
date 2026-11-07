package com.example.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val timestamp: Long,
    val isMotionTriggered: Boolean = false,
    val thumbnailPath: String? = null,
    val cameraName: String = "Local Camera",
    val isSavedOnCamera: Boolean = true
)
