package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
  @Query("SELECT * FROM devices ORDER BY name ASC")
  fun getAllDevices(): Flow<List<DeviceEntity>>

  @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
  suspend fun getDeviceById(id: String): DeviceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(device: DeviceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(devices: List<DeviceEntity>)

  @Update
  suspend fun update(device: DeviceEntity)

  @Delete
  suspend fun delete(device: DeviceEntity)
}

@Dao
interface NotificationDao {
  @Query("SELECT * FROM notifications ORDER BY id DESC")
  fun getAllNotifications(): Flow<List<NotificationEntity>>

  @Query("SELECT * FROM notifications WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getNotificationsForDevice(deviceId: String): Flow<List<NotificationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(notification: NotificationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(notifications: List<NotificationEntity>)

  @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
  suspend fun markAsRead(id: String)

  @Query("UPDATE notifications SET repliesJson = :repliesJson WHERE id = :id")
  suspend fun updateReplies(id: String, repliesJson: String)

  @Query("DELETE FROM notifications WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM notifications")
  suspend fun clearAll()
}

@Dao
interface SmsDao {
  @Query("SELECT * FROM sms_messages ORDER BY id ASC")
  fun getAllMessages(): Flow<List<SmsEntity>>

  @Query("SELECT * FROM sms_messages WHERE threadId = :threadId ORDER BY id ASC")
  fun getMessagesForThread(threadId: String): Flow<List<SmsEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(sms: SmsEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(messages: List<SmsEntity>)

  @Delete
  suspend fun delete(sms: SmsEntity)
}

@Dao
interface ContactDao {
  @Query("SELECT * FROM contacts ORDER BY name ASC")
  fun getAllContacts(): Flow<List<ContactEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(contact: ContactEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(contacts: List<ContactEntity>)

  @Update
  suspend fun update(contact: ContactEntity)

  @Delete
  suspend fun delete(contact: ContactEntity)
}

@Dao
interface CallLogDao {
  @Query("SELECT * FROM call_logs ORDER BY id DESC")
  fun getAllCallLogs(): Flow<List<CallLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(callLog: CallLogEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(callLogs: List<CallLogEntity>)

  @Delete
  suspend fun delete(callLog: CallLogEntity)
}

@Dao
interface TransferDao {
  @Query("SELECT * FROM transfers ORDER BY timestamp DESC")
  fun getAllTransfers(): Flow<List<TransferEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(transfer: TransferEntity)

  @Update
  suspend fun update(transfer: TransferEntity)

  @Query("DELETE FROM transfers")
  suspend fun clearAll()
}

@Dao
interface LocationDao {
  @Query("SELECT * FROM child_locations WHERE deviceId = :deviceId LIMIT 1")
  fun getLocationForDevice(deviceId: String): Flow<LocationEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(location: LocationEntity)
}

@Dao
interface GeofenceDao {
  @Query("SELECT * FROM geofences WHERE deviceId = :deviceId ORDER BY name ASC")
  fun getGeofencesForDevice(deviceId: String): Flow<List<GeofenceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(geofence: GeofenceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(geofences: List<GeofenceEntity>)

  @Update
  suspend fun update(geofence: GeofenceEntity)

  @Delete
  suspend fun delete(geofence: GeofenceEntity)
}

@Dao
interface ManagedAppDao {
  @Query("SELECT * FROM managed_apps WHERE deviceId = :deviceId ORDER BY usageMinutesToday DESC")
  fun getAppsForDevice(deviceId: String): Flow<List<ManagedAppEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(apps: List<ManagedAppEntity>)

  @Query("UPDATE managed_apps SET isBlocked = :isBlocked WHERE packageName = :packageName AND deviceId = :deviceId")
  suspend fun updateBlockStatus(packageName: String, deviceId: String, isBlocked: Boolean)

  @Query("UPDATE managed_apps SET dailyLimitMinutes = :limitMinutes WHERE packageName = :packageName AND deviceId = :deviceId")
  suspend fun updateDailyLimit(packageName: String, deviceId: String, limitMinutes: Int)

  @Query("UPDATE managed_apps SET isAlwaysAllowed = :allowed WHERE packageName = :packageName AND deviceId = :deviceId")
  suspend fun updateAlwaysAllowed(packageName: String, deviceId: String, allowed: Boolean)
}

@Dao
interface TimelineEventDao {
  @Query("SELECT * FROM timeline_events WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getEventsForDevice(deviceId: String): Flow<List<TimelineEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(events: List<TimelineEventEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(event: TimelineEventEntity)
}

@Dao
interface ChildRequestDao {
  @Query("SELECT * FROM child_requests WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getRequestsForDevice(deviceId: String): Flow<List<ChildRequestEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(requests: List<ChildRequestEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(request: ChildRequestEntity)

  @Query("UPDATE child_requests SET status = :status WHERE id = :id")
  suspend fun updateStatus(id: String, status: String)
}

@Dao
interface DeviceAlertDao {
  @Query("SELECT * FROM device_alerts WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getAlertsForDevice(deviceId: String): Flow<List<DeviceAlertEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(alerts: List<DeviceAlertEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(alert: DeviceAlertEntity)

  @Query("UPDATE device_alerts SET isResolved = 1 WHERE id = :id")
  suspend fun resolveAlert(id: String)
}

@Dao
interface WebHistoryDao {
  @Query("SELECT * FROM web_history WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getWebHistoryForDevice(deviceId: String): Flow<List<WebHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(history: List<WebHistoryEntity>)

  @Query("UPDATE web_history SET isBlocked = :isBlocked WHERE domain = :domain AND deviceId = :deviceId")
  suspend fun updateDomainBlockStatus(domain: String, deviceId: String, isBlocked: Boolean)
}

@Dao
interface DrivingTripDao {
  @Query("SELECT * FROM driving_trips WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getTripsForDevice(deviceId: String): Flow<List<DrivingTripEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(trips: List<DrivingTripEntity>)
}

@Dao
interface DowntimePolicyDao {
  @Query("SELECT * FROM downtime_policies WHERE deviceId = :deviceId")
  fun getPoliciesForDevice(deviceId: String): Flow<List<DowntimePolicyEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(policy: DowntimePolicyEntity)

  @Query("UPDATE downtime_policies SET isEnabled = :isEnabled WHERE id = :id")
  suspend fun setEnabled(id: String, isEnabled: Boolean)

  @Delete
  suspend fun delete(policy: DowntimePolicyEntity)
}

@Dao
interface InstantBlockDao {
  @Query("SELECT * FROM instant_blocks WHERE deviceId = :deviceId LIMIT 1")
  fun getInstantBlock(deviceId: String): Flow<InstantBlockEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setInstantBlock(instantBlock: InstantBlockEntity)
}

@Dao
interface DetectionEventDao {
  @Query("SELECT * FROM detection_events WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getEventsForDevice(deviceId: String): Flow<List<DetectionEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(event: DetectionEventEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(events: List<DetectionEventEntity>)

  @Query("UPDATE detection_events SET isReviewed = 1 WHERE id = :id")
  suspend fun markReviewed(id: String)
}

@Dao
interface ImageDetectionDao {
  @Query("SELECT * FROM image_detections WHERE deviceId = :deviceId ORDER BY id DESC")
  fun getDetectionsForDevice(deviceId: String): Flow<List<ImageDetectionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(record: ImageDetectionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(records: List<ImageDetectionEntity>)
}

@Dao
interface FamilyMemberDao {
  @Query("SELECT * FROM family_members ORDER BY name ASC")
  fun getAllMembers(): Flow<List<FamilyMemberEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(members: List<FamilyMemberEntity>)
}

@Dao
interface FamilyChatMessageDao {
  @Query("SELECT * FROM family_chat_messages ORDER BY id ASC")
  fun getAllMessages(): Flow<List<FamilyChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(message: FamilyChatMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(messages: List<FamilyChatMessageEntity>)
}

@Dao
interface OfflineQueueDao {
  @Query("SELECT * FROM offline_queue WHERE isSynced = 0 ORDER BY timestamp ASC")
  fun getPendingEvents(): Flow<List<OfflineQueueEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun enqueue(event: OfflineQueueEntity)

  @Query("UPDATE offline_queue SET isSynced = 1 WHERE id = :id")
  suspend fun markSynced(id: String)

  @Query("DELETE FROM offline_queue WHERE isSynced = 1")
  suspend fun clearSynced()
}


