package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceEntity::class, AuditLogEntity::class, CommandEntity::class, MasterPinEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun sentinelDao(): SentinelDao

    companion object {
        @Volatile
        private var INSTANCE: SentinelDatabase? = null

        fun getDatabase(context: Context): SentinelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SentinelDatabase::class.java,
                    "sentinel_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
