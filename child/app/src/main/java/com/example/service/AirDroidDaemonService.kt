package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AirDroidChildApp
import com.example.MainActivity
import com.example.R
import com.example.data.model.DaemonStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AirDroidDaemonService : Service() {

    companion object {
        private const val TAG = "AirDroidDaemonService"
        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "airdroid_daemon_channel"

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, AirDroidDaemonService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AirDroidDaemonService", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AirDroidDaemonService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        createNotificationChannel()

        serviceScope.launch {
            AirDroidChildApp.daemonServer?.status?.collectLatest { status ->
                updateForegroundNotification(status)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AirDroidChildApp.daemonServer?.stopServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                AirDroidChildApp.daemonServer?.pause()
            }
            ACTION_RESUME -> {
                AirDroidChildApp.daemonServer?.resume()
            }
            ACTION_START, null -> {
                val notification = buildNotification(AirDroidChildApp.daemonServer?.status?.value ?: DaemonStatus.LISTENING)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG, "SecurityException on startForeground with type, trying fallback", se)
                    try {
                        startForeground(NOTIFICATION_ID, notification)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed startForeground fallback", e)
                    }
                }

                AirDroidChildApp.daemonServer?.startServer()
            }
        }

        // START_STICKY ensures service restarts if killed by memory manager
        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AirDroidChild::DaemonWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.d(TAG, "Daemon wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Daemon wake lock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Game Centre Daemon",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AirDroid child service active in background for parent connection"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: DaemonStatus): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, AirDroidDaemonService::class.java).apply {
            action = if (status == DaemonStatus.PAUSED) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, AirDroidDaemonService::class.java).apply {
            action = ACTION_STOP
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 2, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ip = AirDroidChildApp.daemonServer?.getLocalIpAddress() ?: "127.0.0.1"
        val statusText = when (status) {
            DaemonStatus.CONNECTED -> "Active • Paired Parent Connected ($ip:8888)"
            DaemonStatus.LISTENING -> "Active • Listening for Parent at http://$ip:8888"
            DaemonStatus.PAUSED -> "Daemon Paused • Remote actions paused"
            else -> "Daemon Initializing..."
        }

        val pauseLabel = if (status == DaemonStatus.PAUSED) "Resume" else "Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Game Centre Daemon")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.game_centre_icon)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, pauseLabel, pauseResumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .build()
    }

    private fun updateForegroundNotification(status: DaemonStatus) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(status))
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        AirDroidChildApp.daemonServer?.stopServer()
        Log.i(TAG, "AirDroid Daemon Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
