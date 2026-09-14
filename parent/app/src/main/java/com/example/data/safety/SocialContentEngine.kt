package com.example.data.safety

import com.example.data.AppLogger
import com.example.data.model.AlertSeverity
import com.example.data.model.DetectionEvent
import com.example.data.model.SafetyCategory
import java.util.UUID

/**
 * SocialContentEngine:
 * Monitors permitted text content from approved social apps (WhatsApp, Instagram, Discord, Snapchat, TikTok, Telegram)
 * through authorized Android accessibility and notification listeners without bypassing platform boundaries.
 */
class SocialContentEngine(
  private val keywordRuleEngine: KeywordRuleEngine
) {
  private val supportedSocialPackages = mapOf(
    "com.whatsapp" to "WhatsApp",
    "com.instagram.android" to "Instagram",
    "com.discord" to "Discord",
    "com.snapchat.android" to "Snapchat",
    "com.zhiliaoapp.musically" to "TikTok",
    "org.telegram.messenger" to "Telegram"
  )

  fun isSupportedSocialApp(packageName: String): Boolean {
    return supportedSocialPackages.containsKey(packageName)
  }

  fun getSocialAppName(packageName: String): String {
    return supportedSocialPackages[packageName] ?: "Social App"
  }

  /**
   * Evaluates text captured legally from social app events.
   * Generates a DetectionEvent if a safety rule triggers.
   */
  fun processText(
    deviceId: String,
    packageName: String,
    text: String,
    sender: String = ""
  ): List<DetectionEvent> {
    val matches = keywordRuleEngine.evaluate(text)
    if (matches.isEmpty()) return emptyList()

    val appName = getSocialAppName(packageName)
    val events = matches.map { match ->
      val snippet = createSafeSnippet(text, match.startOffset, match.endOffset)
      AppLogger.log(
        AppLogger.Category.EVENT,
        "Safety rule triggered on $appName: ${match.rule.keyword} (Severity: ${match.rule.severity})"
      )
      DetectionEvent(
        id = "det-${UUID.randomUUID().toString().take(8)}",
        deviceId = deviceId,
        appName = appName,
        packageName = packageName,
        keyword = match.rule.keyword,
        severity = match.rule.severity,
        category = match.rule.category,
        contextSnippet = if (sender.isNotBlank()) "[$sender]: $snippet" else snippet,
        timestamp = "Just now",
        isReviewed = false
      )
    }
    return events
  }

  private fun createSafeSnippet(fullText: String, start: Int, end: Int): String {
    val prefix = (start - 25).coerceAtLeast(0)
    val suffix = (end + 25).coerceAtMost(fullText.length)
    val prefixEllipsis = if (prefix > 0) "..." else ""
    val suffixEllipsis = if (suffix < fullText.length) "..." else ""
    return "$prefixEllipsis${fullText.substring(prefix, suffix)}$suffixEllipsis"
  }
}
