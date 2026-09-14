package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.AirDroidChildApp
import com.example.data.model.SyncedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AirDroidNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "AirDroidNotifListener"

        @Volatile
        var instance: AirDroidNotificationListenerService? = null
            private set

        private val _notificationFlow = MutableSharedFlow<SyncedNotification>(extraBufferCapacity = 50)
        val notificationFlow = _notificationFlow.asSharedFlow()

        private val activeActionsMap = mutableMapOf<String, Notification.Action>()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "AirDroid Notification Listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) instance = null
        Log.w(TAG, "AirDroid Notification Listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignore our own foreground daemon notification
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        // Check for quick reply actions (RemoteInput)
        var hasReply = false
        val actions = notification.actions
        if (actions != null) {
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                    hasReply = true
                    activeActionsMap[sbn.key] = action
                    break
                }
            }
        }

        val synced = SyncedNotification(
            id = sbn.key,
            packageName = sbn.packageName,
            appTitle = appName,
            title = title,
            text = text,
            postTime = sbn.postTime,
            canReply = hasReply,
            isSynced = true
        )

        serviceScope.launch {
            _notificationFlow.tryEmit(synced)
            try {
                AirDroidChildApp.repository?.saveNotification(synced)
                AirDroidChildApp.repository?.logAction(
                    actionType = "NOTIFICATION",
                    details = "Intercepted notification from $appName: \"$title - $text\""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error caching notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) {
            activeActionsMap.remove(sbn.key)
            serviceScope.launch {
                AirDroidChildApp.repository?.removeNotification(sbn.key)
            }
        }
    }

    /**
     * Injects a quick reply text response directly into the notification's action bundle.
     */
    fun sendQuickReply(notificationKey: String, replyText: String): Boolean {
        val action = activeActionsMap[notificationKey]
        if (action == null) {
            Log.w(TAG, "No cached action for notification key: $notificationKey")
            return false
        }

        val remoteInputs = action.remoteInputs ?: return false
        val bundle = Bundle()

        for (input in remoteInputs) {
            bundle.putCharSequence(input.resultKey, replyText)
        }

        val intent = Intent()
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

        return try {
            action.actionIntent.send(this, 0, intent)
            Log.i(TAG, "Quick reply sent successfully to notification $notificationKey: $replyText")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send quick reply", e)
            false
        }
    }
}
