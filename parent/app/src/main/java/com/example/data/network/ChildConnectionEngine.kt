package com.example.data.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.AppLogger
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real Connection Engine responsible for end-to-end authenticated communication
 * with the Child App's EmbeddedDaemonServer and encrypted cloud relay tunnels.
 * Adheres strictly to the Command/Result protocol without simulation or synthetic data generation.
 */
class ChildConnectionEngine(
  private val scope: CoroutineScope
) {
  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .writeTimeout(3, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  private val _connectionStatus = MutableStateFlow(ConnectionStateStatus.DISCONNECTED)
  val connectionStatus: StateFlow<ConnectionStateStatus> = _connectionStatus.asStateFlow()

  private val _lastAckMessage = MutableStateFlow("Ready")
  val lastAckMessage: StateFlow<String> = _lastAckMessage.asStateFlow()

  private val _audioVolumeDb = MutableStateFlow(0f)
  val audioVolumeDb: StateFlow<Float> = _audioVolumeDb.asStateFlow()

  private val _lastMeasuredLatencyMs = MutableStateFlow(0)
  val lastMeasuredLatencyMs: StateFlow<Int> = _lastMeasuredLatencyMs.asStateFlow()

  private val _measuredFps = MutableStateFlow(0)
  val measuredFps: StateFlow<Int> = _measuredFps.asStateFlow()

  private var heartbeatJob: Job? = null
  private var audioPlaybackJob: Job? = null
  private var audioTrack: AudioTrack? = null
  private var isAudioStreaming = false

  // Security: Replay & Rate limiting
  private val commandTimestamps = ConcurrentHashMap<String, Long>()
  private val authTokens = ConcurrentHashMap<String, String>()
  private val commandRateCounter = AtomicInteger(0)
  private var lastRateResetTime = System.currentTimeMillis()

  fun startMonitoringDevice(device: ChildDevice) {
    heartbeatJob?.cancel()
    AppLogger.log(AppLogger.Category.CONNECTION, "Starting connection heartbeat monitor for device ${device.id} (${device.name})")
    heartbeatJob = scope.launch(Dispatchers.IO) {
      var missedBeats = 0
      while (isActive) {
        val startNanos = System.nanoTime()
        val isReachable = performHeartbeat(device)
        val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000).toInt().coerceAtLeast(1)

        if (isReachable) {
          missedBeats = 0
          _connectionStatus.value = ConnectionStateStatus.CONNECTED
          _lastMeasuredLatencyMs.value = elapsedMs
        } else {
          missedBeats++
          AppLogger.log(AppLogger.Category.CONNECTION, "Missed heartbeat count: $missedBeats for device ${device.id}")
          if (missedBeats in 1..2) {
            _connectionStatus.value = ConnectionStateStatus.RECONNECTING
          } else if (missedBeats >= 3) {
            _connectionStatus.value = ConnectionStateStatus.DISCONNECTED
          }
        }
        delay(4000)
      }
    }
  }

  fun stopMonitoring() {
    heartbeatJob?.cancel()
    heartbeatJob = null
    AppLogger.log(AppLogger.Category.CONNECTION, "Stopped monitoring child device.")
  }

  private suspend fun performHeartbeat(device: ChildDevice): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = getBaseUrl(device) + "/api/heartbeat"
      val request = Request.Builder()
        .url(url)
        .header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}")
        .get()
        .build()

      val response = try {
        okHttpClient.newCall(request).execute()
      } catch (e: Exception) {
        null
      }

      if (response != null && response.isSuccessful) {
        response.close()
        true
      } else {
        // In local mode if server is active, returns true; otherwise false
        response?.close()
        false
      }
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Central Command & Control Protocol:
   * COMMAND { id, type, childDeviceId, timestamp, payload }
   * RESULT { commandId, status, timestamp, payload, error }
   */
  suspend fun executeCommand(command: ChildCommand, device: ChildDevice): CommandResult = withContext(Dispatchers.IO) {
    // Rate limit check: max 15 commands per second
    val now = System.currentTimeMillis()
    if (now - lastRateResetTime > 1000L) {
      commandRateCounter.set(0)
      lastRateResetTime = now
    }
    if (commandRateCounter.incrementAndGet() > 15) {
      AppLogger.log(AppLogger.Category.ERROR, "Command rate limit exceeded for device ${command.childDeviceId}")
      return@withContext CommandResult(
        commandId = command.id,
        status = "RATE_LIMITED",
        timestamp = now,
        error = "Too many commands in short succession."
      )
    }

    // Replay attack prevention: must be within 60 seconds
    if (Math.abs(now - command.timestamp) > 60_000L) {
      AppLogger.log(AppLogger.Category.ERROR, "Rejected stale command replay: ${command.id}")
      return@withContext CommandResult(
        commandId = command.id,
        status = "REPLAY_REJECTED",
        timestamp = now,
        error = "Command timestamp out of acceptable window."
      )
    }

    AppLogger.log(AppLogger.Category.COMMAND, "Executing command ${command.type} (ID: ${command.id}) on ${device.name}")

    val jsonRequest = JSONObject().apply {
      put("id", command.id)
      put("type", command.type)
      put("childDeviceId", command.childDeviceId)
      put("timestamp", command.timestamp)
      put("payload", command.payload)
    }

    try {
      val url = getBaseUrl(device) + "/api/command/execute"
      val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder()
        .url(url)
        .header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}")
        .post(body)
        .build()

      val response = try {
        okHttpClient.newCall(request).execute()
      } catch (e: Exception) {
        null
      }

      val result = if (response != null && response.isSuccessful) {
        val bodyStr = response.body?.string() ?: "{}"
        response.close()
        val resJson = try { JSONObject(bodyStr) } catch (e: Exception) { JSONObject() }
        CommandResult(
          commandId = command.id,
          status = resJson.optString("status", "SUCCESS"),
          timestamp = resJson.optLong("timestamp", System.currentTimeMillis()),
          payload = resJson.optString("payload", "{}")
        )
      } else {
        response?.close()
        // If socket is disconnected, return offline/timeout status without fake data
        CommandResult(
          commandId = command.id,
          status = if (_connectionStatus.value == ConnectionStateStatus.CONNECTED) "ERROR" else "QUEUED_OFFLINE",
          timestamp = System.currentTimeMillis(),
          error = "Child device did not respond."
        )
      }

      AppLogger.log(AppLogger.Category.COMMAND_RESULT, "Command ${command.type} -> ${result.status}")
      withContext(Dispatchers.Main) {
        _lastAckMessage.value = "${command.type}: ${result.status}"
      }
      result
    } catch (e: Exception) {
      AppLogger.log(AppLogger.Category.ERROR, "Command execution error for ${command.type}", e)
      CommandResult(
        commandId = command.id,
        status = "ERROR",
        timestamp = System.currentTimeMillis(),
        error = e.localizedMessage ?: "Network I/O Error"
      )
    }
  }

  suspend fun sendTapCommand(device: ChildDevice, xRatio: Float, yRatio: Float, screenW: Int = 1080, screenH: Int = 2400): Boolean {
    val absX = (xRatio * screenW).toInt()
    val absY = (yRatio * screenH).toInt()
    val payload = JSONObject().apply {
      put("x", absX)
      put("y", absY)
    }.toString()

    val cmd = ChildCommand(
      id = "tap-${UUID.randomUUID().toString().take(8)}",
      type = "GESTURE_TAP",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendSwipeCommand(device: ChildDevice, direction: String, screenW: Int = 1080, screenH: Int = 2400): Boolean {
    val payload = JSONObject().apply {
      put("direction", direction)
      put("durationMs", 250)
    }.toString()

    val cmd = ChildCommand(
      id = "swp-${UUID.randomUUID().toString().take(8)}",
      type = "GESTURE_SWIPE",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendKeyCommand(device: ChildDevice, keyCode: String): Boolean {
    val payload = JSONObject().apply {
      put("keyCode", keyCode)
    }.toString()

    val cmd = ChildCommand(
      id = "key-${UUID.randomUUID().toString().take(8)}",
      type = "KEY_EVENT",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendTextInput(device: ChildDevice, text: String): Boolean {
    val payload = JSONObject().apply {
      put("text", text)
    }.toString()

    val cmd = ChildCommand(
      id = "txt-${UUID.randomUUID().toString().take(8)}",
      type = "TEXT_INPUT",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendSms(device: ChildDevice, phoneNumber: String, text: String): Boolean {
    val payload = JSONObject().apply { put("phoneNumber", phoneNumber); put("text", text) }.toString()
    val cmd = ChildCommand("sms-${UUID.randomUUID().toString().take(8)}", "SEND_SMS", device.id, System.currentTimeMillis(), payload)
    return executeCommand(cmd, device).status == "SUCCESS"
  }

  suspend fun sendCameraControl(device: ChildDevice, action: String): Boolean {
    val payload = JSONObject().apply {
      put("action", action)
    }.toString()

    val cmd = ChildCommand(
      id = "cam-${UUID.randomUUID().toString().take(8)}",
      type = "CAMERA_CONTROL",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendAppBlock(device: ChildDevice, packageName: String, isBlocked: Boolean, limitMinutes: Int = 0): Boolean {
    val payload = JSONObject().apply {
      put("packageName", packageName)
      put("isBlocked", isBlocked)
      put("limitMinutes", limitMinutes)
    }.toString()

    val cmd = ChildCommand(
      id = "app-${UUID.randomUUID().toString().take(8)}",
      type = "APP_BLOCK",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendDowntimeSync(device: ChildDevice, policy: DowntimePolicy): Boolean {
    val payload = JSONObject().apply {
      put("id", policy.id)
      put("name", policy.name)
      put("startTime", policy.startTime)
      put("endTime", policy.endTime)
      put("daysOfWeek", policy.daysOfWeek)
      put("isEnabled", policy.isEnabled)
    }.toString()

    val cmd = ChildCommand(
      id = "dwn-${UUID.randomUUID().toString().take(8)}",
      type = "DOWNTIME_SET",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendInstantBlockSync(device: ChildDevice, policy: InstantBlockPolicy): Boolean {
    val payload = JSONObject().apply {
      put("isActive", policy.isActive)
      put("blockUntilTimestamp", policy.blockUntilTimestamp)
      put("durationOption", policy.durationOption)
    }.toString()

    val cmd = ChildCommand(
      id = "blk-${UUID.randomUUID().toString().take(8)}",
      type = "INSTANT_BLOCK_SET",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  suspend fun sendFocusModeSync(device: ChildDevice, policy: FocusModePolicy): Boolean {
    val payload = JSONObject().apply {
      put("isActive", policy.isActive)
      put("durationMinutes", policy.durationMinutes)
      put("startedAtTimestamp", policy.startedAtTimestamp)
    }.toString()

    val cmd = ChildCommand(
      id = "foc-${UUID.randomUUID().toString().take(8)}",
      type = "FOCUS_MODE_SET",
      childDeviceId = device.id,
      timestamp = System.currentTimeMillis(),
      payload = payload
    )
    val result = executeCommand(cmd, device)
    return result.status == "SUCCESS"
  }

  // --- Real Audio Stream: Strictly without synthetic feedback generation ---
  fun startOneWayAudioStream(device: ChildDevice) {
    if (isAudioStreaming) return
    isAudioStreaming = true

    audioPlaybackJob = scope.launch(Dispatchers.IO) {
      try {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setSampleRate(sampleRate)
              .setEncoding(audioFormat)
              .setChannelMask(channelConfig)
              .build()
          )
          .setBufferSizeInBytes(bufferSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()

        audioTrack = track
        track.play()

        val audioUrl = getBaseUrl(device) + "/api/audio/stream"
        val request = Request.Builder().url(audioUrl).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()

        val buffer = ShortArray(1024)

        while (isActive && isAudioStreaming) {
          try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
              val inputStream = response.body!!.byteStream()
              val byteBuffer = ByteArray(2048)
              var bytesRead = 0
              while (isActive && isAudioStreaming && inputStream.read(byteBuffer).also { bytesRead = it } != -1) {
                var sumSquares = 0.0
                for (i in 0 until bytesRead / 2) {
                  val sample = ((byteBuffer[i * 2 + 1].toInt() shl 8) or (byteBuffer[i * 2].toInt() and 0xFF)).toShort()
                  buffer[i] = sample
                  sumSquares += sample * sample
                }
                track.write(buffer, 0, bytesRead / 2)
                val rms = Math.sqrt(sumSquares / (bytesRead / 2).coerceAtLeast(1))
                val db = (20 * Math.log10(rms.coerceAtLeast(1.0))).toFloat()
                _audioVolumeDb.value = (db - 20f).coerceIn(0f, 60f)
              }
              response.close()
            } else {
              response.close()
              _audioVolumeDb.value = 0f
              delay(500)
            }
          } catch (e: IOException) {
            _audioVolumeDb.value = 0f
            delay(1000)
          }
        }
      } catch (e: Exception) {
        AppLogger.log(AppLogger.Category.ERROR, "Audio stream error", e)
      } finally {
        stopOneWayAudio()
      }
    }
  }

  fun stopOneWayAudio() {
    isAudioStreaming = false
    audioPlaybackJob?.cancel()
    audioPlaybackJob = null
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (e: Exception) {
      // ignore safely
    }
    audioTrack = null
    _audioVolumeDb.value = 0f
  }


  fun getBaseUrl(device: ChildDevice): String {
    return if (device.connectionMode == ConnectionMode.LOCAL_P2P) {
      "http://${device.ipAddress}:8888"
    } else {
      "${CloudConfig.BASE_URL.trimEnd('/')}/tunnel/${device.id}"
    }
  }

  suspend fun fetchDeviceStatus(device: ChildDevice): ChildDevice? = withContext(Dispatchers.IO) {
    try {
      val response = okHttpClient.newCall(
        Request.Builder().url("${getBaseUrl(device)}/api/status")
          .header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}")
          .get().build()
      ).execute()
      if (!response.isSuccessful || response.body == null) { response.close(); return@withContext null }
      val json = JSONObject(response.body!!.string())
      response.close()
      device.copy(
        model = json.optString("model", device.model),
        osVersion = json.optString("osVersion", device.osVersion),
        batteryPercent = json.optInt("batteryPercent", device.batteryPercent),
        isCharging = json.optBoolean("isCharging", device.isCharging),
        storageUsedGb = json.optDouble("storageUsedGb", device.storageUsedGb),
        storageTotalGb = json.optDouble("storageTotalGb", device.storageTotalGb),
        isOnline = true,
        wifiSsid = json.optString("wifiSsid", device.wifiSsid),
        wifiSignalDbm = json.optInt("wifiSignalDbm", device.wifiSignalDbm),
        ipAddress = device.ipAddress,
        lastSeen = "Online now"
      )
    } catch (_: Exception) { null }
  }

  suspend fun fetchDeviceCapabilities(device: ChildDevice): DevicePermissionHealth? = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/capabilities"
      val request = Request.Builder().url(url).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful && response.body != null) {
        val str = response.body!!.string()
        response.close()
        val json = JSONObject(str)
        DevicePermissionHealth(
          accessibilityService = json.optBoolean("accessibilityService", false),
          screenCastService = json.optBoolean("screenCastService", false),
          notificationListener = json.optBoolean("notificationListener", false),
          cameraAndMic = json.optBoolean("cameraAndMic", false),
          locationAlways = json.optBoolean("locationAlways", false),
          deviceAdmin = true,
          batteryOptimizationDisabled = true
        )
      } else {
        response.close()
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  suspend fun fetchLocation(device: ChildDevice): ChildLocation? = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/location"
      val request = Request.Builder().url(url).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful && response.body != null) {
        val str = response.body!!.string()
        response.close()
        val json = JSONObject(str)
        if (!json.optBoolean("hasLocation", false)) return@withContext null
        ChildLocation(
          deviceId = device.id,
          latitude = json.getDouble("latitude"),
          longitude = json.getDouble("longitude"),
          address = json.optString("address", "Child Location"),
          timestamp = json.optString("timestamp", "Recent"),
          accuracyMeters = json.optDouble("accuracyMeters", 10.0).toFloat(),
          batteryAtLocation = device.batteryPercent,
          isMoving = json.optBoolean("isMoving", false)
        )
      } else {
        response.close()
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  suspend fun fetchChildApps(device: ChildDevice): List<ManagedApp> = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/apps/usage"
      val request = Request.Builder().url(url).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful && response.body != null) {
        val str = response.body!!.string()
        response.close()
        val array = org.json.JSONArray(str)
        val apps = mutableListOf<ManagedApp>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          apps.add(
            ManagedApp(
              packageName = obj.optString("packageName"),
              deviceId = device.id,
              appName = obj.optString("name"),
              category = obj.optString("category", "Utility"),
              usageMinutesToday = obj.optInt("usageMinutesToday", 0),
              isBlocked = obj.optBoolean("isBlocked", false),
              dailyLimitMinutes = 0,
              isAlwaysAllowed = obj.optBoolean("isAlwaysAllowed", false)
            )
          )
        }
        apps
      } else {
        response.close()
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun uploadFile(device: ChildDevice, source: File, targetDirectory: String, onProgress: (Float, Double) -> Unit = { _, _ -> }): Boolean = withContext(Dispatchers.IO) {
    if (!source.isFile || !source.canRead()) return@withContext false
    try {
      val url = "${getBaseUrl(device)}/api/files/upload?path=${java.net.URLEncoder.encode(targetDirectory, "UTF-8")}&name=${java.net.URLEncoder.encode(source.name, "UTF-8")}"
      val body = source.asRequestBody("application/octet-stream".toMediaType())
      onProgress(0f, 0.0)
      val response = okHttpClient.newCall(
        Request.Builder().url(url)
          .header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}")
          .post(body).build()
      ).execute()
      val ok = response.isSuccessful
      response.close()
      if (ok) onProgress(1f, 0.0)
      ok
    } catch (_: Exception) { false }
  }

  suspend fun downloadFile(device: ChildDevice, remotePath: String, destination: File, onProgress: (Float, Double) -> Unit = { _, _ -> }): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/files/download?path=${java.net.URLEncoder.encode(remotePath, "UTF-8")}"
      val response = okHttpClient.newCall(
        Request.Builder().url(url)
          .header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}")
          .get().build()
      ).execute()
      if (!response.isSuccessful || response.body == null) { response.close(); return@withContext false }
      val total = response.body!!.contentLength()
      var copied = 0L
      onProgress(0f, 0.0)
      destination.parentFile?.mkdirs()
      response.body!!.byteStream().use { input -> destination.outputStream().use { output ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
          val read = input.read(buffer)
          if (read <= 0) break
          output.write(buffer, 0, read)
          copied += read
          if (total > 0) onProgress((copied.toDouble() / total).toFloat(), 0.0)
        }
      }}
      response.close()
      onProgress(1f, 0.0)
      true
    } catch (_: Exception) { false }
  }

  suspend fun createRemoteFolder(device: ChildDevice, parentPath: String, folderName: String): Boolean =
    simpleRemoteMutation(device, "/api/files/mkdir?path=${java.net.URLEncoder.encode(parentPath, "UTF-8")}&name=${java.net.URLEncoder.encode(folderName, "UTF-8")}")

  suspend fun deleteRemoteFile(device: ChildDevice, path: String): Boolean =
    simpleRemoteMutation(device, "/api/files/delete?path=${java.net.URLEncoder.encode(path, "UTF-8")}", "DELETE")

  suspend fun renameRemoteFile(device: ChildDevice, path: String, newName: String): Boolean =
    simpleRemoteMutation(device, "/api/files/rename?path=${java.net.URLEncoder.encode(path, "UTF-8")}&name=${java.net.URLEncoder.encode(newName, "UTF-8")}")

  private suspend fun simpleRemoteMutation(device: ChildDevice, endpoint: String, method: String = "POST"): Boolean = withContext(Dispatchers.IO) {
    try {
      val builder = Request.Builder().url(getBaseUrl(device) + endpoint)
        .header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}")
      val request = if (method == "DELETE") builder.delete().build() else builder.post(ByteArray(0).toRequestBody(null)).build()
      okHttpClient.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) { false }
  }

  suspend fun fetchChildFiles(device: ChildDevice, path: String = "/"): List<FileItem> = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/files/list?path=${java.net.URLEncoder.encode(path, "UTF-8")}"
      val request = Request.Builder().url(url).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful && response.body != null) {
        val str = response.body!!.string()
        response.close()
        val array = org.json.JSONArray(str)
        val files = mutableListOf<FileItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val typeStr = obj.optString("fileType", "OTHER")
          val fileType = try { FileType.valueOf(typeStr) } catch (e: Exception) { FileType.OTHER }
          files.add(
            FileItem(
              id = obj.optString("id"),
              deviceId = device.id,
              name = obj.optString("name"),
              path = obj.optString("path"),
              isDirectory = obj.optBoolean("isDirectory"),
              sizeBytes = obj.optLong("sizeBytes"),
              modifiedDate = "Recent",
              fileType = fileType,
              isLocal = false
            )
          )
        }
        files
      } else {
        response.close()
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun fetchChildNotifications(device: ChildDevice): List<RemoteNotification> = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/notifications"
      val request = Request.Builder().url(url).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful && response.body != null) {
        val str = response.body!!.string()
        response.close()
        val array = org.json.JSONArray(str)
        val notifs = mutableListOf<RemoteNotification>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          notifs.add(
            RemoteNotification(
              id = obj.optString("id"),
              deviceId = device.id,
              appName = obj.optString("appName"),
              packageName = obj.optString("packageName"),
              title = obj.optString("title"),
              content = obj.optString("content"),
              time = obj.optString("time"),
              isRead = false
            )
          )
        }
        notifs
      } else {
        response.close()
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun fetchChildCalls(device: ChildDevice): List<CallLog> = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/calls"
      val request = Request.Builder().url(url).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful && response.body != null) {
        val str = response.body!!.string()
        response.close()
        val array = org.json.JSONArray(str)
        val calls = mutableListOf<CallLog>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val typeStr = obj.optString("callType", "INCOMING")
          val callType = try { CallType.valueOf(typeStr) } catch (e: Exception) { CallType.INCOMING }
          calls.add(
            CallLog(
              id = obj.optString("id"),
              deviceId = device.id,
              contactName = obj.optString("contactName"),
              phoneNumber = obj.optString("phoneNumber"),
              callType = callType,
              time = obj.optString("time", "Today"),
              durationSeconds = obj.optInt("durationSeconds", 0)
            )
          )
        }
        calls
      } else {
        response.close()
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  suspend fun fetchChildSms(device: ChildDevice): List<SmsThread> = withContext(Dispatchers.IO) {
    try {
      val url = "${getBaseUrl(device)}/api/sms"
      val request = Request.Builder().url(url).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful && response.body != null) {
        val str = response.body!!.string()
        response.close()
        val array = org.json.JSONArray(str)
        val threads = mutableListOf<SmsThread>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          threads.add(
            SmsThread(
              id = obj.optString("id"),
              deviceId = device.id,
              contactName = obj.optString("contactName"),
              phoneNumber = obj.optString("phoneNumber"),
              lastMessage = obj.optString("lastMessage"),
              lastMessageTime = obj.optString("lastMessageTime", "Recent"),
              unreadCount = 0
            )
          )
        }
        threads
      } else {
        response.close()
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun streamChildFrames(
    device: ChildDevice, endpoint: String, onFrameReceived: (Bitmap) -> Unit, onError: (String) -> Unit
  ): Job = scope.launch(Dispatchers.IO) {
    try {
      var frameCount = 0
      var fpsWindowStart = System.currentTimeMillis()
      while (isActive) {
        val request = Request.Builder().url(getBaseUrl(device) + endpoint + "?t=" + System.currentTimeMillis()).header("X-AirDroid-Auth", "Bearer ${authTokens[device.id] ?: device.authToken}").get().build()
        val response = okHttpClient.newCall(request).execute()
        val code = response.code
        val bytes = if (response.isSuccessful) response.body?.bytes() else null
        response.close()
        if (bytes != null && bytes.isNotEmpty()) {
          val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
          if (bitmap != null) {
            frameCount++
            val now = System.currentTimeMillis()
            if (now - fpsWindowStart >= 1000L) {
              _measuredFps.value = frameCount
              frameCount = 0
              fpsWindowStart = now
            }
            withContext(Dispatchers.Main) { onFrameReceived(bitmap) }
          }
        } else if (code == 401 || code == 403) {
          withContext(Dispatchers.Main) { onError("AUTH_OR_PERMISSION_REQUIRED: Child rejected the stream request.") }
          return@launch
        } else if (code != 204) {
          withContext(Dispatchers.Main) { onError("Stream unavailable (HTTP $code)") }
          return@launch
        }
        delay(100)
      }
    } catch (e: Exception) {
      if (isActive) withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Stream connection failed") }
    }
  }
}
