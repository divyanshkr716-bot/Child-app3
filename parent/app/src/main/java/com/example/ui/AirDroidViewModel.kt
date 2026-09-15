package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AirDroidRepository
import com.example.data.AppLogger
import com.example.data.model.*
import com.example.data.network.ChildConnectionEngine
import com.example.data.network.ParentPairingServer
import com.example.data.safety.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class TouchPoint(val x: Float, val y: Float, val timestamp: Long = System.currentTimeMillis())

data class AirMirrorUiState(
  val isFullscreen: Boolean = false,
  val lastInputEvent: String = "Ready",
  val touchPoints: List<TouchPoint> = emptyList(),
  val remoteInputFieldText: String = "",
  val currentFrame: ImageBitmap? = null,
  val isStreaming: Boolean = false,
  val permissionError: String? = null
)

data class RemoteCameraUiState(
  val isStreaming: Boolean = false,
  val isFrontCamera: Boolean = false,
  val isFlashlightOn: Boolean = false,
  val isAudioOn: Boolean = true,
  val isRecording: Boolean = false,
  val recordSeconds: Int = 0,
  val capturedPhotos: List<String> = emptyList(),
  val currentFrame: ImageBitmap? = null,
  val permissionError: String? = null
)

class AirDroidViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = AirDroidRepository(application, viewModelScope)


  val devices: StateFlow<List<ChildDevice>> = repository.devices
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _selectedDeviceId = MutableStateFlow("")
  val selectedDeviceId: StateFlow<String> = _selectedDeviceId.asStateFlow()

  val selectedDevice: StateFlow<ChildDevice?> = combine(devices, selectedDeviceId) { list, id ->
    list.find { it.id == id } ?: list.firstOrNull()
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private val _activeTab = MutableStateFlow(ActiveTab.DASHBOARD)
  val activeTab: StateFlow<ActiveTab> = _activeTab.asStateFlow()

  private val _isDarkTheme = MutableStateFlow(true)
  val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

  // Real-time latency & FPS monitoring
  private val _networkLatencyMs = MutableStateFlow(0)
  val networkLatencyMs: StateFlow<Int> = _networkLatencyMs.asStateFlow()

  private val _fpsCounter = MutableStateFlow(0)
  val fpsCounter: StateFlow<Int> = _fpsCounter.asStateFlow()

  // Real-time Notification toaster
  private val _activeToaster = MutableStateFlow<RemoteNotification?>(null)
  val activeToaster: StateFlow<RemoteNotification?> = _activeToaster.asStateFlow()

  // AirMirror State
  private val _airMirrorState = MutableStateFlow(AirMirrorUiState())
  val airMirrorState: StateFlow<AirMirrorUiState> = _airMirrorState.asStateFlow()

  // Camera State
  private val _cameraState = MutableStateFlow(RemoteCameraUiState())
  val cameraState: StateFlow<RemoteCameraUiState> = _cameraState.asStateFlow()

  // Files
  val localFiles: StateFlow<List<FileItem>> = repository.localFiles
  val remoteFiles: StateFlow<List<FileItem>> = repository.remoteFiles
  val activeTransfers: StateFlow<List<TransferTask>> = repository.activeTransfers

  private val _localCurrentPath = MutableStateFlow(application.getExternalFilesDir(null)?.absolutePath ?: application.filesDir.absolutePath)
  val localCurrentPath: StateFlow<String> = _localCurrentPath.asStateFlow()

  private val _remoteCurrentPath = MutableStateFlow("/storage/emulated/0")
  val remoteCurrentPath: StateFlow<String> = _remoteCurrentPath.asStateFlow()

  private val _fileSearchQuery = MutableStateFlow("")
  val fileSearchQuery: StateFlow<String> = _fileSearchQuery.asStateFlow()

  private val _isGalleryMode = MutableStateFlow(false)
  val isGalleryMode: StateFlow<Boolean> = _isGalleryMode.asStateFlow()

  // Notifications & SMS
  val notifications: StateFlow<List<RemoteNotification>> = repository.notifications
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val smsMessages: StateFlow<List<SmsMessage>> = repository.smsMessages
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val contacts: StateFlow<List<Contact>> = repository.contacts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val callLogs: StateFlow<List<CallLog>> = repository.callLogs
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _selectedSmsThread = MutableStateFlow("thread-mom")
  val selectedSmsThread: StateFlow<String> = _selectedSmsThread.asStateFlow()

  private val _smsDraft = MutableStateFlow("")
  val smsDraft: StateFlow<String> = _smsDraft.asStateFlow()

  // Real Connection Engine
  val connectionEngine = ChildConnectionEngine(viewModelScope)
  init { repository.bindConnectionEngine(connectionEngine) }
  val connectionStatus: StateFlow<ConnectionStateStatus> = connectionEngine.connectionStatus
  val lastAckMessage: StateFlow<String> = connectionEngine.lastAckMessage
  val audioVolumeDb: StateFlow<Float> = connectionEngine.audioVolumeDb

  private val _isOneWayAudioActive = MutableStateFlow(false)
  val isOneWayAudioActive: StateFlow<Boolean> = _isOneWayAudioActive.asStateFlow()

  // Feature Flows (Mapped to Selected Device)
  @OptIn(ExperimentalCoroutinesApi::class)
  val childLocation: StateFlow<ChildLocation?> = _selectedDeviceId.flatMapLatest { id ->
    repository.getLocationForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  @OptIn(ExperimentalCoroutinesApi::class)
  val geofences: StateFlow<List<Geofence>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getGeofencesForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val managedApps: StateFlow<List<ManagedApp>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getAppsForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val timelineEvents: StateFlow<List<ActivityTimelineEvent>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getTimelineEventsForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val childRequests: StateFlow<List<ChildAppRequest>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getRequestsForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val deviceAlerts: StateFlow<List<DeviceAlert>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getAlertsForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val webHistory: StateFlow<List<WebHistoryRecord>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getWebHistoryForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val drivingTrips: StateFlow<List<DrivingTrip>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getDrivingTripsForDevice(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val sosAlert = MutableStateFlow<SosAlert?>(null)

  val devicePermissionHealth = MutableStateFlow(DevicePermissionHealth())

  // Safety & Monitoring Engines
  val keywordRuleEngine = KeywordRuleEngine()
  val socialContentEngine = SocialContentEngine(keywordRuleEngine)
  val aiContentEngine = AIContentEngine(keywordRuleEngine)
  val imageEngine = InappropriateImageEngine()

  // Downtime, Instant Block & Focus Mode
  @OptIn(ExperimentalCoroutinesApi::class)
  val downtimePolicies: StateFlow<List<DowntimePolicy>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getDowntimePolicies(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val instantBlockPolicy: StateFlow<InstantBlockPolicy?> = _selectedDeviceId.flatMapLatest { id ->
    repository.getInstantBlock(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private val _focusModePolicy = MutableStateFlow(FocusModePolicy("", false, 45, 0L))
  val focusModePolicy: StateFlow<FocusModePolicy> = _focusModePolicy.asStateFlow()

  // Social & AI Detection Events
  @OptIn(ExperimentalCoroutinesApi::class)
  val detectionEvents: StateFlow<List<DetectionEvent>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getDetectionEvents(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val imageDetections: StateFlow<List<ImageDetectionRecord>> = _selectedDeviceId.flatMapLatest { id ->
    repository.getImageDetections(id)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _pairingStatusMessage = MutableStateFlow("")
  val pairingStatusMessage: StateFlow<String> = _pairingStatusMessage.asStateFlow()

  // Family Members & Chat
  val familyMembers: StateFlow<List<FamilyMember>> = repository.getFamilyMembers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val familyChatMessages: StateFlow<List<FamilyChatMessage>> = repository.getFamilyChatMessages()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val pendingOfflineEvents: StateFlow<List<OfflineQueuedEvent>> = repository.getPendingOfflineEvents()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Local parent session; cloud authentication can be added when a real identity backend is configured.
  val userSession: StateFlow<UserSession> = MutableStateFlow(UserSession())

  // Pairing state must be declared before the server callback uses it.
  private val _pairingCode = MutableStateFlow("")
  val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

  private val parentPairingServer = ParentPairingServer(application, { reg ->
    viewModelScope.launch {
      val device = ChildDevice(reg.id, reg.name, reg.model.ifBlank { "Android" }, reg.os.ifBlank { "Android" }, 0, false, 0.0, 0.0, true, "", 0, "", ConnectionMode.REMOTE_CLOUD, "Online now", reg.parentToken)
      val saved = repository.saveRealChildDevice(device, reg.name)
      _selectedDeviceId.value = saved.id
      connectionEngine.startMonitoringDevice(saved)
      _pairingStatusMessage.value = "Child connected: ${reg.name}"
      _activeTab.value = ActiveTab.DASHBOARD
    }
  }, { code ->
    _pairingCode.value = code
  })

  val pairingServerIp: String get() = "Cloud relay"

  init {
    _pairingCode.value = parentPairingServer.pairingCode()
    parentPairingServer.start()
  }

  override fun onCleared() {
    parentPairingServer.stop()
    connectionEngine.stopMonitoring()
    super.onCleared()
  }

  private var recordingJob: Job? = null
  private var cameraStreamJob: Job? = null
  private var screenStreamJob: Job? = null

  init {
    // Collect real latency & FPS from actual network connection without simulation
    viewModelScope.launch {
      connectionEngine.lastMeasuredLatencyMs.collect { latency ->
        _networkLatencyMs.value = latency
      }
    }
    viewModelScope.launch {
      connectionEngine.measuredFps.collect { fps ->
        _fpsCounter.value = fps
      }
    }
    viewModelScope.launch {
      selectedDevice.collectLatest { dev ->
        if (dev != null) {
          repository.setTransferDevice(dev)
          connectionEngine.startMonitoringDevice(dev)
          refreshDeviceCapabilities()
        }
      }
    }
  }

  fun setTab(tab: ActiveTab) {
    _activeTab.value = tab
    if (tab == ActiveTab.APP_MANAGEMENT) {
      viewModelScope.launch {
        selectedDevice.value?.let { dev -> repository.replaceManagedApps(connectionEngine.fetchChildApps(dev)) }
      }
    }
    if (tab == ActiveTab.REMOTE_CAMERA) {
      startCameraStream()
    } else {
      stopCameraStream()
    }
    if (tab == ActiveTab.AIR_MIRROR) {
      startScreenStream()
    } else {
      stopScreenStream()
    }
  }

  fun startCameraStream() {
    val dev = selectedDevice.value ?: return
    cameraStreamJob?.cancel()
    _cameraState.update { it.copy(isStreaming = true, permissionError = null) }
    viewModelScope.launch {
      val started = connectionEngine.sendCameraControl(dev, "START_CAMERA")
      if (!started) {
        _cameraState.update { it.copy(isStreaming = false, permissionError = "Child camera could not be started. Check camera permission.") }
        return@launch
      }
    }
    cameraStreamJob = connectionEngine.streamChildFrames(
      device = dev, endpoint = "/api/camera/frame",
      onFrameReceived = { bitmap ->
        _cameraState.update { it.copy(currentFrame = bitmap.asImageBitmap(), permissionError = null) }
      },
      onError = { err ->
        _cameraState.update { it.copy(permissionError = err) }
      }
    )
  }

  fun stopCameraStream() {
    cameraStreamJob?.cancel()
    cameraStreamJob = null
    _cameraState.update { it.copy(isStreaming = false) }
  }

  fun startScreenStream() {
    val dev = selectedDevice.value ?: return
    screenStreamJob?.cancel()
    _airMirrorState.update { it.copy(isStreaming = true, permissionError = null) }
    screenStreamJob = connectionEngine.streamChildFrames(
      device = dev, endpoint = "/api/screen/frame",
      onFrameReceived = { bitmap ->
        _airMirrorState.update { it.copy(currentFrame = bitmap.asImageBitmap(), permissionError = null) }
      },
      onError = { err ->
        _airMirrorState.update { it.copy(permissionError = err) }
      }
    )
  }

  fun stopScreenStream() {
    screenStreamJob?.cancel()
    screenStreamJob = null
    _airMirrorState.update { it.copy(isStreaming = false) }
  }

  fun refreshDeviceCapabilities() {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      val status = connectionEngine.fetchDeviceStatus(dev)
      if (status != null) repository.updateDeviceRuntimeState(status)
      val caps = connectionEngine.fetchDeviceCapabilities(dev)
      val remoteFiles = connectionEngine.fetchChildFiles(dev, _remoteCurrentPath.value)
      repository.replaceRemoteFiles(remoteFiles)
      if (caps != null) {
        devicePermissionHealth.value = caps
      }
      val loc = connectionEngine.fetchLocation(dev)
      if (loc != null) {
        repository.insertLocation(loc)
      }
    }
  }

  fun toggleTheme() {
    _isDarkTheme.update { !it }
  }

  fun selectDevice(deviceId: String) {
    _selectedDeviceId.value = deviceId
  }

  fun toggleConnectionMode() {
    val current = selectedDevice.value ?: return
    val newMode = if (current.connectionMode == ConnectionMode.LOCAL_P2P) {
      ConnectionMode.REMOTE_CLOUD
    } else {
      ConnectionMode.LOCAL_P2P
    }
    viewModelScope.launch {
      repository.updateDeviceConnectionMode(current.id, newMode)
    }
  }

  // --- AirMirror Engine Actions ---
  fun onScreenTouch(xPercent: Float, yPercent: Float) {
    val point = TouchPoint(xPercent, yPercent)
    val absX = (xPercent * 1080).toInt()
    val absY = (yPercent * 2400).toInt()
    _airMirrorState.update {
      it.copy(
        lastInputEvent = "ACTION_DOWN at ($absX, $absY)",
        touchPoints = (it.touchPoints + point).takeLast(5)
      )
    }
    viewModelScope.launch {
      selectedDevice.value?.let { dev ->
        connectionEngine.sendTapCommand(dev, xPercent, yPercent)
      }
    }
  }

  fun onScreenSwipe(direction: String) {
    _airMirrorState.update {
      it.copy(lastInputEvent = "Gesture: Swipe $direction (Scroll mapped)")
    }
    viewModelScope.launch {
      selectedDevice.value?.let { dev ->
        connectionEngine.sendSwipeCommand(dev, direction)
      }
    }
  }

  fun onHardwareButton(action: String) {
    _airMirrorState.update { state ->
      when (action) {
        "BACK" -> state.copy(lastInputEvent = "Hardware: Back Key (KeyEvent.KEYCODE_BACK)")
        "HOME" -> state.copy(lastInputEvent = "Hardware: Home Key (KeyEvent.KEYCODE_HOME)")
        "RECENTS" -> state.copy(lastInputEvent = "Hardware: Recents Key (KeyEvent.KEYCODE_APP_SWITCH)")
        "VOLUME_UP" -> state.copy(lastInputEvent = "Hardware: Volume Up")
        "VOLUME_DOWN" -> state.copy(lastInputEvent = "Hardware: Volume Down")
        "POWER" -> state.copy(lastInputEvent = "Hardware: Lock/Power")
        else -> state
      }
    }
    viewModelScope.launch {
      selectedDevice.value?.let { dev ->
        connectionEngine.sendKeyCommand(dev, action)
      }
    }
  }

  fun toggleFullscreen() {
    _airMirrorState.update { it.copy(isFullscreen = !it.isFullscreen) }
  }


  fun onTypeInputKey(char: String) {
    _airMirrorState.update {
      val newText = it.remoteInputFieldText + char
      it.copy(
        remoteInputFieldText = newText,
        lastInputEvent = "Keyboard mapping: Typed '$char' into remote input"
      )
    }
    viewModelScope.launch {
      selectedDevice.value?.let { dev ->
        connectionEngine.sendTextInput(dev, char)
      }
    }
  }

  fun onBackspaceInput() {
    _airMirrorState.update {
      val newText = if (it.remoteInputFieldText.isNotEmpty()) it.remoteInputFieldText.dropLast(1) else ""
      it.copy(
        remoteInputFieldText = newText,
        lastInputEvent = "Keyboard mapping: KeyCode.DEL"
      )
    }
    viewModelScope.launch {
      selectedDevice.value?.let { dev ->
        connectionEngine.sendKeyCommand(dev, "KEYCODE_DEL")
      }
    }
  }

  fun clearRemoteInput() {
    _airMirrorState.update { it.copy(remoteInputFieldText = "") }
  }

  // --- Remote Camera Tools ---

  // --- App Management Actions ---
  fun toggleAppBlock(packageName: String, currentBlocked: Boolean) {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      val newBlocked = !currentBlocked
      repository.setAppBlocked(packageName, dev.id, newBlocked)
      connectionEngine.sendAppBlock(dev, packageName, newBlocked)
    }
  }

  fun setAppDailyLimit(packageName: String, limitMinutes: Int) {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      repository.setAppDailyLimit(packageName, dev.id, limitMinutes)
      connectionEngine.sendAppBlock(dev, packageName, false, limitMinutes)
    }
  }

  fun toggleAppAlwaysAllowed(packageName: String, currentAllowed: Boolean) {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      repository.setAppAlwaysAllowed(packageName, dev.id, !currentAllowed)
    }
  }

  // --- Requests & Alerts Actions ---
  fun respondToChildRequest(requestId: String, approved: Boolean) {
    viewModelScope.launch {
      repository.respondToRequest(requestId, approved)
    }
  }

  fun resolveDeviceAlert(alertId: String) {
    viewModelScope.launch {
      repository.resolveAlert(alertId)
    }
  }

  // --- Location & Geofence Actions ---
  fun addNewGeofence(name: String, address: String, radiusMeters: Int) {
    val dev = selectedDevice.value ?: return
    val currentLoc = childLocation.value
    viewModelScope.launch {
      val loc = currentLoc ?: return@launch
      val geo = Geofence(
        id = "geo-${System.currentTimeMillis() % 10000}",
        deviceId = dev.id,
        name = name,
        address = address,
        latitude = loc.latitude,
        longitude = loc.longitude,
        radiusMeters = radiusMeters
      )
      repository.addGeofence(geo)
    }
  }

  fun removeGeofence(geofence: Geofence) {
    viewModelScope.launch {
      repository.deleteGeofence(geofence)
    }
  }

  // --- Web Protection Actions ---
  fun toggleWebDomainBlock(domain: String, currentBlocked: Boolean) {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      repository.setDomainBlocked(domain, dev.id, !currentBlocked)
    }
  }

  // --- SOS Actions ---
  fun resolveSos() {
    sosAlert.update { it?.copy(isResolved = true, isSirenPlaying = false) }
  }

  fun toggleSosSiren() {
    sosAlert.update { it?.copy(isSirenPlaying = !it.isSirenPlaying) }
  }

  // --- Multi-Child / Device Actions ---
  fun renameSelectedDevice(newName: String) {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      repository.renameDevice(dev.id, newName)
    }
  }

  fun removeSelectedDevice() {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      repository.removeDevice(dev.id)
      _activeTab.value = ActiveTab.DASHBOARD
    }
  }

  fun sendPermissionFixRequest() {
    val dev = selectedDevice.value ?: return
    viewModelScope.launch {
      connectionEngine.executeCommand(
        ChildCommand(
          id = "perm-${UUID.randomUUID().toString().take(8)}",
          type = "REQUEST_PERMISSIONS",
          childDeviceId = dev.id,
          timestamp = System.currentTimeMillis(),
          payload = "{}"
        ),
        dev
      )
      refreshDeviceCapabilities()
    }
  }

  // --- File Explorer Actions ---
  fun setLocalPath(path: String) {
    _localCurrentPath.value = path
    repository.refreshLocalFiles(path)
  }

  fun setRemotePath(path: String) {
    _remoteCurrentPath.value = path
    val dev = selectedDevice.value ?: return
    viewModelScope.launch { repository.replaceRemoteFiles(connectionEngine.fetchChildFiles(dev, path)) }
  }

  fun setFileSearch(query: String) {
    _fileSearchQuery.value = query
  }

  fun toggleGalleryMode() {
    _isGalleryMode.update { !it }
  }

  fun uploadFile(file: FileItem) {
    repository.uploadFileToMobile(file, _remoteCurrentPath.value)
  }

  fun downloadFile(file: FileItem) {
    repository.downloadFileToPC(file)
  }

  fun createNewFolder(isLocal: Boolean, name: String) {
    val current = if (isLocal) _localCurrentPath.value else _remoteCurrentPath.value
    repository.createFolder(isLocal, current, name)
  }

  fun deleteFile(file: FileItem) {
    repository.deleteFile(file)
  }

  fun renameFile(file: FileItem, newName: String) {
    repository.renameFile(file, newName)
  }

  // --- Notification Actions & Quick Reply ---
  fun replyToNotification(notifId: String, reply: String) {
    viewModelScope.launch {
      repository.replyToNotification(notifId, reply)
      _activeToaster.value = null
    }
  }

  fun dismissToaster() {
    _activeToaster.value = null
  }

  fun markNotificationRead(notifId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(notifId)
    }
  }

  // --- SMS Actions ---
  fun selectSmsThread(threadId: String) {
    _selectedSmsThread.value = threadId
  }

  fun setSmsDraft(draft: String) {
    _smsDraft.value = draft
  }

  fun sendCurrentSms(phone: String) {
    val text = _smsDraft.value.trim()
    if (text.isEmpty()) return
    viewModelScope.launch {
      val device = selectedDevice.value ?: return@launch
      val sent = connectionEngine.sendSms(device, phone, text)
      if (sent) _smsDraft.value = ""
    }
  }

  // --- Contacts Actions ---
  fun addContact(name: String, phone: String, email: String, label: String) {
    viewModelScope.launch {
      val devId = selectedDevice.value?.id ?: return@launch
      repository.addContact(devId, name, phone, email, label)
    }
  }

  fun deleteContact(contactId: String) {
    viewModelScope.launch {
      repository.deleteContact(contactId)
    }
  }

  // --- Auth & Pairing ---

  fun refreshPairingCode() {
    parentPairingServer.regenerateCode()
  }



  // --- Downtime Management ---
  fun toggleDowntime(id: String, isEnabled: Boolean) {
    viewModelScope.launch {
      repository.setDowntimeEnabled(id, isEnabled)
      selectedDevice.value?.let { dev ->
        downtimePolicies.value.find { it.id == id }?.let { policy ->
          connectionEngine.sendDowntimeSync(dev, policy.copy(isEnabled = isEnabled))
        }
      }
    }
  }

  fun saveDowntimePolicy(policy: DowntimePolicy) {
    viewModelScope.launch {
      repository.saveDowntimePolicy(policy)
      selectedDevice.value?.let { dev ->
        connectionEngine.sendDowntimeSync(dev, policy)
      }
    }
  }

  // --- Instant Block ---
  fun setInstantBlock(isActive: Boolean, durationMinutes: Int, option: String) {
    val devId = selectedDeviceId.value
    viewModelScope.launch {
      repository.setInstantBlock(devId, isActive, durationMinutes, option)
      selectedDevice.value?.let { dev ->
        val policy = InstantBlockPolicy(
          deviceId = dev.id,
          isActive = isActive,
          blockUntilTimestamp = if (isActive && durationMinutes > 0) System.currentTimeMillis() + (durationMinutes * 60 * 1000L) else 0L,
          durationOption = option
        )
        connectionEngine.sendInstantBlockSync(dev, policy)
      }
    }
  }

  // --- Focus Mode ---
  fun toggleFocusMode(durationMinutes: Int) {
    val current = _focusModePolicy.value
    val newActive = !current.isActive
    val updated = FocusModePolicy(
      deviceId = selectedDeviceId.value,
      isActive = newActive,
      durationMinutes = durationMinutes,
      startedAtTimestamp = if (newActive) System.currentTimeMillis() else 0L,
      blockedCount = if (newActive) 0 else current.blockedCount
    )
    _focusModePolicy.value = updated
    viewModelScope.launch {
      selectedDevice.value?.let { dev ->
        connectionEngine.sendFocusModeSync(dev, updated)
      }
    }
  }

  // --- Social & AI Content Safety ---
  fun markDetectionReviewed(id: String) {
    viewModelScope.launch {
      repository.markDetectionReviewed(id)
    }
  }

  fun addKeywordRule(keyword: String, category: SafetyCategory, severity: AlertSeverity) {
    val rule = KeywordRule("kr-${UUID.randomUUID().toString().take(6)}", keyword, category, severity)
    keywordRuleEngine.addRule(rule)
  }

  fun removeKeywordRule(ruleId: String) {
    keywordRuleEngine.removeRule(ruleId)
  }

  fun processIncomingSocialContent(packageName: String, text: String, sender: String = "") {
    val devId = selectedDeviceId.value
    val events = socialContentEngine.processText(devId, packageName, text, sender)
    viewModelScope.launch {
      events.forEach { event ->
        repository.insertDetectionEvent(event)
        // Post high-priority alert for parent review
        if (event.severity == AlertSeverity.CRITICAL || event.severity == AlertSeverity.HIGH) {
          repository.insertAlert(
            devId = devId,
            title = "Safety Alert: ${event.appName}",
            message = "Triggered keyword '${event.keyword}' in ${event.appName}",
            type = "CONTENT_DETECTION",
            severity = event.severity
          )
        }
      }
    }
  }

  fun processIncomingAiContent(packageName: String, text: String, isPrompt: Boolean = true) {
    val devId = selectedDeviceId.value
    val events = aiContentEngine.processAiPromptOrResponse(devId, packageName, text, isPrompt)
    viewModelScope.launch {
      events.forEach { event ->
        repository.insertDetectionEvent(event)
        if (event.severity == AlertSeverity.CRITICAL || event.severity == AlertSeverity.HIGH) {
          repository.insertAlert(
            devId = devId,
            title = "AI Safety Alert: ${event.appName}",
            message = "Flagged query '${event.keyword}' on ${event.appName}",
            type = "AI_CONTENT_DETECTION",
            severity = event.severity
          )
        }
      }
    }
  }

  // --- Family Chat ---
  fun sendFamilyChatMessage(text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      val msg = FamilyChatMessage(
        id = "msg-${UUID.randomUUID().toString().take(8)}",
        senderId = "fam-1",
        senderName = "Parent Admin",
        message = text.trim(),
        timestamp = "Just now",
        isFromParent = true,
        status = "Delivered"
      )
      repository.sendFamilyChatMessage(msg)
    }
  }

  // --- Offline Synchronization Queue ---
  fun syncOfflineEvents() {
    viewModelScope.launch {
      val pending = pendingOfflineEvents.value
      val dev = selectedDevice.value ?: return@launch
      AppLogger.log(AppLogger.Category.EVENT, "Flushing ${pending.size} offline queued events to child device ${dev.id}")
      pending.forEach { event ->
        val cmd = ChildCommand(
          id = event.id,
          type = event.eventType,
          childDeviceId = event.deviceId,
          timestamp = event.timestamp,
          payload = event.payloadJson
        )
        val result = connectionEngine.executeCommand(cmd, dev)
        if (result.status == "SUCCESS") {
          repository.markOfflineEventSynced(event.id)
        }
      }
    }
  }
}
