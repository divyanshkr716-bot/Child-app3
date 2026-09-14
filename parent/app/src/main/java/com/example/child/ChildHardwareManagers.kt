package com.example.child

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

object ChildHardwareManagers {

  // --- Location Provider ---
  class LocationProvider(private val context: Context) : LocationListener {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    fun startListening() {
      if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return
      }
      try {
        val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val best = lastGps ?: lastNet
        if (best != null) {
          _lastLocation.value = best
        }
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this)
        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, this)
      } catch (e: Exception) {
        // Log or handle gracefully
      }
    }

    fun stopListening() {
      try {
        locationManager?.removeUpdates(this)
      } catch (e: Exception) {}
    }

    override fun onLocationChanged(location: Location) {
      _lastLocation.value = location
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
  }

  // --- Device Health Provider ---
  class DeviceHealthProvider(private val context: Context) {
    fun getBatteryInfo(): Pair<Int, Boolean> {
      val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
      val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
      val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
      } else false
      return Pair(level, isCharging)
    }

    fun getStorageInfo(): Pair<Double, Double> {
      return try {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        val totalGb = (totalBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
        val freeGb = (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
        val usedGb = totalGb - freeGb
        Pair(usedGb, totalGb)
      } catch (e: Exception) {
        Pair(42.0, 128.0)
      }
    }

    fun getNetworkInfo(): Triple<String, Int, String> {
      return try {
        val wifiMgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val info = wifiMgr?.connectionInfo
        val ssid = info?.ssid?.replace("\"", "") ?: "Mobile Data"
        val rssi = info?.rssi ?: -50
        val ip = formatIpAddress(info?.ipAddress ?: 0)
        Triple(if (ssid == "<unknown ssid>") "Connected Wi-Fi" else ssid, rssi, ip)
      } catch (e: Exception) {
        Triple("Wi-Fi", -60, "127.0.0.1")
      }
    }

    private fun formatIpAddress(ip: Int): String {
      return if (ip == 0) "127.0.0.1" else String.format(
        "%d.%d.%d.%d",
        ip and 0xff,
        ip shr 8 and 0xff,
        ip shr 16 and 0xff,
        ip shr 24 and 0xff
      )
    }
  }

  // --- Audio Stream Provider (Microphone) ---
  class AudioStreamProvider(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    fun streamAudioTo(outputStream: OutputStream, maxDurationSec: Int = 120) {
      if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        throw SecurityException("PERMISSION_REQUIRED: RECORD_AUDIO")
      }
      val sampleRate = 16000
      val channelConfig = AudioFormat.CHANNEL_IN_MONO
      val audioFormat = AudioFormat.ENCODING_PCM_16BIT
      val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

      try {
        audioRecord = AudioRecord(
          MediaRecorder.AudioSource.MIC,
          sampleRate,
          channelConfig,
          audioFormat,
          bufferSize
        )
        audioRecord?.startRecording()
        isRecording = true
        val buffer = ByteArray(bufferSize)
        val startTime = System.currentTimeMillis()
        val maxDurationMs = maxDurationSec * 1000L

        while (isRecording && (System.currentTimeMillis() - startTime < maxDurationMs)) {
          val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
          if (read > 0) {
            outputStream.write(buffer, 0, read)
            outputStream.flush()
          } else {
            break
          }
        }
      } finally {
        stopAudio()
      }
    }

    fun stopAudio() {
      isRecording = false
      try {
        audioRecord?.stop()
        audioRecord?.release()
      } catch (e: Exception) {}
      audioRecord = null
    }
  }

  // --- Camera Controller & Flashlight Provider ---
  class CameraStreamProvider(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var isTorchOn = false

    fun toggleTorch(enable: Boolean? = null): Boolean {
      try {
        val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
          val characteristics = cameraManager.getCameraCharacteristics(id)
          characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false

        isTorchOn = enable ?: !isTorchOn
        cameraManager.setTorchMode(cameraId, isTorchOn)
        return isTorchOn
      } catch (e: Exception) {
        return false
      }
    }

    fun isTorchActive(): Boolean = isTorchOn
  }

  // --- Usage Stats & Installed App Provider ---
  class UsageStatsProvider(private val context: Context) {
    fun hasUsageStatsPermission(): Boolean {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
      val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      }
      return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getInstalledApps(): List<ManagedApp> {
      val pm = context.packageManager
      val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      val managed = mutableListOf<ManagedApp>()

      val nonSystemApps = installed.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || isCommonWhitelistedApp(it.packageName) }

      for (app in nonSystemApps) {
        val appName = pm.getApplicationLabel(app).toString()
        val pkg = app.packageName
        val category = categorizeApp(pkg, appName)
        managed.add(
          ManagedApp(
            packageName = pkg,
            deviceId = "child-local",
            appName = appName,
            category = category,
            usageMinutesToday = 0,
            isBlocked = false,
            dailyLimitMinutes = 0,
            isAlwaysAllowed = pkg in listOf("com.google.android.dialer", "com.google.android.apps.messaging")
          )
        )
      }
      return managed
    }

    private fun isCommonWhitelistedApp(pkg: String): Boolean {
      return pkg.startsWith("com.google.android") || pkg.contains("chrome") || pkg.contains("camera") || pkg.contains("youtube")
    }

    private fun categorizeApp(pkg: String, name: String): String {
      val lower = (pkg + name).lowercase()
      return when {
        lower.contains("game") || lower.contains("play") || lower.contains("craft") || lower.contains("subway") -> "Gaming"
        lower.contains("social") || lower.contains("chat") || lower.contains("whatsapp") || lower.contains("insta") || lower.contains("snap") || lower.contains("tiktok") || lower.contains("discord") -> "Social"
        lower.contains("video") || lower.contains("tube") || lower.contains("netflix") || lower.contains("prime") || lower.contains("stream") -> "Video"
        lower.contains("learn") || lower.contains("school") || lower.contains("math") || lower.contains("class") || lower.contains("duo") -> "Education"
        else -> "Utility"
      }
    }
  }

  // --- Real File Storage Provider ---
  class StorageProvider(private val context: Context) {
    fun listFiles(dirPath: String = "/"): List<FileItem> {
      val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
      val targetDir = if (dirPath == "/" || dirPath.isBlank()) baseDir else File(baseDir, dirPath.removePrefix("/"))
      if (!targetDir.exists()) targetDir.mkdirs()

      val files = targetDir.listFiles() ?: emptyArray()
      return files.map { f ->
        val fileType = when {
          f.isDirectory -> FileType.FOLDER
          f.name.endsWith(".jpg", true) || f.name.endsWith(".png", true) || f.name.endsWith(".webp", true) -> FileType.IMAGE
          f.name.endsWith(".mp4", true) || f.name.endsWith(".mkv", true) -> FileType.VIDEO
          f.name.endsWith(".mp3", true) || f.name.endsWith(".wav", true) || f.name.endsWith(".m4a", true) -> FileType.AUDIO
          f.name.endsWith(".pdf", true) || f.name.endsWith(".doc", true) || f.name.endsWith(".txt", true) -> FileType.DOCUMENT
          f.name.endsWith(".zip", true) || f.name.endsWith(".rar", true) -> FileType.ARCHIVE
          f.name.endsWith(".apk", true) -> FileType.APK
          else -> FileType.OTHER
        }
        FileItem(
          id = f.name.hashCode().toString(),
          deviceId = "child-local",
          name = f.name,
          path = f.absolutePath.replace(baseDir.absolutePath, "").ifEmpty { "/" },
          isDirectory = f.isDirectory,
          sizeBytes = if (f.isFile) f.length() else 0L,
          modifiedDate = "Modified recently",
          fileType = fileType,
          isLocal = false
        )
      }
    }

    fun getFile(relativePath: String): File? {
      val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
      val f = File(baseDir, relativePath.removePrefix("/"))
      return if (f.exists()) f else null
    }

    fun saveFile(relativePath: String, input: InputStream): Long {
      val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
      val target = File(baseDir, relativePath.removePrefix("/"))
      target.parentFile?.mkdirs()
      target.outputStream().use { out ->
        return input.copyTo(out)
      }
    }
  }

  // --- Siren Emergency Alarm Provider ---
  class SirenAlarmManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private var ringtone: Ringtone? = null
    private var isPlaying = false

    fun startSiren() {
      if (isPlaying) return
      isPlaying = true
      try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.play()
      } catch (e: Exception) {
        try {
          toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
          toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 15000)
        } catch (ex: Exception) {}
      }
    }

    fun stopSiren() {
      isPlaying = false
      try {
        ringtone?.stop()
      } catch (e: Exception) {}
      try {
        toneGenerator?.stopTone()
        toneGenerator?.release()
      } catch (e: Exception) {}
      ringtone = null
      toneGenerator = null
    }

    fun isSirenActive(): Boolean = isPlaying
  }

  // --- Real Telephony & SMS Provider ---
  class TelephonyProvider(private val context: Context) {
    fun getCalls(): List<com.example.data.model.CallLog> {
      if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
      }
      val calls = mutableListOf<com.example.data.model.CallLog>()
      try {
        val cursor = context.contentResolver.query(
          android.provider.CallLog.Calls.CONTENT_URI,
          arrayOf(android.provider.CallLog.Calls._ID, android.provider.CallLog.Calls.NUMBER, android.provider.CallLog.Calls.CACHED_NAME, android.provider.CallLog.Calls.TYPE, android.provider.CallLog.Calls.DATE, android.provider.CallLog.Calls.DURATION),
          null,
          null,
          "${android.provider.CallLog.Calls.DATE} DESC LIMIT 50"
        )
        cursor?.use {
          val numIdx = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
          val nameIdx = it.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
          val typeIdx = it.getColumnIndex(android.provider.CallLog.Calls.TYPE)
          val durIdx = it.getColumnIndex(android.provider.CallLog.Calls.DURATION)
          val idIdx = it.getColumnIndex(android.provider.CallLog.Calls._ID)

          while (it.moveToNext()) {
            val num = if (numIdx >= 0) it.getString(numIdx) ?: "Unknown" else "Unknown"
            val name = if (nameIdx >= 0) it.getString(nameIdx) ?: num else num
            val typeVal = if (typeIdx >= 0) it.getInt(typeIdx) else android.provider.CallLog.Calls.INCOMING_TYPE
            val dur = if (durIdx >= 0) it.getInt(durIdx) else 0
            val id = if (idIdx >= 0) it.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()

            val callType = when (typeVal) {
              android.provider.CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
              android.provider.CallLog.Calls.MISSED_TYPE -> CallType.MISSED
              else -> CallType.INCOMING
            }
            calls.add(com.example.data.model.CallLog(id, "child-local", name, num, callType, "Today", dur))
          }
        }
      } catch (e: Exception) {}
      return calls
    }

    fun getSmsThreads(): List<SmsThread> {
      if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
      }
      val threads = mutableListOf<SmsThread>()
      try {
        val cursor = context.contentResolver.query(
          Telephony.Sms.CONTENT_URI,
          arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.READ),
          null,
          null,
          "${Telephony.Sms.DATE} DESC LIMIT 40"
        )
        cursor?.use {
          val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
          val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
          val idIdx = it.getColumnIndex(Telephony.Sms._ID)

          while (it.moveToNext()) {
            val addr = if (addrIdx >= 0) it.getString(addrIdx) ?: "Contact" else "Contact"
            val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
            val id = if (idIdx >= 0) it.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()

            threads.add(
              SmsThread(
                id = id,
                deviceId = "child-local",
                contactName = addr,
                phoneNumber = addr,
                lastMessage = body,
                lastMessageTime = "Recent",
                unreadCount = 0
              )
            )
          }
        }
      } catch (e: Exception) {}
      return threads
    }

    fun getContacts(): List<Contact> {
      if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
      }
      val contacts = mutableListOf<Contact>()
      try {
        val cursor = context.contentResolver.query(
          ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
          arrayOf(ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
          null,
          null,
          "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 60"
        )
        cursor?.use {
          val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
          val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
          val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)

          while (it.moveToNext()) {
            val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Contact" else "Contact"
            val num = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
            val id = if (idIdx >= 0) it.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
            contacts.add(Contact(id, "child-local", name, num, ""))
          }
        }
      } catch (e: Exception) {}
      return contacts
    }
  }
}
