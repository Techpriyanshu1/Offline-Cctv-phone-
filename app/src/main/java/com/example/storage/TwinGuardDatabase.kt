package com.example.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.storage.dao.MotionEventsDao
import com.example.storage.dao.PairedCamerasDao
import com.example.storage.dao.RecordingsDao
import com.example.storage.entities.MotionEventEntity
import com.example.storage.entities.PairedCameraEntity
import com.example.storage.entities.RecordingEntity

@Database(
    entities = [RecordingEntity::class, MotionEventEntity::class, PairedCameraEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TwinGuardDatabase : RoomDatabase() {
    abstract fun recordingsDao(): RecordingsDao
    abstract fun motionEventsDao(): MotionEventsDao
    abstract fun pairedCamerasDao(): PairedCamerasDao

    companion object {
        @Volatile
        private var INSTANCE: TwinGuardDatabase? = null

        fun getDatabase(context: Context): TwinGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TwinGuardDatabase::class.java,
                    "twinguard_cctv_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
