package com.example.engine

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.example.service.AirDroidAccessibilityService

class TouchAutomationEngine(private val context: Context) {

    companion object {
        private const val TAG = "TouchAutomationEngine"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Checks if the AirDroid Accessibility Service is enabled in System Settings.
     */
    fun isAccessibilityPermissionGranted(): Boolean {
        if (AirDroidAccessibilityService.isConnected()) return true

        val expectedServiceName = "${context.packageName}/${AirDroidAccessibilityService::class.java.canonicalName}"
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.contains(AirDroidAccessibilityService::class.java.simpleName)
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility permission", e)
        }
        return false
    }

    /**
     * Maps scaled relative coordinates (0..1) or raw coordinates into device screen pixels.
     */
    fun performClick(normOrRawX: Float, normOrRawY: Float, isNormalized: Boolean = false, callback: ((Boolean) -> Unit)? = null): Boolean {
        val displayMetrics = Resources.getSystem().displayMetrics
        val finalX = if (isNormalized) normOrRawX * displayMetrics.widthPixels else normOrRawX
        val finalY = if (isNormalized) normOrRawY * displayMetrics.heightPixels else normOrRawY

        val service = AirDroidAccessibilityService.instance
        if (service == null) {
            Log.w(TAG, "Cannot dispatch touch: AirDroid Accessibility Service is not active")
            callback?.invoke(false)
            return false
        }

        return service.dispatchClick(finalX, finalY, callback)
    }

    /**
     * Performs a long press gesture.
     */
    fun performLongPress(normOrRawX: Float, normOrRawY: Float, isNormalized: Boolean = false, callback: ((Boolean) -> Unit)? = null): Boolean {
        val displayMetrics = Resources.getSystem().displayMetrics
        val finalX = if (isNormalized) normOrRawX * displayMetrics.widthPixels else normOrRawX
        val finalY = if (isNormalized) normOrRawY * displayMetrics.heightPixels else normOrRawY

        val service = AirDroidAccessibilityService.instance ?: return false
        return service.dispatchLongPress(finalX, finalY, 800, callback)
    }

    /**
     * Performs a swipe gesture.
     */
    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300,
        isNormalized: Boolean = false,
        callback: ((Boolean) -> Unit)? = null
    ): Boolean {
        val displayMetrics = Resources.getSystem().displayMetrics
        val sX = if (isNormalized) startX * displayMetrics.widthPixels else startX
        val sY = if (isNormalized) startY * displayMetrics.heightPixels else startY
        val eX = if (isNormalized) endX * displayMetrics.widthPixels else endX
        val eY = if (isNormalized) endY * displayMetrics.heightPixels else endY

        val service = AirDroidAccessibilityService.instance
        if (service == null) {
            Log.w(TAG, "Cannot dispatch swipe: Accessibility Service is not active")
            callback?.invoke(false)
            return false
        }

        return service.dispatchSwipe(sX, sY, eX, eY, durationMs, callback)
    }

    /**
     * Injects remote text input into focused editable field.
     */
    fun performTextInjection(text: String): Boolean {
        val service = AirDroidAccessibilityService.instance ?: return false
        return service.dispatchSetText(text)
    }

    /**
     * Performs simulated hardware buttons (Home, Back, Recents, Volume Up/Down, Lock).
     */
    fun performKeyEvent(keyName: String): Boolean {
        val service = AirDroidAccessibilityService.instance
        return when (keyName.uppercase()) {
            "HOME", "KEYCODE_HOME" -> service?.dispatchGlobalKey(1) ?: false
            "BACK", "KEYCODE_BACK" -> service?.dispatchGlobalKey(2) ?: false
            "RECENTS", "APP_SWITCH", "KEYCODE_APP_SWITCH" -> service?.dispatchGlobalKey(3) ?: false
            "NOTIFICATIONS" -> service?.dispatchGlobalKey(4) ?: false
            "QUICK_SETTINGS" -> service?.dispatchGlobalKey(5) ?: false
            "LOCK", "LOCK_SCREEN", "POWER" -> service?.dispatchGlobalKey(6) ?: false
            "VOLUME_UP", "KEYCODE_VOLUME_UP" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                true
            }
            "VOLUME_DOWN", "KEYCODE_VOLUME_DOWN" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                true
            }
            else -> false
        }
    }

    /**
     * Remotely launches an app by package name.
     */
    fun launchPackage(packageName: String): Boolean {
        val service = AirDroidAccessibilityService.instance ?: return false
        return service.launchAppByPackage(packageName)
    }
}
