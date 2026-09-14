package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores child-side parental restriction state.
 * Deliberately does not hide the app, disguise it, suppress notifications, or auto-approve Android dialogs.
 */
class StealthManager(context: Context) {
    companion object { private const val PREFS_NAME = "child_restriction_prefs" }
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isSetupCompleted = MutableStateFlow(prefs.getBoolean("setup_completed", false))
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _blockedPackages = MutableStateFlow(
        prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
    )
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    private val _dailyLimits = MutableStateFlow(loadLimits())
    val dailyLimits: StateFlow<Map<String, Int>> = _dailyLimits.asStateFlow()

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean("setup_completed", completed).apply()
        _isSetupCompleted.value = completed
    }

    fun isPackageBlocked(packageName: String): Boolean = _blockedPackages.value.contains(packageName)

    fun addBlockedPackage(packageName: String) {
        val updated = _blockedPackages.value.toMutableSet().apply { add(packageName) }
        prefs.edit().putStringSet("blocked_packages", updated).apply()
        _blockedPackages.value = updated
    }

    fun removeBlockedPackage(packageName: String) {
        val updated = _blockedPackages.value.toMutableSet().apply { remove(packageName) }
        prefs.edit().putStringSet("blocked_packages", updated).apply()
        _blockedPackages.value = updated
    }

    fun setDailyLimit(packageName: String, minutes: Int) {
        val updated = _dailyLimits.value.toMutableMap()
        if (minutes > 0) updated[packageName] = minutes else updated.remove(packageName)
        prefs.edit().putString("daily_limits", updated.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
        _dailyLimits.value = updated
    }

    private fun loadLimits(): Map<String, Int> = prefs.getString("daily_limits", "").orEmpty()
        .split(';').mapNotNull { part ->
            val kv = part.split('=', limit = 2)
            if (kv.size == 2) kv[0].takeIf { it.isNotBlank() }?.let { pkg -> pkg to (kv[1].toIntOrNull() ?: 0) } else null
        }.filter { it.second > 0 }.toMap()
}
