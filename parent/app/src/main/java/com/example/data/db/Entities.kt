package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
  @PrimaryKey val id: String,
  val name: String,
  val model: String,
  val osVersion: String,
  val batteryPercent: Int,
  val isCharging: Boolean,
  val storageUsedGb: Double,
  val storageTotalGb: Double,
  val isOnline: Boolean,
  val wifiSsid: String,
  val wifiSignalDbm: Int,
  val ipAddress: String,
  val connectionMode: String,
  val lastSeen: String,
  val authToken: String = ""
)

@Entity(tableName = "notifications")
data class NotificationEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val appName: String,
  val packageName: String,
  val title: String,
  val content: String,
  val time: String,
  val isRead: Boolean,
  val repliesJson: String
)

@Entity(tableName = "sms_messages")
data class SmsEntity(
  @PrimaryKey val id: String,
  val threadId: String,
  val deviceId: String,
  val contactName: String,
  val phoneNumber: String,
  val isFromMe: Boolean,
  val text: String,
  val time: String,
  val status: String
)

@Entity(tableName = "contacts")
data class ContactEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val name: String,
  val phone: String,
  val email: String,
  val label: String
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val contactName: String,
  val phoneNumber: String,
  val callType: String,
  val time: String,
  val durationSeconds: Int
)

@Entity(tableName = "transfers")
data class TransferEntity(
  @PrimaryKey val id: String,
  val fileName: String,
  val fileSize: Long,
  val progress: Float,
  val speedKbps: Double,
  val isUpload: Boolean,
  val status: String,
  val timestamp: Long
)

@Entity(tableName = "child_locations")
data class LocationEntity(
  @PrimaryKey val deviceId: String,
  val latitude: Double,
  val longitude: Double,
  val address: String,
  val accuracyMeters: Float,
  val timestamp: String,
  val batteryAtLocation: Int,
  val isMoving: Boolean
)

@Entity(tableName = "geofences")
data class GeofenceEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val name: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val radiusMeters: Int,
  val isTriggerOnEnter: Boolean,
  val isTriggerOnExit: Boolean,
  val isCurrentlyInside: Boolean
)

@Entity(tableName = "managed_apps")
data class ManagedAppEntity(
  @PrimaryKey val packageName: String,
  val deviceId: String,
  val appName: String,
  val category: String,
  val usageMinutesToday: Int,
  val isBlocked: Boolean,
  val dailyLimitMinutes: Int,
  val isAlwaysAllowed: Boolean,
  val installDate: String
)

@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val title: String,
  val description: String,
  val time: String,
  val category: String,
  val iconType: String
)

@Entity(tableName = "child_requests")
data class ChildRequestEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val childName: String,
  val appName: String,
  val packageName: String,
  val requestedMinutes: Int,
  val requestType: String,
  val reason: String,
  val timestamp: String,
  val status: String
)

@Entity(tableName = "device_alerts")
data class DeviceAlertEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val title: String,
  val message: String,
  val alertType: String,
  val timestamp: String,
  val severity: String,
  val isResolved: Boolean
)

@Entity(tableName = "web_history")
data class WebHistoryEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val title: String,
  val url: String,
  val domain: String,
  val visitCount: Int,
  val timestamp: String,
  val isBlocked: Boolean,
  val category: String
)

@Entity(tableName = "driving_trips")
data class DrivingTripEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val date: String,
  val startTime: String,
  val endTime: String,
  val origin: String,
  val destination: String,
  val distanceMiles: Double,
  val durationMinutes: Int,
  val topSpeedMph: Int,
  val avgSpeedMph: Int,
  val hardBrakingEvents: Int,
  val rapidAccels: Int,
  val phoneUsageMinutes: Int,
  val safetyScore: Int
)

@Entity(tableName = "downtime_policies")
data class DowntimePolicyEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val name: String,
  val startTime: String,
  val endTime: String,
  val daysOfWeek: String,
  val isEnabled: Boolean,
  val allowedAppsJson: String
)

@Entity(tableName = "instant_blocks")
data class InstantBlockEntity(
  @PrimaryKey val deviceId: String,
  val isActive: Boolean,
  val blockUntilTimestamp: Long,
  val durationOption: String
)

@Entity(tableName = "detection_events")
data class DetectionEventEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val appName: String,
  val packageName: String,
  val keyword: String,
  val severity: String,
  val category: String,
  val contextSnippet: String,
  val timestamp: String,
  val isReviewed: Boolean
)

@Entity(tableName = "image_detections")
data class ImageDetectionEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val imageUri: String,
  val riskCategory: String,
  val confidenceScore: Float,
  val timestamp: String,
  val flagged: Boolean
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
  @PrimaryKey val id: String,
  val name: String,
  val role: String,
  val isOnline: Boolean,
  val lastSeen: String
)

@Entity(tableName = "family_chat_messages")
data class FamilyChatMessageEntity(
  @PrimaryKey val id: String,
  val senderId: String,
  val senderName: String,
  val message: String,
  val timestamp: String,
  val isFromParent: Boolean,
  val status: String
)

@Entity(tableName = "offline_queue")
data class OfflineQueueEntity(
  @PrimaryKey val id: String,
  val deviceId: String,
  val eventType: String,
  val payloadJson: String,
  val timestamp: Long,
  val isSynced: Boolean
)

