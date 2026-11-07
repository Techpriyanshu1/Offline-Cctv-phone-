package com.example.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.storage.entities.MotionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotionEventsDao {
    @Query("SELECT * FROM motion_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<MotionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MotionEventEntity): Long

    @Delete
    suspend fun deleteEvent(event: MotionEventEntity)

    @Query("DELETE FROM motion_events")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM motion_events")
    fun getEventCount(): Flow<Int>
}
