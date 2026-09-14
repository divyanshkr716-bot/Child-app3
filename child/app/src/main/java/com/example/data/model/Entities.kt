package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_devices")
data class PairedParentDevice(
    @PrimaryKey val deviceId: String,
    val deviceName: String,
    val pairingCode: String,
    val connectionToken: String,
    val ipAddress: String,
    val platform: String = "Desktop/Web",
    val pairedAt: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "daemon_logs")
data class DaemonLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // TOUCH, SWIPE, KEY_EVENT, SCREEN_CAST, NOTIFICATION, FILE_IO, CAMERA, PAIRING
    val details: String,
    val sourceDevice: String = "Parent Console",
    val isSuccess: Boolean = true
)

@Entity(tableName = "synced_notifications")
data class SyncedNotification(
    @PrimaryKey val id: String,
    val packageName: String,
    val appTitle: String,
    val title: String,
    val text: String,
    val postTime: Long = System.currentTimeMillis(),
    val canReply: Boolean = false,
    val isSynced: Boolean = true
)

enum class DaemonStatus {
    STOPPED,
    INITIALIZING,
    LISTENING,
    CONNECTED,
    PAUSED
}

data class RemoteTouchCommand(
    val type: String, // CLICK, SWIPE, KEY
    val x: Float = 0f,
    val y: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val durationMs: Long = 100,
    val keyCode: Int = 0 // 1=HOME, 2=BACK, 3=RECENTS, 4=VOL_UP, 5=VOL_DOWN
)

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)
