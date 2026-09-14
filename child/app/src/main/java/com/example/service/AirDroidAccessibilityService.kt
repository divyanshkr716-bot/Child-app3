package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.AirDroidChildApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AirDroidAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AirDroidA11yService"

        @Volatile
        var instance: AirDroidAccessibilityService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _foregroundPackage = MutableStateFlow("")
        val foregroundPackage: StateFlow<String> = _foregroundPackage.asStateFlow()

        fun isConnected(): Boolean = instance != null

    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceRunning.value = true

        try {
            val info = serviceInfo ?: android.accessibilityservice.AccessibilityServiceInfo()
            info.flags = android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 100
            serviceInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Could not set dynamic serviceInfo", e)
        }

        Log.i(TAG, "AirDroid Accessibility Service connected and ready for automation")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkgName = event.packageName?.toString() ?: return
        if (pkgName.isNotEmpty() && pkgName != packageName) {
            _foregroundPackage.value = pkgName
            checkAndEnforceAppBlock(pkgName)
            checkAndEnforceDailyLimit(pkgName)
        }
    }

    private fun checkAndEnforceAppBlock(pkgName: String) {
        val stealthMgr = AirDroidChildApp.stealthManager ?: return
        if (stealthMgr.isPackageBlocked(pkgName)) {
            Log.w(TAG, "Child opened blocked package: $pkgName. Enforcing parental restriction!")
            performGlobalAction(GLOBAL_ACTION_HOME)
            serviceScope.launch {
                AirDroidChildApp.repository?.logAction(
                    "APP_BLOCKED",
                    "Parent restricted app $pkgName was closed automatically",
                    isSuccess = true
                )
            }
        }
    }


    private fun checkAndEnforceDailyLimit(pkgName: String) {
        val limit = AirDroidChildApp.stealthManager?.dailyLimits?.value?.get(pkgName) ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        try {
            val usage = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }
            val stats = usage.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis())
            val usedMinutes = stats.filter { it.packageName == pkgName }.sumOf { it.totalTimeInForeground } / 60_000L
            if (usedMinutes >= limit) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                serviceScope.launch { AirDroidChildApp.repository?.logAction("APP_DAILY_LIMIT_REACHED", "$pkgName reached its $limit minute daily limit", isSuccess = true) }
            }
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {
        Log.w(TAG, "AirDroid Accessibility Service interrupted")
        _isServiceRunning.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceRunning.value = false
        }
        Log.i(TAG, "AirDroid Accessibility Service destroyed")
    }

    /**
     * Dispatches a single click at the specified (x, y) coordinates.
     */
    fun dispatchClick(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return false
        }

        val clickPath = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Dispatches a long press at the specified (x, y) coordinates.
     */
    fun dispatchLongPress(x: Float, y: Float, durationMs: Long = 800, callback: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return false
        }

        val clickPath = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Dispatches a swipe or drag gesture from (startX, startY) to (endX, endY).
     */
    fun dispatchSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300,
        callback: ((Boolean) -> Unit)? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            callback?.invoke(false)
            return false
        }

        val swipePath = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val stroke = GestureDescription.StrokeDescription(swipePath, 0, durationMs.coerceAtLeast(100))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Remotely injects text into the currently active editable input field.
     */
    fun dispatchSetText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "Remotely injected text: \"$text\" with result=$result")
            return result
        }
        return false
    }

    /**
     * Dispatches simulated hardware buttons via Global Actions.
     */
    fun dispatchGlobalKey(actionId: Int): Boolean {
        val globalAction = when (actionId) {
            1 -> GLOBAL_ACTION_HOME
            2 -> GLOBAL_ACTION_BACK
            3 -> GLOBAL_ACTION_RECENTS
            4 -> GLOBAL_ACTION_NOTIFICATIONS
            5 -> GLOBAL_ACTION_QUICK_SETTINGS
            6 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) GLOBAL_ACTION_LOCK_SCREEN else GLOBAL_ACTION_BACK
            else -> return false
        }
        return performGlobalAction(globalAction)
    }

    /**
     * Remotely launches an app by package name.
     */
    fun launchAppByPackage(pkg: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $pkg", e)
            false
        }
    }
}
