package com.example.data.safety

import com.example.data.model.AlertSeverity
import com.example.data.model.KeywordRule
import com.example.data.model.SafetyCategory
import java.util.concurrent.CopyOnWriteArrayList

data class KeywordMatchResult(
  val rule: KeywordRule,
  val matchedText: String,
  val startOffset: Int,
  val endOffset: Int
)

/**
 * KeywordRuleEngine evaluates text streams against safety dictionaries and regex patterns.
 */
class KeywordRuleEngine {
  private val rules = CopyOnWriteArrayList<KeywordRule>()

  init {
    loadDefaultRules()
  }

  fun getRules(): List<KeywordRule> = rules.toList()

  fun addRule(rule: KeywordRule) {
    if (rules.none { it.id == rule.id || it.keyword.equals(rule.keyword, ignoreCase = true) }) {
      rules.add(rule)
    }
  }

  fun removeRule(ruleId: String) {
    rules.removeAll { it.id == ruleId }
  }

  fun evaluate(text: String): List<KeywordMatchResult> {
    if (text.isBlank()) return emptyList()
    val results = mutableListOf<KeywordMatchResult>()
    val lower = text.lowercase()

    for (rule in rules) {
      if (rule.isRegex) {
        try {
          val regex = Regex(rule.keyword, RegexOption.IGNORE_CASE)
          val match = regex.find(text)
          if (match != null) {
            results.add(
              KeywordMatchResult(
                rule = rule,
                matchedText = match.value,
                startOffset = match.range.first,
                endOffset = match.range.last
              )
            )
          }
        } catch (e: Exception) {
          // invalid regex ignored safely
        }
      } else {
        val target = rule.keyword.lowercase()
        val index = lower.indexOf(target)
        if (index >= 0) {
          results.add(
            KeywordMatchResult(
              rule = rule,
              matchedText = text.substring(index, index + target.length),
              startOffset = index,
              endOffset = index + target.length
            )
          )
        }
      }
    }
    return results
  }

  private fun loadDefaultRules() {
    // Bullying & Harassment
    rules.add(KeywordRule("kr-1", "kill yourself", SafetyCategory.BULLYING, AlertSeverity.CRITICAL))
    rules.add(KeywordRule("kr-2", "nobody likes you", SafetyCategory.BULLYING, AlertSeverity.HIGH))
    rules.add(KeywordRule("kr-3", "loser", SafetyCategory.BULLYING, AlertSeverity.MEDIUM))
    rules.add(KeywordRule("kr-4", "ugly and stupid", SafetyCategory.BULLYING, AlertSeverity.HIGH))

    // Violence & Threats
    rules.add(KeywordRule("kr-5", "beat you up", SafetyCategory.VIOLENCE, AlertSeverity.HIGH))
    rules.add(KeywordRule("kr-6", "fight after school", SafetyCategory.VIOLENCE, AlertSeverity.HIGH))
    rules.add(KeywordRule("kr-7", "bring a weapon", SafetyCategory.VIOLENCE, AlertSeverity.CRITICAL))

    // Self-Harm
    rules.add(KeywordRule("kr-8", "want to end my life", SafetyCategory.SELF_HARM, AlertSeverity.CRITICAL))
    rules.add(KeywordRule("kr-9", "hurting myself", SafetyCategory.SELF_HARM, AlertSeverity.CRITICAL))

    // Suspicious Contacts / Predatory Behavior
    rules.add(KeywordRule("kr-10", "don't tell your parents", SafetyCategory.SUSPICIOUS_CONTACT, AlertSeverity.CRITICAL))
    rules.add(KeywordRule("kr-11", "send me a picture of you", SafetyCategory.SUSPICIOUS_CONTACT, AlertSeverity.CRITICAL))
    rules.add(KeywordRule("kr-12", "meet me alone", SafetyCategory.SUSPICIOUS_CONTACT, AlertSeverity.CRITICAL))

    // AI Safety & Risky Prompts
    rules.add(KeywordRule("kr-13", "bypass parental controls", SafetyCategory.AI_SENSITIVE_PROMPT, AlertSeverity.HIGH))
    rules.add(KeywordRule("kr-14", "how to hide apps from parents", SafetyCategory.AI_SENSITIVE_PROMPT, AlertSeverity.HIGH))
    rules.add(KeywordRule("kr-15", "generate malware script", SafetyCategory.AI_SENSITIVE_PROMPT, AlertSeverity.CRITICAL))
    rules.add(KeywordRule("kr-16", "pretend you are my romantic partner", SafetyCategory.AI_SENSITIVE_PROMPT, AlertSeverity.HIGH))
  }
}
