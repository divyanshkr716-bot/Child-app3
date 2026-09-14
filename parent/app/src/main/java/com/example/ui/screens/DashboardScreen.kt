package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveTab
import com.example.data.model.ChildDevice
import com.example.data.model.ConnectionMode
import com.example.data.model.RemoteNotification
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
  device: ChildDevice?,
  notifications: List<RemoteNotification>,
  onNavigateTab: (ActiveTab) -> Unit,
  onToggleConnectionMode: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (device == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("No device connected. Please pair a child device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    return
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Hero Card: Bound Device Live Status
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().testTag("dashboard_hero_card")
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(AirDroidGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.Smartphone,
                  contentDescription = null,
                  tint = AirDroidGreen,
                  modifier = Modifier.size(28.dp)
                )
              }
              Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = device.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(if (device.isOnline) StatusOnline else StatusOffline)
                  )
                }
                Text(
                  text = "${device.model} • ${device.osVersion}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Connection Mode Badge & Switch Action
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = if (device.connectionMode == ConnectionMode.LOCAL_P2P)
                StatusLocalP2P.copy(alpha = 0.2f)
              else
                StatusCloudRelay.copy(alpha = 0.2f),
              modifier = Modifier.clickable { onToggleConnectionMode() }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = if (device.connectionMode == ConnectionMode.LOCAL_P2P) Icons.Filled.Wifi else Icons.Filled.Cloud,
                  contentDescription = null,
                  tint = if (device.connectionMode == ConnectionMode.LOCAL_P2P) StatusLocalP2P else StatusCloudRelay,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = device.connectionMode.label,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (device.connectionMode == ConnectionMode.LOCAL_P2P) StatusLocalP2P else StatusCloudRelay
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // 4 Metadata Metrics: Battery, Wi-Fi, Storage, IP Address
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            MetricPill(
              icon = if (device.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Battery5Bar,
              iconColor = if (device.isCharging) StatusBatteryCharging else AirDroidGreen,
              title = "Battery",
              value = "${device.batteryPercent}%",
              subtext = if (device.isCharging) "Fast Charging" else "Discharging",
              modifier = Modifier.weight(1f)
            )

            MetricPill(
              icon = Icons.Filled.Wifi,
              iconColor = StatusLocalP2P,
              title = "Wi-Fi Signal",
              value = "${device.wifiSignalDbm} dBm",
              subtext = device.wifiSsid,
              modifier = Modifier.weight(1f)
            )

            MetricPill(
              icon = Icons.Filled.Storage,
              iconColor = AirDroidCyan,
              title = "Storage",
              value = "${device.storageUsedGb.toInt()} GB",
              subtext = "of ${device.storageTotalGb.toInt()} GB used",
              modifier = Modifier.weight(1f)
            )

            MetricPill(
              icon = Icons.Filled.Sensors,
              iconColor = StatusCloudRelay,
              title = "Network IP",
              value = device.ipAddress,
              subtext = "TLS 1.3 Active",
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Storage Progress Bar
          val storagePercent = (device.storageUsedGb / device.storageTotalGb).toFloat()
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Internal Storage Usage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("${(storagePercent * 100).toInt()}% Used", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { storagePercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = AirDroidGreen,
              trackColor = MaterialTheme.colorScheme.surface
            )
          }
        }
      }
    }

    // Quick Action Module Tiles
    item {
      Text(
        text = "CORE MODULES",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickActionCard(
          title = "AirMirror Stream",
          description = "Live canvas screen mirror & remote control mapping",
          icon = Icons.Filled.ScreenShare,
          accentColor = AirDroidGreen,
          tag = "dashboard_action_mirror",
          onClick = { onNavigateTab(ActiveTab.AIR_MIRROR) },
          modifier = Modifier.weight(1f)
        )

        QuickActionCard(
          title = "Dual-Pane Files",
          description = "PC <-> Child Storage transfer & media gallery view",
          icon = Icons.Filled.FolderShared,
          accentColor = AirDroidCyan,
          tag = "dashboard_action_files",
          onClick = { onNavigateTab(ActiveTab.FILE_TRANSFER) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickActionCard(
          title = "SMS & Notification Hub",
          description = "Real-time alerts, quick replies & cellular SMS",
          icon = Icons.Filled.Chat,
          accentColor = Color(0xFFF59E0B),
          tag = "dashboard_action_sms",
          onClick = { onNavigateTab(ActiveTab.MESSAGES) },
          modifier = Modifier.weight(1f)
        )

        QuickActionCard(
          title = "Remote Camera",
          description = "Live surveillance feed, flashlight & remote snapshot",
          icon = Icons.Filled.Videocam,
          accentColor = Color(0xFFEC4899),
          tag = "dashboard_action_camera",
          onClick = { onNavigateTab(ActiveTab.REMOTE_CAMERA) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        QuickActionCard(
          title = "Social & AI Safety",
          description = "Keyword rules, grooming detection & content alerts",
          icon = Icons.Filled.Security,
          accentColor = Color(0xFF10B981),
          tag = "dashboard_action_social_safety",
          onClick = { onNavigateTab(ActiveTab.SOCIAL_SAFETY) },
          modifier = Modifier.weight(1f)
        )

        QuickActionCard(
          title = "Family Chat Hub",
          description = "Encrypted messaging, quick replies & requests",
          icon = Icons.Filled.Forum,
          accentColor = Color(0xFF6366F1),
          tag = "dashboard_action_family_chat",
          onClick = { onNavigateTab(ActiveTab.FAMILY_CHAT) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Recent Mobile Alerts & Notifications
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "RECENT MOBILE ALERTS",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = { onNavigateTab(ActiveTab.MESSAGES) }) {
          Text("View All (${notifications.size})", fontSize = 12.sp, color = AirDroidGreen)
        }
      }
    }

    items(notifications.take(3)) { notif ->
      Card(
        shape = RoundedCornerShape(12.dp),
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
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(AirDroidGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when {
                notif.appName.contains("WhatsApp", ignoreCase = true) -> Icons.Filled.Chat
                notif.appName.contains("Phone", ignoreCase = true) -> Icons.Filled.Call
                else -> Icons.Filled.Notifications
              },
              contentDescription = null,
              tint = AirDroidGreen,
              modifier = Modifier.size(20.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${notif.appName} • ${notif.title}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = notif.time,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = notif.content,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }

          IconButton(
            onClick = { onNavigateTab(ActiveTab.MESSAGES) },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.Reply,
              contentDescription = "Quick Reply",
              tint = AirDroidGreen,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun MetricPill(
  icon: ImageVector,
  iconColor: Color,
  title: String,
  value: String,
  subtext: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
      Text(subtext, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun QuickActionCard(
  title: String,
  description: String,
  icon: ImageVector,
  accentColor: Color,
  tag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    modifier = modifier
      .clickable { onClick() }
      .testTag(tag)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accentColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
      Spacer(modifier = Modifier.height(4.dp))
      Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
  }
}
