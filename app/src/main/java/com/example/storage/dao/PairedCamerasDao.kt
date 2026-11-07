package com.example.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.storage.entities.PairedCameraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedCamerasDao {
    @Query("SELECT * FROM paired_cameras ORDER BY lastSeenTimestamp DESC")
    fun getAllPairedCameras(): Flow<List<PairedCameraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(camera: PairedCameraEntity)

    @Delete
    suspend fun deleteCamera(camera: PairedCameraEntity)

    @Query("DELETE FROM paired_cameras WHERE id = :id")
    suspend fun deleteById(id: String)
}
