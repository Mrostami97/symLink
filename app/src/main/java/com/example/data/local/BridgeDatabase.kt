package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PairedDeviceEntity::class,
        BridgeMessageEntity::class,
        BridgeCallEventEntity::class,
        SyncedContactEntity::class,
        BatteryHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BridgeDatabase : RoomDatabase() {
    abstract fun bridgeDao(): BridgeDao

    companion object {
        @Volatile
        private var INSTANCE: BridgeDatabase? = null

        fun getInstance(context: Context): BridgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BridgeDatabase::class.java,
                    "itell_bridge_db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
