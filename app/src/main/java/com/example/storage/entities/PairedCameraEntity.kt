package com.example.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_cameras")
data class PairedCameraEntity(
    @PrimaryKey
    val id: String, // Device ID or MAC/UUID
    val name: String,
    val ipAddress: String,
    val port: Int = 8888,
    val pairingPin: String = "",
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val connectionType: String = "Wi-Fi Direct" // or Hotspot / Local LAN
)
