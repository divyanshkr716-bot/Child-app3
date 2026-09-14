package com.example.data

import android.util.Log

/**
 * Structured Logger adhering to security & privacy guidelines.
 * Categories: PAIRING, CONNECTION, COMMAND, COMMAND_RESULT, PERMISSION, STREAM, LOCATION, POLICY, EVENT, ERROR
 * Ensures sensitive tokens/passwords are never logged in plain text.
 */
object AppLogger {
  private const val TAG = "AirDroidSystem"

  enum class Category {
    PAIRING,
    CONNECTION,
    COMMAND,
    COMMAND_RESULT,
    PERMISSION,
    STREAM,
    LOCATION,
    POLICY,
    EVENT,
    ERROR
  }

  fun log(category: Category, message: String, error: Throwable? = null) {
    val sanitized = sanitize(message)
    val formatted = "[$category] $sanitized"
    if (category == Category.ERROR || error != null) {
      Log.e(TAG, formatted, error)
    } else {
      Log.i(TAG, formatted)
    }
  }

  private fun sanitize(input: String): String {
    // Redact potential tokens, secrets, or keys
    return input
      .replace(Regex("(?i)(password|token|secret|key)=\\S+"), "$1=***REDACTED***")
      .replace(Regex("(?i)bearer\\s+[A-Za-z0-9-_.]+"), "Bearer ***REDACTED***")
  }
}
