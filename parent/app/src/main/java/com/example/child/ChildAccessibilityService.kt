package com.example.child

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.safety.KeywordRuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

class ChildAccessibilityService : AccessibilityService() {

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  companion object {
    @Volatile
    var instance: ChildAccessibilityService? = null
      private set

    val blockedPackages = CopyOnWriteArraySet<String>()
    @Volatile
    var isInstantBlockActive = false
    @Volatile
    var isDowntimeActive = false
    val whitelistedDowntimePackages = CopyOnWriteArraySet(
      listOf("com.google.android.dialer", "com.google.android.apps.messaging", "com.android.phone", "com.android.contacts")
    )

    var onSafetyTextDetected: ((packageName: String, text: String) -> Unit)? = null

    fun isServiceRunning(): Boolean = instance != null
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
  }

  override fun onDestroy() {
    super.onDestroy()
    if (instance == this) {
      instance = null
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    val pkgName = event.packageName?.toString() ?: return

    // 1. App Blocking Enforcement
    val shouldBlock = when {
      blockedPackages.contains(pkgName) -> true
      isInstantBlockActive && !whitelistedDowntimePackages.contains(pkgName) -> true
      isDowntimeActive && !whitelistedDowntimePackages.contains(pkgName) -> true
      else -> false
    }

    if (shouldBlock && pkgName != packageName && !pkgName.contains("launcher")) {
      performGlobalAction(GLOBAL_ACTION_HOME)
    }

    // 2. Real-time text extraction for social/AI safety
    val eventText = event.text.joinToString(" ")
    if (eventText.isNotBlank()) {
      onSafetyTextDetected?.invoke(pkgName, eventText)
    }
  }

  override fun onInterrupt() {}

  // --- Remote Gestures Dispatched from Parent UI ---
  fun performRemoteTap(xPercent: Float, yPercent: Float): Boolean {
    val displayMetrics = resources.displayMetrics
    val screenX = (xPercent * displayMetrics.widthPixels).coerceIn(0f, displayMetrics.widthPixels.toFloat())
    val screenY = (yPercent * displayMetrics.heightPixels).coerceIn(0f, displayMetrics.heightPixels.toFloat())

    val path = Path().apply {
      moveTo(screenX, screenY)
    }
    val stroke = GestureDescription.StrokeDescription(path, 0, 50)
    val gesture = GestureDescription.Builder().addStroke(stroke).build()
    return dispatchGesture(gesture, null, null)
  }

  fun performRemoteSwipe(x1Percent: Float, y1Percent: Float, x2Percent: Float, y2Percent: Float, durationMs: Long = 300): Boolean {
    val displayMetrics = resources.displayMetrics
    val startX = (x1Percent * displayMetrics.widthPixels).coerceIn(0f, displayMetrics.widthPixels.toFloat())
    val startY = (y1Percent * displayMetrics.heightPixels).coerceIn(0f, displayMetrics.heightPixels.toFloat())
    val endX = (x2Percent * displayMetrics.widthPixels).coerceIn(0f, displayMetrics.widthPixels.toFloat())
    val endY = (y2Percent * displayMetrics.heightPixels).coerceIn(0f, displayMetrics.heightPixels.toFloat())

    val path = Path().apply {
      moveTo(startX, startY)
      lineTo(endX, endY)
    }
    val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(50, 1000))
    val gesture = GestureDescription.Builder().addStroke(stroke).build()
    return dispatchGesture(gesture, null, null)
  }

  fun performGoHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
  fun performGoBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
  fun performShowRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
}
