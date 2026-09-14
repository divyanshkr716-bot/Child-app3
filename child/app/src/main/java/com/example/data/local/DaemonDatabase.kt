package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DaemonLog
import com.example.data.model.PairedParentDevice
import com.example.data.model.SyncedNotification

@Database(
    entities = [
        PairedParentDevice::class,
        DaemonLog::class,
        SyncedNotification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DaemonDatabase : RoomDatabase() {
    abstract fun daemonDao(): DaemonDao

    companion object {
        @Volatile
        private var INSTANCE: DaemonDatabase? = null

        fun getInstance(context: Context): DaemonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DaemonDatabase::class.java,
                    "airdroid_daemon.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
