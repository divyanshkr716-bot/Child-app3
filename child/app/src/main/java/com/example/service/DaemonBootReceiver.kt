package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.AirDroidChildApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DaemonBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DaemonBootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action ?: return

        Log.i(TAG, "Received system broadcast: $action. Starting allowed background services after reboot...")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AirDroidChild:BootWakeLock"
        )
        wakeLock?.acquire(15000L) // hold wakelock for 15s to guarantee service initialization

        try {
            // 1. Start Persistent Daemon Foreground Service
            AirDroidDaemonService.startService(context)

            // 2. Start Embedded HTTP / WebSocket Daemon Server
            AirDroidChildApp.daemonServer?.startServer()

            // 3. Re-engage Location Tracking without prompting
            AirDroidChildApp.locationManager?.startTracking()

            // 4. Record Audit Log
            CoroutineScope(Dispatchers.IO).launch {
                AirDroidChildApp.repository?.logAction(
                    "DEVICE_BOOT_RESTART_AUTO_RESUMED",
                    "Device restarted ($action). Allowed child services were restarted subject to Android permissions and OS restrictions.",
                    isSuccess = true
                )
            }

            Log.i(TAG, "Game Centre background services restarted post-boot.")
        } catch (e: Exception) {
            Log.e(TAG, "Error in DaemonBootReceiver onReceive", e)
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }
}
