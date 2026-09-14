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
import com.example.data.model.ActivityTimelineEvent
import com.example.data.model.ChildDevice
import com.example.ui.theme.*

@Composable
fun EventsUsageScreen(
  device: ChildDevice?,
  timelineEvents: List<ActivityTimelineEvent>,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf("USAGE") } // "USAGE" or "TIMELINE"

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Header
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
            imageVector = Icons.Filled.QueryStats,
            contentDescription = null,
            tint = AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "Screen Time & Activity Reports • ${device?.name ?: "No child selected"}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (timelineEvents.isEmpty()) "No activity data received from Child" else "${timelineEvents.size} activity events received",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // View toggle
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilterChip(
            selected = selectedTab == "USAGE",
            onClick = { selectedTab = "USAGE" },
            label = { Text("Usage Reports", fontSize = 11.sp) },
            modifier = Modifier.testTag("tab_usage_reports")
          )
          FilterChip(
            selected = selectedTab == "TIMELINE",
            onClick = { selectedTab = "TIMELINE" },
            label = { Text("Activity Timeline (${timelineEvents.size})", fontSize = 11.sp) },
            modifier = Modifier.testTag("tab_timeline_events")
          )
        }
      }
    }

    when (selectedTab) {
      "USAGE" -> {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Top Summary Row (Today's Total vs Limit, Unlocks, Data Usage)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Screen Time Card
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TODAY'S SCREEN TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("—", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AirDroidGreen)
                LinearProgressIndicator(
                  progress = { 0f },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                  color = AirDroidGreen,
                  trackColor = AirDroidGreen.copy(alpha = 0.2f)
                )
                Text("Usage limit data is shown only when received from the Child device.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            // Unlocks & Pickups Card
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("DEVICE PICKUPS & UNLOCKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("—", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AirDroidCyan)
                Text("Unlock data unavailable", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No synthetic pickup metrics", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            // Data Usage Card
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TODAY'S DATA USAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("—", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Network usage unavailable", fontSize = 10.sp, color = AirDroidGreen)
                Text("No synthetic network totals", fontSize = 10.sp, color = AirDroidCyan)
              }
            }
          }

          // Category Usage Distribution
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                "USAGE BY CATEGORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              // Multi-color progress bar
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(5.dp))
              ) {
                Box(modifier = Modifier.weight(0.50f).fillMaxHeight().background(Color(0xFFE11D48))) // Entertainment
                Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFF8B5CF6))) // Gaming
                Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(AirDroidCyan))       // Social
                Box(modifier = Modifier.weight(0.10f).fillMaxHeight().background(AirDroidGreen))     // Education
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE11D48)))
                  Text("Entertainment: 1h 50m (50%)", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                  Text("Gaming: 55m (25%)", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AirDroidCyan))
                  Text("Social: 33m (15%)", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AirDroidGreen))
                  Text("Education: 24m (10%)", fontSize = 10.sp)
                }
              }
            }
          }

          // Weekly Usage Bar Chart
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().weight(1f)
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                "PAST 7 DAYS SCREEN TIME TREND",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              val days = listOf(
                Pair("Mon", 210),
                Pair("Tue", 195),
                Pair("Wed", 240),
                Pair("Thu", 180),
                Pair("Fri", 280),
                Pair("Sat", 320),
                Pair("Sun", 222)
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
              ) {
                days.forEach { (day, minutes) ->
                  val heightFraction = (minutes / 360f).coerceIn(0.1f, 1.0f)
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text("${minutes / 60}h ${minutes % 60}m", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                      modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(heightFraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (day == "Sun") AirDroidGreen else AirDroidCyan.copy(alpha = 0.7f))
                    )
                    Text(day, fontSize = 11.sp, fontWeight = if (day == "Sun") FontWeight.Bold else FontWeight.Normal)
                  }
                }
              }
            }
          }
        }
      }

      "TIMELINE" -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(timelineEvents) { ev ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = when (ev.iconType) {
                    "APP_LAUNCH" -> AirDroidCyan.copy(alpha = 0.2f)
                    "APP_BLOCKED" -> StatusOffline.copy(alpha = 0.2f)
                    "GEOFENCE_ENTER" -> AirDroidGreen.copy(alpha = 0.2f)
                    "DEVICE_UNLOCK" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surface
                  },
                  modifier = Modifier.size(38.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = when (ev.iconType) {
                        "APP_LAUNCH" -> Icons.Filled.OpenInNew
                        "APP_BLOCKED" -> Icons.Filled.Block
                        "GEOFENCE_ENTER" -> Icons.Filled.LocationOn
                        "DEVICE_UNLOCK" -> Icons.Filled.LockOpen
                        else -> Icons.Filled.Notifications
                      },
                      contentDescription = null,
                      tint = when (ev.iconType) {
                        "APP_LAUNCH" -> AirDroidCyan
                        "APP_BLOCKED" -> StatusOffline
                        "GEOFENCE_ENTER" -> AirDroidGreen
                        "DEVICE_UNLOCK" -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.onSurface
                      },
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = ev.title,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = ev.time,
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Text(
                    text = ev.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
