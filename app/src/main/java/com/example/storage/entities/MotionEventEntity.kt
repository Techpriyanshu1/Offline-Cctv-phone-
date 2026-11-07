package com.example.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motion_events")
data class MotionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val snapshotPath: String? = null,
    val deltaScore: Float,
    val cameraName: String = "TwinGuard Node",
    val acknowledged: Boolean = false,
    val triggerZone: String = "Center"
)
