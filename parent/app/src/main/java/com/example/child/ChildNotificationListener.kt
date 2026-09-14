package com.example.child

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.model.RemoteNotification
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class ChildNotificationListener : NotificationListenerService() {

  companion object {
    @Volatile
    var instance: ChildNotificationListener? = null
      private set

    val interceptedNotifications = CopyOnWriteArrayList<RemoteNotification>()
    var onNotificationIntercepted: ((RemoteNotification) -> Unit)? = null

    fun isServiceRunning(): Boolean = instance != null
  }

  private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

  override fun onListenerConnected() {
    super.onListenerConnected()
    instance = this
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    if (instance == this) {
      instance = null
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (instance == this) {
      instance = null
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val extras = sbn.notification?.extras ?: return

    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
      ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
      ?: ""

    if (title.isBlank() && text.isBlank()) return

    val pkg = sbn.packageName ?: "com.unknown"
    val pm = packageManager
    val appName = try {
      val appInfo = pm.getApplicationInfo(pkg, 0)
      pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
      pkg.substringAfterLast(".")
    }

    val notif = RemoteNotification(
      id = "notif-${sbn.id}-${sbn.postTime}",
      deviceId = "child-local",
      appName = appName,
      packageName = pkg,
      title = title,
      content = text,
      time = timeFormat.format(Date(sbn.postTime)),
      isRead = false
    )

    interceptedNotifications.add(0, notif)
    while (interceptedNotifications.size > 100) {
      interceptedNotifications.removeAt(interceptedNotifications.lastIndex)
    }

    onNotificationIntercepted?.invoke(notif)
  }
}
