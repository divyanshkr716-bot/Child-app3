package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ActiveTab
import com.example.ui.AirDroidViewModel
import com.example.ui.components.AppTopBar
import com.example.ui.components.NavigationSidebar
import com.example.ui.components.NotificationToaster
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize the Child Daemon Service in background so local device features & endpoints are live
    try {
    } catch (e: Exception) {
      // Permission or startup failure handled gracefully
    }

    setContent {
      val viewModel: AirDroidViewModel = viewModel()
      val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

      MyApplicationTheme(darkTheme = isDarkTheme) {
        val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
        val devices by viewModel.devices.collectAsStateWithLifecycle()
        val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
        val selectedDeviceId by viewModel.selectedDeviceId.collectAsStateWithLifecycle()
        val latencyMs by viewModel.networkLatencyMs.collectAsStateWithLifecycle()
        val fps by viewModel.fpsCounter.collectAsStateWithLifecycle()
        val activeToaster by viewModel.activeToaster.collectAsStateWithLifecycle()
        val airMirrorState by viewModel.airMirrorState.collectAsStateWithLifecycle()
        val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
        val localFiles by viewModel.localFiles.collectAsStateWithLifecycle()
        val remoteFiles by viewModel.remoteFiles.collectAsStateWithLifecycle()
        val activeTransfers by viewModel.activeTransfers.collectAsStateWithLifecycle()
        val localCurrentPath by viewModel.localCurrentPath.collectAsStateWithLifecycle()
        val remoteCurrentPath by viewModel.remoteCurrentPath.collectAsStateWithLifecycle()
        val isGalleryMode by viewModel.isGalleryMode.collectAsStateWithLifecycle()
        val notifications by viewModel.notifications.collectAsStateWithLifecycle()
        val smsMessages by viewModel.smsMessages.collectAsStateWithLifecycle()
        val contacts by viewModel.contacts.collectAsStateWithLifecycle()
        val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
        val selectedSmsThread by viewModel.selectedSmsThread.collectAsStateWithLifecycle()
        val smsDraft by viewModel.smsDraft.collectAsStateWithLifecycle()
        val userSession by viewModel.userSession.collectAsStateWithLifecycle()
        val pairingCode by viewModel.pairingCode.collectAsStateWithLifecycle()

        // Parental Control & Security Flows
        val childLocation by viewModel.childLocation.collectAsStateWithLifecycle()
        val geofences by viewModel.geofences.collectAsStateWithLifecycle()
        val managedApps by viewModel.managedApps.collectAsStateWithLifecycle()
        val timelineEvents by viewModel.timelineEvents.collectAsStateWithLifecycle()
        val childRequests by viewModel.childRequests.collectAsStateWithLifecycle()
        val deviceAlerts by viewModel.deviceAlerts.collectAsStateWithLifecycle()
        val webHistory by viewModel.webHistory.collectAsStateWithLifecycle()
        val drivingTrips by viewModel.drivingTrips.collectAsStateWithLifecycle()
        val sosAlert by viewModel.sosAlert.collectAsStateWithLifecycle()
        val permissionHealth by viewModel.devicePermissionHealth.collectAsStateWithLifecycle()
        val audioVolumeDb by viewModel.audioVolumeDb.collectAsStateWithLifecycle()
        val isOneWayAudioActive by viewModel.isOneWayAudioActive.collectAsStateWithLifecycle()
        val detectionEvents by viewModel.detectionEvents.collectAsStateWithLifecycle()
        val imageDetections by viewModel.imageDetections.collectAsStateWithLifecycle()
        val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle()
        val familyChatMessages by viewModel.familyChatMessages.collectAsStateWithLifecycle()

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            AppTopBar(
              selectedDevice = selectedDevice,
              devices = devices,
              onSelectDevice = { viewModel.selectDevice(it) },
              onToggleConnectionMode = { viewModel.toggleConnectionMode() },
              latencyMs = latencyMs,
              fps = fps,
              isDarkTheme = isDarkTheme,
              onToggleTheme = { viewModel.toggleTheme() },
              userSession = userSession
            )
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .background(MaterialTheme.colorScheme.background)
          ) {
            Row(modifier = Modifier.fillMaxSize()) {
              // Left Navigation Sidebar
              NavigationSidebar(
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) },
                devices = devices,
                selectedDeviceId = selectedDeviceId,
                onSelectDevice = { viewModel.selectDevice(it) },
                unreadNotifsCount = notifications.count { !it.isRead }
              )

              // Main Active Workspace Module
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              ) {
                when (activeTab) {
                  ActiveTab.DASHBOARD -> DashboardScreen(
                    device = selectedDevice,
                    notifications = notifications,
                    onNavigateTab = { viewModel.setTab(it) },
                    onToggleConnectionMode = { viewModel.toggleConnectionMode() }
                  )

                  ActiveTab.AIR_MIRROR -> AirMirrorScreen(
                    device = selectedDevice,
                    airMirrorState = airMirrorState,
                    latencyMs = latencyMs,
                    fps = fps,
                    onScreenTouch = { x, y -> viewModel.onScreenTouch(x, y) },
                    onScreenSwipe = { dir -> viewModel.onScreenSwipe(dir) },
                    onHardwareButton = { btn -> viewModel.onHardwareButton(btn) },
                    onToggleFullscreen = { viewModel.toggleFullscreen() },
                    onTypeKey = { key -> viewModel.onTypeInputKey(key) },
                    onBackspace = { viewModel.onBackspaceInput() },
                    onClearInput = { viewModel.clearRemoteInput() }
                  )

                  ActiveTab.FILE_TRANSFER -> FileTransferScreen(
                    localFiles = localFiles,
                    remoteFiles = remoteFiles,
                    activeTransfers = activeTransfers,
                    localCurrentPath = localCurrentPath,
                    remoteCurrentPath = remoteCurrentPath,
                    isGalleryMode = isGalleryMode,
                    onToggleGalleryMode = { viewModel.toggleGalleryMode() },
                    onSetLocalPath = { viewModel.setLocalPath(it) },
                    onSetRemotePath = { viewModel.setRemotePath(it) },
                    onUploadFile = { viewModel.uploadFile(it) },
                    onDownloadFile = { viewModel.downloadFile(it) },
                    onCreateFolder = { isLocal, name -> viewModel.createNewFolder(isLocal, name) },
                    onDeleteFile = { viewModel.deleteFile(it) },
                    onRenameFile = { item, name -> viewModel.renameFile(item, name) }
                  )

                  ActiveTab.MESSAGES -> SmsNotificationScreen(
                    notifications = notifications,
                    smsMessages = smsMessages,
                    selectedThreadId = selectedSmsThread,
                    smsDraft = smsDraft,
                    onSelectThread = { viewModel.selectSmsThread(it) },
                    onSmsDraftChange = { viewModel.setSmsDraft(it) },
                    onSendSms = { phone -> viewModel.sendCurrentSms(phone) },
                    onReplyNotification = { id, text -> viewModel.replyToNotification(id, text) },
                    onDismissNotification = { id -> viewModel.markNotificationRead(id) }
                  )

                  ActiveTab.REMOTE_CAMERA -> RemoteCameraScreen(
                    device = selectedDevice,
                    cameraState = cameraState,
                    latencyMs = latencyMs,
                    fps = fps
                  )

                  ActiveTab.SOCIAL_SAFETY -> SocialSafetyScreen(
                    device = selectedDevice,
                    detectionEvents = detectionEvents,
                    imageDetections = imageDetections,
                    onMarkReviewed = { viewModel.markDetectionReviewed(it) },
                    onAddRule = { kw, cat, sev -> viewModel.addKeywordRule(kw, cat, sev) },
                    onRemoveRule = { viewModel.removeKeywordRule(it) },
                  )

                  ActiveTab.FAMILY_CHAT -> FamilyChatScreen(
                    members = familyMembers,
                    messages = familyChatMessages,
                    onSendMessage = { viewModel.sendFamilyChatMessage(it) }
                  )

                  ActiveTab.LOCATION -> LocationScreen(
                    device = selectedDevice,
                    location = childLocation,
                    geofences = geofences,
                    onAddGeofence = { name, addr, r -> viewModel.addNewGeofence(name, addr, r) },
                    onDeleteGeofence = { geo -> viewModel.removeGeofence(geo) }
                  )

                  ActiveTab.APP_MANAGEMENT -> AppManagementScreen(
                    device = selectedDevice,
                    apps = managedApps,
                    onToggleBlock = { pkg, blocked -> viewModel.toggleAppBlock(pkg, blocked) },
                    onSetDailyLimit = { pkg, limit -> viewModel.setAppDailyLimit(pkg, limit) },
                    onToggleAlwaysAllowed = { pkg, allowed -> viewModel.toggleAppAlwaysAllowed(pkg, allowed) }
                  )

                  ActiveTab.EVENTS_USAGE -> EventsUsageScreen(
                    device = selectedDevice,
                    timelineEvents = timelineEvents
                  )

                  ActiveTab.REQUESTS_ALERTS -> RequestsAlertsScreen(
                    device = selectedDevice,
                    requests = childRequests,
                    alerts = deviceAlerts,
                    onRespondToRequest = { id, approved -> viewModel.respondToChildRequest(id, approved) },
                    onResolveAlert = { id -> viewModel.resolveDeviceAlert(id) }
                  )

                  ActiveTab.WEB_MONITOR -> WebMonitoringScreen(
                    device = selectedDevice,
                    webHistory = webHistory,
                    onToggleDomainBlock = { dom, blocked -> viewModel.toggleWebDomainBlock(dom, blocked) }
                  )

                  ActiveTab.DRIVING_SAFETY -> DrivingDetectionScreen(
                    device = selectedDevice,
                    trips = drivingTrips
                  )

                  ActiveTab.DEVICE_HEALTH -> DeviceHealthScreen(
                    device = selectedDevice,
                    permissionHealth = permissionHealth,
                    onSendFixRequest = { viewModel.sendPermissionFixRequest() },
                    onRenameDevice = { newName -> viewModel.renameSelectedDevice(newName) },
                    onRemoveDevice = { viewModel.removeSelectedDevice() }
                  )

                  ActiveTab.SOS_CENTER -> SosEmergencyScreen(
                    device = selectedDevice,
                    sosAlert = sosAlert,
                    onResolveSos = { viewModel.resolveSos() },
                    onToggleSiren = { viewModel.toggleSosSiren() }
                  )

                  ActiveTab.CALLS_CONTACTS -> ContactsCallsScreen(
                    contacts = contacts,
                    callLogs = callLogs,
                    onAddContact = { n, p, e, l -> viewModel.addContact(n, p, e, l) },
                    onDeleteContact = { id -> viewModel.deleteContact(id) }
                  )

                  ActiveTab.PAIRING -> DevicePairingScreen(
                    pairingCode = pairingCode,
                    pairingServerIp = viewModel.pairingServerIp,
                    onRefreshCode = { viewModel.refreshPairingCode() }
                  )
                }
              }
            }

            // Real-time Notification Toaster (Anchored Top-End)
            NotificationToaster(
              notification = activeToaster,
              onDismiss = { viewModel.dismissToaster() },
              onReply = { id, reply -> viewModel.replyToNotification(id, reply) },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp)
            )
          }
        }
      }
    }
  }
}
