package com.example.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.network.ChildConnectionEngine
import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AirDroidRepository(
  context: Context,
  private val scope: CoroutineScope
) {
  private val appContext = context.applicationContext
  private var connectionEngine: ChildConnectionEngine? = null
  private var transferDevice: ChildDevice? = null
  private val database = AppDatabase.getDatabase(context, scope)
  private val deviceDao = database.deviceDao()
  private val notificationDao = database.notificationDao()
  private val smsDao = database.smsDao()
  private val contactDao = database.contactDao()
  private val callLogDao = database.callLogDao()
  private val transferDao = database.transferDao()
  private val locationDao = database.locationDao()
  private val geofenceDao = database.geofenceDao()
  private val managedAppDao = database.managedAppDao()
  private val timelineEventDao = database.timelineEventDao()
  private val childRequestDao = database.childRequestDao()
  private val deviceAlertDao = database.deviceAlertDao()
  private val webHistoryDao = database.webHistoryDao()
  private val drivingTripDao = database.drivingTripDao()
  private val downtimePolicyDao = database.downtimePolicyDao()
  private val instantBlockDao = database.instantBlockDao()
  private val detectionEventDao = database.detectionEventDao()
  private val imageDetectionDao = database.imageDetectionDao()
  private val familyMemberDao = database.familyMemberDao()
  private val familyChatMessageDao = database.familyChatMessageDao()
  private val offlineQueueDao = database.offlineQueueDao()

  val devices: Flow<List<ChildDevice>> = deviceDao.getAllDevices().map { entities ->
    entities.map { it.toModel() }
  }

  suspend fun saveRealChildDevice(device: ChildDevice, displayName: String = device.name): ChildDevice {
    val entity = DeviceEntity(
      id = device.id, name = displayName, model = device.model, osVersion = device.osVersion,
      batteryPercent = device.batteryPercent, isCharging = device.isCharging, storageUsedGb = device.storageUsedGb,
      storageTotalGb = device.storageTotalGb, isOnline = device.isOnline, wifiSsid = device.wifiSsid,
      wifiSignalDbm = device.wifiSignalDbm, ipAddress = device.ipAddress, connectionMode = device.connectionMode.name,
      lastSeen = device.lastSeen, authToken = device.authToken
    )
    deviceDao.insertOrUpdate(entity)
    return entity.toModel()
  }

  suspend fun updateDeviceRuntimeState(device: ChildDevice) {
    val existing = deviceDao.getDeviceById(device.id) ?: return
    deviceDao.update(existing.copy(
      model = device.model, osVersion = device.osVersion, batteryPercent = device.batteryPercent,
      isCharging = device.isCharging, storageUsedGb = device.storageUsedGb, storageTotalGb = device.storageTotalGb,
      isOnline = device.isOnline, wifiSsid = device.wifiSsid, wifiSignalDbm = device.wifiSignalDbm,
      lastSeen = device.lastSeen, ipAddress = device.ipAddress
    ))
  }

  suspend fun updateDeviceConnectionMode(deviceId: String, mode: ConnectionMode) {
    val existing = deviceDao.getDeviceById(deviceId) ?: return
    deviceDao.update(existing.copy(connectionMode = mode.name))
  }

  // --- Real File Explorer System ---
  private val _localFiles = MutableStateFlow<List<FileItem>>(emptyList())
  val localFiles: StateFlow<List<FileItem>> = _localFiles.asStateFlow()

  private val _remoteFiles = MutableStateFlow<List<FileItem>>(emptyList())
  val remoteFiles: StateFlow<List<FileItem>> = _remoteFiles.asStateFlow()

  private val _activeTransfers = MutableStateFlow<List<TransferTask>>(emptyList())
  val activeTransfers: StateFlow<List<TransferTask>> = _activeTransfers.asStateFlow()

  init { refreshLocalFiles() }

  fun bindConnectionEngine(engine: ChildConnectionEngine) { connectionEngine = engine }
  fun setTransferDevice(device: ChildDevice?) { transferDevice = device }

  fun replaceRemoteFiles(files: List<FileItem>) { _remoteFiles.value = files }

  fun refreshLocalFiles(path: String? = null) {
    val root = File(path ?: (appContext.getExternalFilesDir(null)?.absolutePath ?: appContext.filesDir.absolutePath))
    _localFiles.value = listFiles(root, "pc-local", true)
  }

  private fun listFiles(dir: File, deviceId: String, isLocal: Boolean): List<FileItem> {
    if (!dir.exists() || !dir.isDirectory) return emptyList()
    return dir.listFiles()?.map { file ->
      FileItem(
        id = file.absolutePath, deviceId = deviceId, name = file.name, path = file.absolutePath,
        isDirectory = file.isDirectory, sizeBytes = if (file.isFile) file.length() else 0L,
        modifiedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified())),
        fileType = when {
          file.isDirectory -> FileType.FOLDER
          file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) || file.name.endsWith(".png", true) -> FileType.IMAGE
          file.name.endsWith(".mp4", true) || file.name.endsWith(".mkv", true) -> FileType.VIDEO
          file.name.endsWith(".mp3", true) || file.name.endsWith(".wav", true) -> FileType.AUDIO
          file.name.endsWith(".apk", true) -> FileType.APK
          file.name.endsWith(".zip", true) || file.name.endsWith(".rar", true) -> FileType.ARCHIVE
          file.name.endsWith(".pdf", true) || file.name.endsWith(".doc", true) || file.name.endsWith(".docx", true) -> FileType.DOCUMENT
          else -> FileType.OTHER
        },
        isLocal = isLocal
      )
    }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
  }

  fun uploadFileToMobile(localFile: FileItem, targetMobilePath: String) {
    val device = repositoryDeviceForTransfer() ?: return
    val engine = connectionEngine ?: return
    val taskId = UUID.randomUUID().toString().take(8)
    val task = TransferTask(taskId, localFile.name, localFile.sizeBytes, 0f, 0.0, true, "Uploading...")
    _activeTransfers.update { listOf(task) + it }
    scope.launch {
      val ok = engine.uploadFile(device, File(localFile.path), targetMobilePath) { progress, speed ->
        _activeTransfers.update { tasks -> tasks.map { t -> if (t.id == taskId) t.copy(progress = progress, speedKbps = speed) else t } }
      }
      _activeTransfers.update { tasks -> tasks.map { t -> if (t.id == taskId) t.copy(progress = if (ok) 1f else t.progress, status = if (ok) "Completed" else "Failed") else t } }
    }
  }

  fun downloadFileToPC(remoteFile: FileItem) {
    val device = repositoryDeviceForTransfer() ?: return
    val engine = connectionEngine ?: return
    val baseDir = appContext.getExternalFilesDir("downloads") ?: appContext.filesDir
    if (!baseDir.exists()) baseDir.mkdirs()
    val destination = File(baseDir, remoteFile.name)
    val taskId = UUID.randomUUID().toString().take(8)
    _activeTransfers.update { listOf(TransferTask(taskId, remoteFile.name, remoteFile.sizeBytes, 0f, 0.0, false, "Downloading...")) + it }
    scope.launch {
      val ok = engine.downloadFile(device, remoteFile.path, destination) { progress, speed ->
        _activeTransfers.update { tasks -> tasks.map { t -> if (t.id == taskId) t.copy(progress = progress, speedKbps = speed) else t } }
      }
      if (ok) refreshLocalFiles(baseDir.absolutePath)
      _activeTransfers.update { tasks -> tasks.map { t -> if (t.id == taskId) t.copy(progress = if (ok) 1f else t.progress, status = if (ok) "Completed" else "Failed") else t } }
    }
  }

  private fun repositoryDeviceForTransfer(): ChildDevice? = transferDevice

  fun createFolder(isLocal: Boolean, currentPath: String, folderName: String) {
    if (folderName.isBlank()) return
    if (isLocal) {
      val dir = File(currentPath, folderName)
      if (dir.mkdirs()) refreshLocalFiles(currentPath)
    } else {
      val device = repositoryDeviceForTransfer() ?: return
      val engine = connectionEngine ?: return
      scope.launch { engine.createRemoteFolder(device, currentPath, folderName) }
    }
  }

  fun deleteFile(file: FileItem) {
    if (file.isLocal) {
      if (File(file.path).deleteRecursively()) refreshLocalFiles(File(file.path).parent ?: file.path)
    } else {
      val device = repositoryDeviceForTransfer() ?: return
      val engine = connectionEngine ?: return
      scope.launch { engine.deleteRemoteFile(device, file.path) }
    }
  }

  fun renameFile(file: FileItem, newName: String) {
    if (newName.isBlank()) return
    if (file.isLocal) {
      val source = File(file.path)
      val target = File(source.parentFile, newName)
      if (source.renameTo(target)) refreshLocalFiles(source.parent ?: file.path)
    } else {
      val device = repositoryDeviceForTransfer() ?: return
      val engine = connectionEngine ?: return
      scope.launch { engine.renameRemoteFile(device, file.path, newName) }
    }
  }

  // --- Notifications ---
  val notifications: Flow<List<RemoteNotification>> = notificationDao.getAllNotifications().map { entities ->
    entities.map { it.toModel() }
  }

  suspend fun markNotificationRead(id: String) {
    notificationDao.markAsRead(id)
  }

  suspend fun replyToNotification(notificationId: String, replyText: String) {
    val notif = notificationDao.getAllNotifications().first().find { it.id == notificationId } ?: return
    val currentReplies = parseReplies(notif.repliesJson) + replyText
    notificationDao.updateReplies(notificationId, repliesToJson(currentReplies))
    notificationDao.markAsRead(notificationId)
  }

  suspend fun dismissNotification(id: String) {
    notificationDao.deleteById(id)
  }

  // --- SMS ---
  val smsMessages: Flow<List<SmsMessage>> = smsDao.getAllMessages().map { entities ->
    entities.map { it.toModel() }
  }


  // --- Contacts ---
  val contacts: Flow<List<Contact>> = contactDao.getAllContacts().map { entities ->
    entities.map { it.toModel() }
  }

  suspend fun addContact(deviceId: String, name: String, phone: String, email: String, label: String) {
    contactDao.insert(
      ContactEntity(
        id = "c-${UUID.randomUUID().toString().take(6)}",
        deviceId = deviceId,
        name = name,
        phone = phone,
        email = email,
        label = label
      )
    )
  }

  suspend fun deleteContact(contactId: String) {
    contactDao.getAllContacts().first().find { it.id == contactId }?.let {
      contactDao.delete(it)
    }
  }

  // --- Call Logs ---
  val callLogs: Flow<List<CallLog>> = callLogDao.getAllCallLogs().map { entities ->
    entities.map { it.toModel() }
  }

  // Helper mappings
  private fun DeviceEntity.toModel(): ChildDevice {
    val mode = try {
      ConnectionMode.valueOf(connectionMode)
    } catch (e: Exception) {
      ConnectionMode.LOCAL_P2P
    }
    return ChildDevice(
      id = id,
      name = name,
      model = model,
      osVersion = osVersion,
      batteryPercent = batteryPercent,
      isCharging = isCharging,
      storageUsedGb = storageUsedGb,
      storageTotalGb = storageTotalGb,
      isOnline = isOnline,
      wifiSsid = wifiSsid,
      wifiSignalDbm = wifiSignalDbm,
      ipAddress = ipAddress,
      connectionMode = mode,
      lastSeen = lastSeen,
      authToken = authToken
    )
  }

  private fun NotificationEntity.toModel(): RemoteNotification {
    return RemoteNotification(
      id = id,
      deviceId = deviceId,
      appName = appName,
      packageName = packageName,
      title = title,
      content = content,
      time = time,
      isRead = isRead,
      replies = parseReplies(repliesJson)
    )
  }

  private fun SmsEntity.toModel(): SmsMessage {
    return SmsMessage(
      id = id,
      threadId = threadId,
      isFromMe = isFromMe,
      text = text,
      time = time,
      status = status
    )
  }

  private fun ContactEntity.toModel(): Contact {
    return Contact(
      id = id,
      deviceId = deviceId,
      name = name,
      phone = phone,
      email = email,
      label = label
    )
  }

  private fun CallLogEntity.toModel(): CallLog {
    val type = try {
      CallType.valueOf(callType)
    } catch (e: Exception) {
      CallType.INCOMING
    }
    return CallLog(
      id = id,
      deviceId = deviceId,
      contactName = contactName,
      phoneNumber = phoneNumber,
      callType = type,
      time = time,
      durationSeconds = durationSeconds
    )
  }

  private fun parseReplies(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    return json.removeSurrounding("[", "]")
      .split(",")
      .map { it.trim().removeSurrounding("\"") }
      .filter { it.isNotBlank() }
  }

  private fun repliesToJson(replies: List<String>): String {
    return "[" + replies.joinToString(",") { "\"$it\"" } + "]"
  }


  // --- Location & Geofence Streams ---
  suspend fun insertLocation(location: ChildLocation) {
    locationDao.insertOrUpdate(
      LocationEntity(
        deviceId = location.deviceId,
        latitude = location.latitude,
        longitude = location.longitude,
        address = location.address,
        accuracyMeters = location.accuracyMeters,
        timestamp = location.timestamp,
        batteryAtLocation = location.batteryAtLocation,
        isMoving = location.isMoving
      )
    )
  }

  fun getLocationForDevice(deviceId: String): Flow<ChildLocation?> {
    return locationDao.getLocationForDevice(deviceId).map { entity ->
      entity?.let {
        ChildLocation(
          deviceId = it.deviceId,
          latitude = it.latitude,
          longitude = it.longitude,
          address = it.address,
          accuracyMeters = it.accuracyMeters,
          timestamp = it.timestamp,
          batteryAtLocation = it.batteryAtLocation,
          isMoving = it.isMoving
        )
      }
    }
  }

  fun getGeofencesForDevice(deviceId: String): Flow<List<Geofence>> {
    return geofenceDao.getGeofencesForDevice(deviceId).map { list ->
      list.map {
        Geofence(
          id = it.id,
          deviceId = it.deviceId,
          name = it.name,
          address = it.address,
          latitude = it.latitude,
          longitude = it.longitude,
          radiusMeters = it.radiusMeters,
          isTriggerOnEnter = it.isTriggerOnEnter,
          isTriggerOnExit = it.isTriggerOnExit,
          isCurrentlyInside = it.isCurrentlyInside
        )
      }
    }
  }

  suspend fun addGeofence(geofence: Geofence) {
    geofenceDao.insert(
      GeofenceEntity(
        id = geofence.id,
        deviceId = geofence.deviceId,
        name = geofence.name,
        address = geofence.address,
        latitude = geofence.latitude,
        longitude = geofence.longitude,
        radiusMeters = geofence.radiusMeters,
        isTriggerOnEnter = geofence.isTriggerOnEnter,
        isTriggerOnExit = geofence.isTriggerOnExit,
        isCurrentlyInside = geofence.isCurrentlyInside
      )
    )
  }

  suspend fun deleteGeofence(geofence: Geofence) {
    geofenceDao.delete(
      GeofenceEntity(
        id = geofence.id,
        deviceId = geofence.deviceId,
        name = geofence.name,
        address = geofence.address,
        latitude = geofence.latitude,
        longitude = geofence.longitude,
        radiusMeters = geofence.radiusMeters,
        isTriggerOnEnter = geofence.isTriggerOnEnter,
        isTriggerOnExit = geofence.isTriggerOnExit,
        isCurrentlyInside = geofence.isCurrentlyInside
      )
    )
  }

  // --- Managed Apps ---
  fun getAppsForDevice(deviceId: String): Flow<List<ManagedApp>> {
    return managedAppDao.getAppsForDevice(deviceId).map { list ->
      list.map {
        ManagedApp(
          packageName = it.packageName,
          deviceId = it.deviceId,
          appName = it.appName,
          category = it.category,
          usageMinutesToday = it.usageMinutesToday,
          isBlocked = it.isBlocked,
          dailyLimitMinutes = it.dailyLimitMinutes,
          isAlwaysAllowed = it.isAlwaysAllowed,
          installDate = it.installDate
        )
      }
    }
  }

  suspend fun setAppBlocked(packageName: String, deviceId: String, isBlocked: Boolean) {
    managedAppDao.updateBlockStatus(packageName, deviceId, isBlocked)
  }

  suspend fun setAppDailyLimit(packageName: String, deviceId: String, limitMinutes: Int) {
    managedAppDao.updateDailyLimit(packageName, deviceId, limitMinutes)
  }

  suspend fun setAppAlwaysAllowed(packageName: String, deviceId: String, allowed: Boolean) {
    managedAppDao.updateAlwaysAllowed(packageName, deviceId, allowed)
  }

  suspend fun replaceManagedApps(apps: List<ManagedApp>) {
    managedAppDao.insertAll(apps.map { app ->
      ManagedAppEntity(
        packageName = app.packageName, deviceId = app.deviceId, appName = app.appName, category = app.category,
        usageMinutesToday = app.usageMinutesToday, isBlocked = app.isBlocked, dailyLimitMinutes = app.dailyLimitMinutes,
        isAlwaysAllowed = app.isAlwaysAllowed, installDate = app.installDate
      )
    })
  }

  // --- Timeline Events ---
  fun getTimelineEventsForDevice(deviceId: String): Flow<List<ActivityTimelineEvent>> {
    return timelineEventDao.getEventsForDevice(deviceId).map { list ->
      list.map {
        ActivityTimelineEvent(
          id = it.id,
          deviceId = it.deviceId,
          title = it.title,
          description = it.description,
          time = it.time,
          category = it.category,
          iconType = it.iconType
        )
      }
    }
  }

  // --- Child Requests ---
  fun getRequestsForDevice(deviceId: String): Flow<List<ChildAppRequest>> {
    return childRequestDao.getRequestsForDevice(deviceId).map { list ->
      list.map {
        val st = when (it.status) {
          "APPROVED" -> RequestStatus.APPROVED
          "REJECTED" -> RequestStatus.REJECTED
          else -> RequestStatus.PENDING
        }
        ChildAppRequest(
          id = it.id,
          deviceId = it.deviceId,
          childName = it.childName,
          appName = it.appName,
          packageName = it.packageName,
          requestedMinutes = it.requestedMinutes,
          requestType = it.requestType,
          reason = it.reason,
          timestamp = it.timestamp,
          status = st
        )
      }
    }
  }

  suspend fun respondToRequest(requestId: String, approved: Boolean) {
    childRequestDao.updateStatus(requestId, if (approved) "APPROVED" else "REJECTED")
  }

  // --- Device Alerts ---
  fun getAlertsForDevice(deviceId: String): Flow<List<DeviceAlert>> {
    return deviceAlertDao.getAlertsForDevice(deviceId).map { list ->
      list.map {
        val sev = when (it.severity) {
          "CRITICAL" -> AlertSeverity.CRITICAL
          "HIGH" -> AlertSeverity.HIGH
          "MEDIUM" -> AlertSeverity.MEDIUM
          else -> AlertSeverity.LOW
        }
        DeviceAlert(
          id = it.id,
          deviceId = it.deviceId,
          title = it.title,
          message = it.message,
          alertType = it.alertType,
          timestamp = it.timestamp,
          severity = sev,
          isResolved = it.isResolved
        )
      }
    }
  }

  suspend fun resolveAlert(alertId: String) {
    deviceAlertDao.resolveAlert(alertId)
  }

  suspend fun insertAlert(devId: String, title: String, message: String, type: String, severity: AlertSeverity) {
    deviceAlertDao.insert(
      DeviceAlertEntity(
        id = "alt-${UUID.randomUUID().toString().take(8)}",
        deviceId = devId,
        title = title,
        message = message,
        timestamp = "Just now",
        alertType = type,
        severity = severity.name,
        isResolved = false
      )
    )
  }

  // --- Web History ---
  fun getWebHistoryForDevice(deviceId: String): Flow<List<WebHistoryRecord>> {
    return webHistoryDao.getWebHistoryForDevice(deviceId).map { list ->
      list.map {
        WebHistoryRecord(
          id = it.id,
          deviceId = it.deviceId,
          title = it.title,
          url = it.url,
          domain = it.domain,
          visitCount = it.visitCount,
          timestamp = it.timestamp,
          isBlocked = it.isBlocked,
          category = it.category
        )
      }
    }
  }

  suspend fun setDomainBlocked(domain: String, deviceId: String, isBlocked: Boolean) {
    webHistoryDao.updateDomainBlockStatus(domain, deviceId, isBlocked)
  }

  // --- Driving Trips ---
  fun getDrivingTripsForDevice(deviceId: String): Flow<List<DrivingTrip>> {
    return drivingTripDao.getTripsForDevice(deviceId).map { list ->
      list.map {
        DrivingTrip(
          id = it.id,
          deviceId = it.deviceId,
          date = it.date,
          startTime = it.startTime,
          endTime = it.endTime,
          origin = it.origin,
          destination = it.destination,
          distanceMiles = it.distanceMiles,
          durationMinutes = it.durationMinutes,
          topSpeedMph = it.topSpeedMph,
          avgSpeedMph = it.avgSpeedMph,
          hardBrakingEvents = it.hardBrakingEvents,
          rapidAccels = it.rapidAccels,
          phoneUsageMinutes = it.phoneUsageMinutes,
          safetyScore = it.safetyScore
        )
      }
    }
  }

  suspend fun removeDevice(deviceId: String) {
    val dev = deviceDao.getDeviceById(deviceId)
    if (dev != null) {
      deviceDao.delete(dev)
    }
  }

  suspend fun renameDevice(deviceId: String, newName: String) {
    val dev = deviceDao.getDeviceById(deviceId)
    if (dev != null) {
      deviceDao.update(dev.copy(name = newName))
    }
  }

  // --- Downtime Policies ---
  fun getDowntimePolicies(deviceId: String): Flow<List<DowntimePolicy>> {
    return downtimePolicyDao.getPoliciesForDevice(deviceId).map { entities ->
      entities.map {
        DowntimePolicy(
          id = it.id,
          deviceId = it.deviceId,
          name = it.name,
          startTime = it.startTime,
          endTime = it.endTime,
          daysOfWeek = it.daysOfWeek,
          isEnabled = it.isEnabled,
          allowedAppPackages = listOf("com.google.android.dialer", "com.google.android.apps.messaging")
        )
      }
    }
  }

  suspend fun setDowntimeEnabled(id: String, isEnabled: Boolean) {
    downtimePolicyDao.setEnabled(id, isEnabled)
  }

  suspend fun saveDowntimePolicy(policy: DowntimePolicy) {
    downtimePolicyDao.insertOrUpdate(
      DowntimePolicyEntity(
        id = policy.id,
        deviceId = policy.deviceId,
        name = policy.name,
        startTime = policy.startTime,
        endTime = policy.endTime,
        daysOfWeek = policy.daysOfWeek,
        isEnabled = policy.isEnabled,
        allowedAppsJson = "[\"com.google.android.dialer\", \"com.google.android.apps.messaging\"]"
      )
    )
  }

  // --- Instant Block ---
  fun getInstantBlock(deviceId: String): Flow<InstantBlockPolicy?> {
    return instantBlockDao.getInstantBlock(deviceId).map { entity ->
      entity?.let {
        InstantBlockPolicy(
          deviceId = it.deviceId,
          isActive = it.isActive,
          blockUntilTimestamp = it.blockUntilTimestamp,
          durationOption = it.durationOption
        )
      }
    }
  }

  suspend fun setInstantBlock(deviceId: String, isActive: Boolean, blockDurationMinutes: Int, durationOption: String) {
    val until = if (isActive && blockDurationMinutes > 0) System.currentTimeMillis() + (blockDurationMinutes * 60 * 1000L) else 0L
    instantBlockDao.setInstantBlock(
      InstantBlockEntity(
        deviceId = deviceId,
        isActive = isActive,
        blockUntilTimestamp = until,
        durationOption = durationOption
      )
    )
  }

  // --- Social & AI Detection Events ---
  fun getDetectionEvents(deviceId: String): Flow<List<DetectionEvent>> {
    return detectionEventDao.getEventsForDevice(deviceId).map { entities ->
      entities.map {
        val cat = try { SafetyCategory.valueOf(it.category) } catch (e: Exception) { SafetyCategory.BULLYING }
        val sev = try { AlertSeverity.valueOf(it.severity) } catch (e: Exception) { AlertSeverity.HIGH }
        DetectionEvent(
          id = it.id,
          deviceId = it.deviceId,
          appName = it.appName,
          packageName = it.packageName,
          keyword = it.keyword,
          severity = sev,
          category = cat,
          contextSnippet = it.contextSnippet,
          timestamp = it.timestamp,
          isReviewed = it.isReviewed
        )
      }
    }
  }

  suspend fun insertDetectionEvent(event: DetectionEvent) {
    detectionEventDao.insert(
      DetectionEventEntity(
        id = event.id,
        deviceId = event.deviceId,
        appName = event.appName,
        packageName = event.packageName,
        keyword = event.keyword,
        severity = event.severity.name,
        category = event.category.name,
        contextSnippet = event.contextSnippet,
        timestamp = event.timestamp,
        isReviewed = event.isReviewed
      )
    )
  }

  suspend fun markDetectionReviewed(id: String) {
    detectionEventDao.markReviewed(id)
  }

  // --- Image Detections ---
  fun getImageDetections(deviceId: String): Flow<List<ImageDetectionRecord>> {
    return imageDetectionDao.getDetectionsForDevice(deviceId).map { list ->
      list.map {
        ImageDetectionRecord(
          id = it.id,
          deviceId = it.deviceId,
          imageUri = it.imageUri,
          riskCategory = it.riskCategory,
          confidenceScore = it.confidenceScore,
          timestamp = it.timestamp,
          flagged = it.flagged
        )
      }
    }
  }

  suspend fun insertImageDetection(record: ImageDetectionRecord) {
    imageDetectionDao.insert(
      ImageDetectionEntity(
        id = record.id,
        deviceId = record.deviceId,
        imageUri = record.imageUri,
        riskCategory = record.riskCategory,
        confidenceScore = record.confidenceScore,
        timestamp = record.timestamp,
        flagged = record.flagged
      )
    )
  }

  // --- Family Members & Chat ---
  fun getFamilyMembers(): Flow<List<FamilyMember>> {
    return familyMemberDao.getAllMembers().map { members ->
      members.map {
        FamilyMember(
          id = it.id,
          name = it.name,
          role = it.role,
          isOnline = it.isOnline,
          lastSeen = it.lastSeen
        )
      }
    }
  }

  fun getFamilyChatMessages(): Flow<List<FamilyChatMessage>> {
    return familyChatMessageDao.getAllMessages().map { list ->
      list.map {
        FamilyChatMessage(
          id = it.id,
          senderId = it.senderId,
          senderName = it.senderName,
          message = it.message,
          timestamp = it.timestamp,
          isFromParent = it.isFromParent,
          status = it.status
        )
      }
    }
  }

  suspend fun sendFamilyChatMessage(message: FamilyChatMessage) {
    familyChatMessageDao.insert(
      FamilyChatMessageEntity(
        id = message.id,
        senderId = message.senderId,
        senderName = message.senderName,
        message = message.message,
        timestamp = message.timestamp,
        isFromParent = message.isFromParent,
        status = message.status
      )
    )
  }

  // --- Offline Synchronization Queue ---
  fun getPendingOfflineEvents(): Flow<List<OfflineQueuedEvent>> {
    return offlineQueueDao.getPendingEvents().map { list ->
      list.map {
        OfflineQueuedEvent(
          id = it.id,
          deviceId = it.deviceId,
          eventType = it.eventType,
          payloadJson = it.payloadJson,
          timestamp = it.timestamp,
          isSynced = it.isSynced
        )
      }
    }
  }

  suspend fun enqueueOfflineEvent(event: OfflineQueuedEvent) {
    offlineQueueDao.enqueue(
      OfflineQueueEntity(
        id = event.id,
        deviceId = event.deviceId,
        eventType = event.eventType,
        payloadJson = event.payloadJson,
        timestamp = event.timestamp,
        isSynced = event.isSynced
      )
    )
  }

  suspend fun markOfflineEventSynced(id: String) {
    offlineQueueDao.markSynced(id)
  }
}
