package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DaemonLog
import com.example.data.model.PairedParentDevice
import com.example.data.model.SyncedNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface DaemonDao {

    // Paired Parent Devices
    @Query("SELECT * FROM paired_devices ORDER BY lastSeen DESC")
    fun getAllPairedDevices(): Flow<List<PairedParentDevice>>

    @Query("SELECT * FROM paired_devices WHERE isConnected = 1 LIMIT 1")
    fun getActiveDevice(): Flow<PairedParentDevice?>

    @Query("SELECT * FROM paired_devices WHERE pairingCode = :code LIMIT 1")
    suspend fun getDeviceByCode(code: String): PairedParentDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: PairedParentDevice)

    @Update
    suspend fun updateDevice(device: PairedParentDevice)

    @Query("UPDATE paired_devices SET isConnected = 0")
    suspend fun markAllDisconnected()

    @Query("DELETE FROM paired_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    // Audit Logs
    @Query("SELECT * FROM daemon_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<DaemonLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DaemonLog)

    @Query("DELETE FROM daemon_logs")
    suspend fun clearLogs()

    // Synced Notifications
    @Query("SELECT * FROM synced_notifications ORDER BY postTime DESC LIMIT 50")
    fun getRecentNotifications(): Flow<List<SyncedNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: SyncedNotification)

    @Query("DELETE FROM synced_notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)
}
