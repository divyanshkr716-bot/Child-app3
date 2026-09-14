package com.example.data.local

import com.example.data.model.DaemonLog
import com.example.data.model.PairedParentDevice
import com.example.data.model.SyncedNotification
import kotlinx.coroutines.flow.Flow

class DaemonRepository(private val dao: DaemonDao) {

    val pairedDevices: Flow<List<PairedParentDevice>> = dao.getAllPairedDevices()
    val activeDevice: Flow<PairedParentDevice?> = dao.getActiveDevice()
    val recentLogs: Flow<List<DaemonLog>> = dao.getRecentLogs()
    val recentNotifications: Flow<List<SyncedNotification>> = dao.getRecentNotifications()

    suspend fun saveDevice(device: PairedParentDevice) {
        dao.insertOrUpdateDevice(device)
    }

    suspend fun updateDevice(device: PairedParentDevice) {
        dao.updateDevice(device)
    }

    suspend fun getDeviceByCode(code: String): PairedParentDevice? {
        return dao.getDeviceByCode(code)
    }

    suspend fun setDeviceConnected(deviceId: String, isConnected: Boolean, ip: String = "") {
        dao.getAllPairedDevices()
        if (isConnected) {
            dao.markAllDisconnected()
        }
        val device = dao.getDeviceByCode(deviceId)
        if (device != null) {
            dao.updateDevice(
                device.copy(
                    isConnected = isConnected,
                    ipAddress = if (ip.isNotEmpty()) ip else device.ipAddress,
                    lastSeen = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun logAction(actionType: String, details: String, isSuccess: Boolean = true, source: String = "Parent Console") {
        dao.insertLog(
            DaemonLog(
                actionType = actionType,
                details = details,
                sourceDevice = source,
                isSuccess = isSuccess
            )
        )
    }

    suspend fun clearLogs() {
        dao.clearLogs()
    }

    suspend fun saveNotification(notification: SyncedNotification) {
        dao.insertNotification(notification)
    }

    suspend fun removeNotification(id: String) {
        dao.deleteNotification(id)
    }
}
