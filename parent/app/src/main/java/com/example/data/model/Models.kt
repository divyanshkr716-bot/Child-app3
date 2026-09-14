package com.example.data.model

enum class ConnectionMode(val label: String, val badgeText: String) {
  LOCAL_P2P("Local Mode (P2P Wi-Fi)", "Local P2P (0 Data)"),
  REMOTE_CLOUD("Remote Mode (Encrypted Cloud Relay)", "Cloud Relay (TLS/E2EE)")
}

enum class FileType {
  FOLDER, IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, OTHER
}

enum class CallType(val label: String) {
  INCOMING("Incoming"),
  OUTGOING("Outgoing"),
  MISSED("Missed")
}

enum class ActiveTab(val title: String) {
  DASHBOARD("Dashboard"),
  AIR_MIRROR("AirMirror"),
  REMOTE_CAMERA("Remote Camera"),
  LOCATION("Live Location"),
  APP_MANAGEMENT("App Limits"),
  EVENTS_USAGE("Today's Events"),
  REQUESTS_ALERTS("Alerts & Requests"),
  FILE_TRANSFER("File Transfer"),
  MESSAGES("SMS & Notifs"),
  CALLS_CONTACTS("Calls & Contacts"),
  WEB_MONITOR("Web Protection"),
  SOCIAL_SAFETY("Social & AI Safety"),
  FAMILY_CHAT("Family Chat"),
  DRIVING_SAFETY("Driving Safety"),
  DEVICE_HEALTH("Device Health"),
  SOS_CENTER("Emergency SOS"),
  PAIRING("Device Pairing")
}

data class ChildDevice(
  val id: String,
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
  val connectionMode: ConnectionMode,
  val lastSeen: String,
  val authToken: String = ""
)

data class FileItem(
  val id: String,
  val deviceId: String,
  val name: String,
  val path: String,
  val isDirectory: Boolean,
  val sizeBytes: Long,
  val modifiedDate: String,
  val fileType: FileType,
  val isLocal: Boolean
)

data class TransferTask(
  val id: String,
  val fileName: String,
  val fileSize: Long,
  val progress: Float,
  val speedKbps: Double,
  val isUpload: Boolean,
  val status: String, // "Transferring", "Completed", "Queued"
  val timestamp: Long = System.currentTimeMillis()
)

data class RemoteNotification(
  val id: String,
  val deviceId: String,
  val appName: String,
  val packageName: String,
  val title: String,
  val content: String,
  val time: String,
  val isRead: Boolean = false,
  val replies: List<String> = emptyList()
)

data class SmsMessage(
  val id: String,
  val threadId: String,
  val isFromMe: Boolean,
  val text: String,
  val time: String,
  val status: String = "Delivered"
)

data class SmsThread(
  val id: String,
  val deviceId: String,
  val contactName: String,
  val phoneNumber: String,
  val lastMessage: String,
  val lastMessageTime: String,
  val unreadCount: Int = 0
)

data class Contact(
  val id: String,
  val deviceId: String,
  val name: String,
  val phone: String,
  val email: String,
  val label: String = "Mobile"
)

data class CallLog(
  val id: String,
  val deviceId: String,
  val contactName: String,
  val phoneNumber: String,
  val callType: CallType,
  val time: String,
  val durationSeconds: Int
)

data class UserSession(
  val email: String = "",
  val name: String = "Local Parent",
  val accountType: String = "Local Network",
  val isLoggedIn: Boolean = false,
  val sessionTimeoutMinutes: Int = 30
)

enum class ConnectionStateStatus(val label: String) {
  CONNECTED("Connected & Active"),
  CONNECTING("Connecting to Child Daemon..."),
  RECONNECTING("Reconnecting (Retry 2/5)..."),
  DISCONNECTED("Disconnected (Daemon Offline)"),
  AUTH_FAILED("Authentication Failed")
}

data class ChildLocation(
  val deviceId: String,
  val latitude: Double,
  val longitude: Double,
  val address: String,
  val accuracyMeters: Float,
  val timestamp: String,
  val batteryAtLocation: Int,
  val isMoving: Boolean
)

data class RoutePoint(
  val latitude: Double,
  val longitude: Double,
  val timestamp: String,
  val speedKmh: Float,
  val label: String = ""
)

data class Geofence(
  val id: String,
  val deviceId: String,
  val name: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val radiusMeters: Int,
  val isTriggerOnEnter: Boolean = true,
  val isTriggerOnExit: Boolean = true,
  val isCurrentlyInside: Boolean = true
)

data class ManagedApp(
  val packageName: String,
  val deviceId: String,
  val appName: String,
  val category: String, // Social, Gaming, Video, Education, Utility
  val usageMinutesToday: Int,
  val isBlocked: Boolean,
  val dailyLimitMinutes: Int = 0, // 0 = unlimited
  val isAlwaysAllowed: Boolean = false,
  val installDate: String = "Unknown"
)

data class DowntimeSchedule(
  val id: String,
  val deviceId: String,
  val name: String,
  val startTime: String,
  val endTime: String,
  val days: String,
  val isEnabled: Boolean
)

data class ActivityTimelineEvent(
  val id: String,
  val deviceId: String,
  val title: String,
  val description: String,
  val time: String,
  val category: String,
  val iconType: String
)

data class DailyUsageStats(
  val totalScreenMinutes: Int,
  val dailyLimitMinutes: Int,
  val educationMinutes: Int,
  val entertainmentMinutes: Int,
  val gamingMinutes: Int,
  val socialMinutes: Int,
  val wifiDataMb: Int,
  val mobileDataMb: Int,
  val notificationsCount: Int
)

enum class RequestStatus {
  PENDING, APPROVED, REJECTED
}

data class ChildAppRequest(
  val id: String,
  val deviceId: String,
  val childName: String,
  val appName: String,
  val packageName: String,
  val requestedMinutes: Int,
  val requestType: String, // "EXTRA_TIME", "UNBLOCK_APP"
  val reason: String,
  val timestamp: String,
  val status: RequestStatus
)

enum class AlertSeverity {
  LOW, MEDIUM, HIGH, CRITICAL
}

data class DeviceAlert(
  val id: String,
  val deviceId: String,
  val title: String,
  val message: String,
  val alertType: String, // "OFFLINE", "LOW_BATTERY", "BLOCKED_APP_ATTEMPT", "GEOFENCE_EXIT", "NEW_APP", "TAMPER"
  val timestamp: String,
  val severity: AlertSeverity,
  val isResolved: Boolean = false
)

data class SosAlert(
  val id: String,
  val deviceId: String,
  val childName: String,
  val latitude: Double,
  val longitude: Double,
  val address: String,
  val timestamp: String,
  val batteryPercent: Int,
  val isSirenPlaying: Boolean,
  val isResolved: Boolean
)

data class WebHistoryRecord(
  val id: String,
  val deviceId: String,
  val title: String,
  val url: String,
  val domain: String,
  val visitCount: Int,
  val timestamp: String,
  val isBlocked: Boolean,
  val category: String
)

data class WebRestrictionCategory(
  val category: String,
  val isBlocked: Boolean,
  val blockedCount: Int
)

data class DrivingTrip(
  val id: String,
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

data class DevicePermissionHealth(
  val accessibilityService: Boolean = true,
  val screenCastService: Boolean = true,
  val notificationListener: Boolean = true,
  val cameraAndMic: Boolean = true,
  val locationAlways: Boolean = true,
  val deviceAdmin: Boolean = true,
  val batteryOptimizationDisabled: Boolean = true
)

// --- Central Command & Control Protocol ---
data class ChildCommand(
  val id: String,
  val type: String,
  val childDeviceId: String,
  val timestamp: Long,
  val payload: String = "{}"
)

data class CommandResult(
  val commandId: String,
  val status: String, // "SUCCESS", "ERROR", "TIMEOUT", "PERMISSION_DENIED"
  val timestamp: Long,
  val payload: String = "{}",
  val error: String? = null
)

// --- Secure Session & Token Management ---
data class AuthSessionToken(
  val token: String,
  val deviceId: String,
  val issuedAt: Long,
  val expiresAt: Long
)

// --- Downtime, Instant Block & Focus Mode Policies ---
data class DowntimePolicy(
  val id: String,
  val deviceId: String,
  val name: String,
  val startTime: String,
  val endTime: String,
  val daysOfWeek: String, // "Mon,Tue,Wed,Thu,Fri"
  val isEnabled: Boolean,
  val allowedAppPackages: List<String> = listOf("com.google.android.dialer", "com.google.android.apps.messaging")
)

data class InstantBlockPolicy(
  val deviceId: String,
  val isActive: Boolean,
  val blockUntilTimestamp: Long = 0L,
  val durationOption: String = "1_HOUR" // "1_HOUR", "2_HOURS", "MIDNIGHT", "MANUAL"
)

data class FocusModePolicy(
  val deviceId: String,
  val isActive: Boolean,
  val durationMinutes: Int,
  val startedAtTimestamp: Long,
  val blockedCount: Int = 0
)

// --- Social & AI Content Detection ---
enum class SafetyCategory {
  BULLYING, EXPLICIT_CONTENT, VIOLENCE, SELF_HARM, SUSPICIOUS_CONTACT, AI_SENSITIVE_PROMPT
}

data class KeywordRule(
  val id: String,
  val keyword: String,
  val category: SafetyCategory,
  val severity: AlertSeverity,
  val isRegex: Boolean = false
)

data class DetectionEvent(
  val id: String,
  val deviceId: String,
  val appName: String,
  val packageName: String,
  val keyword: String,
  val severity: AlertSeverity,
  val category: SafetyCategory,
  val contextSnippet: String,
  val timestamp: String,
  val isReviewed: Boolean = false
)

data class ImageDetectionRecord(
  val id: String,
  val deviceId: String,
  val imageUri: String,
  val riskCategory: String, // "SAFE", "QUESTIONABLE", "EXPLICIT", "VIOLENCE"
  val confidenceScore: Float,
  val timestamp: String,
  val flagged: Boolean
)

// --- Family Chat System ---
data class FamilyMember(
  val id: String,
  val name: String,
  val role: String, // "Parent", "Child", "Guardian"
  val isOnline: Boolean,
  val lastSeen: String = "Online now"
)

data class FamilyChatMessage(
  val id: String,
  val senderId: String,
  val senderName: String,
  val message: String,
  val timestamp: String,
  val isFromParent: Boolean,
  val status: String = "Delivered" // "Sent", "Delivered", "Read"
)

// --- Capability & Permission Discovery ---
data class CapabilityItem(
  val capability: String,
  val status: String, // "AVAILABLE", "PERMISSION_REQUIRED", "NOT_PERMITTED", "UNSUPPORTED"
  val description: String
)

// --- Offline Queue Entity for Reliable Synchronization ---
data class OfflineQueuedEvent(
  val id: String,
  val deviceId: String,
  val eventType: String,
  val payloadJson: String,
  val timestamp: Long,
  val isSynced: Boolean = false
)


