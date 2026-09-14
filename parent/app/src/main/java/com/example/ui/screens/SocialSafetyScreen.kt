package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.safety.KeywordRuleEngine
import com.example.ui.theme.*

@Composable
fun SocialSafetyScreen(
  device: ChildDevice?,
  detectionEvents: List<DetectionEvent>,
  imageDetections: List<ImageDetectionRecord>,
  onMarkReviewed: (String) -> Unit,
  onAddRule: (String, SafetyCategory, AlertSeverity) -> Unit,
  onRemoveRule: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedSubTab by remember { mutableStateOf("ALERTS") }
  var showAddRuleDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Header Banner
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(AirDroidGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.Security,
              contentDescription = null,
              tint = AirDroidGreen,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Social & AI Safety",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "On-device keyword, contact & image risk interception",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = AirDroidGreen.copy(alpha = 0.15f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AirDroidGreen)
            )
            Text(
              text = "Live Protection",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = AirDroidGreen
            )
          }
        }
      }
    }

    // Sub-Tabs Navigation
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf(
        Triple("ALERTS", "Alerts (${detectionEvents.count { !it.isReviewed }})", Icons.Filled.Warning),
        Triple("RULES", "Rules", Icons.Filled.Rule),
        Triple("IMAGES", "Image Scanner", Icons.Filled.ImageSearch),
      ).forEach { (tabKey, title, icon) ->
        val isSelected = selectedSubTab == tabKey
        Button(
          onClick = { selectedSubTab = tabKey },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) AirDroidGreen else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
          ),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f).testTag("social_safety_tab_$tabKey")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp))
            Text(text = title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
          }
        }
      }
    }

    // Tab Contents
    when (selectedSubTab) {
      "ALERTS" -> {
        DetectionAlertsList(
          events = detectionEvents,
          onMarkReviewed = onMarkReviewed
        )
      }
      "RULES" -> {
        KeywordRulesManager(
          onAddRuleClicked = { showAddRuleDialog = true }
        )
      }
      "IMAGES" -> {
        ImageSafetyList(
          images = imageDetections
        )
      }
    }
  }

  if (showAddRuleDialog) {
    AddKeywordRuleDialog(
      onDismiss = { showAddRuleDialog = false },
      onConfirm = { kw, cat, sev ->
        onAddRule(kw, cat, sev)
        showAddRuleDialog = false
      }
    )
  }
}

@Composable
private fun DetectionAlertsList(
  events: List<DetectionEvent>,
  onMarkReviewed: (String) -> Unit
) {
  if (events.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(200.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          tint = AirDroidGreen,
          modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("No safety issues detected", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Social apps and AI interactions are clean.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  } else {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize().testTag("detection_alerts_list")
    ) {
      items(events, key = { it.id }) { event ->
        DetectionEventCard(event = event, onMarkReviewed = { onMarkReviewed(event.id) })
      }
    }
  }
}

@Composable
private fun DetectionEventCard(
  event: DetectionEvent,
  onMarkReviewed: () -> Unit
) {
  val severityColor = when (event.severity) {
    AlertSeverity.CRITICAL -> Color(0xFFEF4444)
    AlertSeverity.HIGH -> Color(0xFFF97316)
    AlertSeverity.MEDIUM -> Color(0xFFF59E0B)
    AlertSeverity.LOW -> Color(0xFF3B82F6)
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.fillMaxWidth().testTag("detection_event_${event.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = severityColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = event.severity.name,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = severityColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Text(
            text = event.appName,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
          ) {
            Text(
              text = event.category.name.replace("_", " "),
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Text(
          text = event.timestamp,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Trigger & Context
      Text(
        text = "Keyword Triggered: \"${event.keyword}\"",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = severityColor
      )

      Spacer(modifier = Modifier.height(4.dp))

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "\"${event.contextSnippet}\"",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(8.dp)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (event.isReviewed) "✓ Reviewed by Parent" else "Needs Parent Review",
          fontSize = 10.sp,
          color = if (event.isReviewed) AirDroidGreen else Color(0xFFF97316),
          fontWeight = FontWeight.Medium
        )

        if (!event.isReviewed) {
          Button(
            onClick = onMarkReviewed,
            colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp).testTag("mark_reviewed_${event.id}")
          ) {
            Text("Mark Reviewed", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun KeywordRulesManager(
  onAddRuleClicked: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Active Protection Keywords & Patterns",
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
      Button(
        onClick = onAddRuleClicked,
        colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(30.dp).testTag("add_custom_rule_button")
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Custom Rule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      val defaultCategories = listOf(
        Pair("Grooming & Suspicious Contact", listOf("meet me alone", "secret place", "send pic", "don't tell parents", "delete chat")),
        Pair("Bullying & Harassment", listOf("kill yourself", "nobody likes you", "ugly loser", "hate you", "go away")),
        Pair("Self-Harm & Suicide", listOf("suicide", "end my life", "cut myself", "want to die", "pills overdose")),
        Pair("Substance & Narcotics", listOf("buy weed", "fentanyl", "vape pen", "dealer", "high pills")),
        Pair("AI Jailbreak & Bypasses", listOf("bypass parental controls", "jailbreak android", "hack wifi", "disable monitoring"))
      )

      items(defaultCategories) { (categoryName, rules) ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(categoryName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AirDroidGreen)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              rules.take(3).forEach { rule ->
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = MaterialTheme.colorScheme.surface
                ) {
                  Text(
                    text = rule,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              if (rules.size > 3) {
                Text("+${rules.size - 3} more", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ImageSafetyList(
  images: List<ImageDetectionRecord>
) {
  if (images.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          imageVector = Icons.Filled.VerifiedUser,
          contentDescription = null,
          tint = AirDroidGreen,
          modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text("No Flagged Images Found", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("Gallery and incoming media scanning is active.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  } else {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(images, key = { it.id }) { img ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Filled.ImageNotSupported, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
              }
              Column {
                Text(img.riskCategory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Confidence: ${(img.confidenceScore * 100).toInt()}% • ${img.timestamp}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFEF4444).copy(alpha = 0.15f)
            ) {
              Text("Flagged", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AddKeywordRuleDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, SafetyCategory, AlertSeverity) -> Unit
) {
  var keyword by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(SafetyCategory.BULLYING) }
  var selectedSeverity by remember { mutableStateOf(AlertSeverity.HIGH) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Custom Safety Keyword", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = keyword,
          onValueChange = { keyword = it },
          label = { Text("Keyword or Phrase") },
          modifier = Modifier.fillMaxWidth().testTag("add_rule_keyword_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (keyword.isNotBlank()) {
            onConfirm(keyword.trim(), selectedCategory, selectedSeverity)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
        modifier = Modifier.testTag("confirm_add_rule")
      ) {
        Text("Add Rule")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
