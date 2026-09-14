package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.ChildDevice
import com.example.data.model.ManagedApp
import com.example.ui.theme.*

@Composable
fun AppManagementScreen(
  device: ChildDevice?,
  apps: List<ManagedApp>,
  onToggleBlock: (String, Boolean) -> Unit,
  onSetDailyLimit: (String, Int) -> Unit,
  onToggleAlwaysAllowed: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedCategory by remember { mutableStateOf("ALL") }
  var blockNewApps by remember { mutableStateOf(true) }
  var bedtimeDowntimeEnabled by remember { mutableStateOf(true) }
  var studyDowntimeEnabled by remember { mutableStateOf(true) }
  var editingAppLimit by remember { mutableStateOf<ManagedApp?>(null) }

  val filteredApps = apps.filter { app ->
    if (selectedCategory == "ALL") true else app.category.equals(selectedCategory, ignoreCase = true)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Bar Header
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Apps,
            contentDescription = null,
            tint = AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "App Usage & Time Limits • ${device?.name ?: "Child Device"}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${apps.count { it.isBlocked }} blocked • ${apps.size} installed applications",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Master Block Newly Installed Apps Toggle
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Auto-Block New Apps",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Switch(
            checked = blockNewApps,
            onCheckedChange = { blockNewApps = it },
            modifier = Modifier.testTag("toggle_block_new_apps")
          )
        }
      }
    }

    // Scheduled Downtimes Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Bedtime Mode Card
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.weight(1f)
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Filled.Bedtime, contentDescription = null, tint = AirDroidCyan, modifier = Modifier.size(20.dp))
            Column {
              Text("Bedtime Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("09:30 PM - 07:00 AM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(
            checked = bedtimeDowntimeEnabled,
            onCheckedChange = { bedtimeDowntimeEnabled = it }
          )
        }
      }

      // School Study Mode Card
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.weight(1f)
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Filled.School, contentDescription = null, tint = AirDroidGreen, modifier = Modifier.size(20.dp))
            Column {
              Text("School Study Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("08:30 AM - 03:00 PM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(
            checked = studyDowntimeEnabled,
            onCheckedChange = { studyDowntimeEnabled = it }
          )
        }
      }
    }

    // Category Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf("ALL", "Entertainment", "Gaming", "Social", "Education").forEach { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { selectedCategory = cat },
          label = { Text(cat, fontSize = 11.sp) },
          modifier = Modifier.testTag("app_cat_${cat.lowercase()}")
        )
      }
    }

    // Apps List
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(filteredApps) { app ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (app.isBlocked) StatusOffline.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier.fillMaxWidth().testTag("app_item_${app.packageName}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // App Info Left
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (app.category) {
                  "Entertainment" -> Color(0xFFE11D48).copy(alpha = 0.2f)
                  "Gaming" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                  "Social" -> AirDroidCyan.copy(alpha = 0.2f)
                  else -> AirDroidGreen.copy(alpha = 0.2f)
                },
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = when (app.category) {
                      "Entertainment" -> Icons.Filled.PlayCircle
                      "Gaming" -> Icons.Filled.SportsEsports
                      "Social" -> Icons.Filled.Forum
                      else -> Icons.Filled.School
                    },
                    contentDescription = null,
                    tint = when (app.category) {
                      "Entertainment" -> Color(0xFFE11D48)
                      "Gaming" -> Color(0xFF8B5CF6)
                      "Social" -> AirDroidCyan
                      else -> AirDroidGreen
                    },
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = app.appName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  if (app.isAlwaysAllowed) {
                    Surface(shape = RoundedCornerShape(4.dp), color = AirDroidGreen.copy(alpha = 0.2f)) {
                      Text("ALWAYS ALLOWED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AirDroidGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                  }
                }
                Text(
                  text = "${app.packageName} • ${app.category}",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Today's Usage: ${app.usageMinutesToday}m ${if (app.dailyLimitMinutes > 0) "/ Limit: ${app.dailyLimitMinutes}m" else "• No Limit"}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (app.dailyLimitMinutes > 0 && app.usageMinutesToday >= app.dailyLimitMinutes) StatusOffline else AirDroidGreen
                )
              }
            }

            // Actions Right
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Edit Limit Button
              OutlinedButton(
                onClick = { editingAppLimit = app },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = if (app.dailyLimitMinutes > 0) "${app.dailyLimitMinutes}m" else "Set Limit",
                  fontSize = 11.sp
                )
              }

              // Always Allowed Star Toggle
              IconButton(
                onClick = { onToggleAlwaysAllowed(app.packageName, app.isAlwaysAllowed) },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = if (app.isAlwaysAllowed) Icons.Filled.Star else Icons.Outlined.StarBorder,
                  contentDescription = "Always Allowed",
                  tint = if (app.isAlwaysAllowed) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
              }

              // Block / Unblock Switch
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                  checked = !app.isBlocked,
                  onCheckedChange = { onToggleBlock(app.packageName, app.isBlocked) },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = AirDroidGreen,
                    checkedTrackColor = AirDroidGreen.copy(alpha = 0.5f),
                    uncheckedThumbColor = StatusOffline,
                    uncheckedTrackColor = StatusOffline.copy(alpha = 0.3f)
                  ),
                  modifier = Modifier.testTag("app_block_switch_${app.packageName}")
                )
                Text(
                  text = if (app.isBlocked) "BLOCKED" else "ALLOWED",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (app.isBlocked) StatusOffline else AirDroidGreen
                )
              }
            }
          }
        }
      }
    }
  }

  // Daily Limit Dialog
  editingAppLimit?.let { app ->
    var limitMinutes by remember { mutableStateOf(if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes else 60) }

    AlertDialog(
      onDismissRequest = { editingAppLimit = null },
      title = { Text("Set Daily Time Limit for ${app.appName}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Allow usage up to: $limitMinutes minutes/day", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Slider(
            value = limitMinutes.toFloat(),
            onValueChange = { limitMinutes = it.toInt() },
            valueRange = 15f..240f,
            steps = 14
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            listOf(30, 60, 90, 120).forEach { m ->
              OutlinedButton(
                onClick = { limitMinutes = m },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text("${m}m", fontSize = 11.sp)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSetDailyLimit(app.packageName, limitMinutes)
            editingAppLimit = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen)
        ) {
          Text("Apply Limit")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            onSetDailyLimit(app.packageName, 0) // unlimited
            editingAppLimit = null
          }
        ) {
          Text("Remove Limit")
        }
      }
    )
  }
}
