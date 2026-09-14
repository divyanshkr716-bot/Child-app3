package com.example.data.safety

import com.example.data.AppLogger
import com.example.data.model.AlertSeverity
import com.example.data.model.DetectionEvent
import com.example.data.model.SafetyCategory
import java.util.UUID

/**
 * AIContentEngine:
 * Monitors interaction prompts and responses in supported AI Chat applications
 * (ChatGPT, Google Gemini, Character.ai, Claude) via approved accessibility node inspection.
 */
class AIContentEngine(
  private val keywordRuleEngine: KeywordRuleEngine
) {
  private val supportedAiPackages = mapOf(
    "com.openai.chatgpt" to "ChatGPT",
    "com.google.android.apps.bard" to "Google Gemini",
    "ai.character.app" to "Character.ai",
    "com.anthropic.claude" to "Claude AI"
  )

  fun isSupportedAiApp(packageName: String): Boolean {
    return supportedAiPackages.containsKey(packageName)
  }

  fun getAiAppName(packageName: String): String {
    return supportedAiPackages[packageName] ?: "AI Assistant"
  }

  fun processAiPromptOrResponse(
    deviceId: String,
    packageName: String,
    content: String,
    isPrompt: Boolean = true
  ): List<DetectionEvent> {
    val matches = keywordRuleEngine.evaluate(content)
    if (matches.isEmpty()) return emptyList()

    val aiAppName = getAiAppName(packageName)
    val rolePrefix = if (isPrompt) "Child Prompt to AI" else "AI Model Response"

    val events = matches.map { match ->
      AppLogger.log(
        AppLogger.Category.EVENT,
        "AI Chat safety event on $aiAppName ($rolePrefix): ${match.rule.keyword}"
      )
      DetectionEvent(
        id = "ai-${UUID.randomUUID().toString().take(8)}",
        deviceId = deviceId,
        appName = aiAppName,
        packageName = packageName,
        keyword = match.rule.keyword,
        severity = match.rule.severity,
        category = match.rule.category,
        contextSnippet = "[$rolePrefix]: ${content.take(160)}",
        timestamp = "Just now",
        isReviewed = false
      )
    }
    return events
  }
}
