package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.model.ConnectionMode
import com.example.data.model.UserSession
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
  selectedDevice: ChildDevice?,
  devices: List<ChildDevice>,
  onSelectDevice: (String) -> Unit,
  onToggleConnectionMode: () -> Unit,
  latencyMs: Int,
  fps: Int,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  userSession: UserSession
) {
  var showDeviceMenu by remember { mutableStateOf(false) }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Brand Logo & Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AirDroidGreen),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.NearMe,
            contentDescription = "AirDroid Logo",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "AirDroid",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
              color = AirDroidGreen.copy(alpha = 0.18f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "PARENT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = AirDroidGreen,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }
          }
          Text(
            text = "Desktop Client & AirMirror Hub",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Active Device Selector Dropdown
        selectedDevice?.let { dev ->
          Box {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .clickable { showDeviceMenu = true }
                .testTag("device_selector_dropdown")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (dev.isOnline) StatusOnline else StatusOffline)
                )
                Text(
                  text = dev.name,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Select Device",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            DropdownMenu(
              expanded = showDeviceMenu,
              onDismissRequest = { showDeviceMenu = false }
            ) {
              devices.forEach { d ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(d.name, fontWeight = FontWeight.SemiBold)
                      Text(
                        "${d.model} • ${d.batteryPercent}% • ${d.lastSeen}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = if (d.model.contains("Tab", ignoreCase = true)) Icons.Filled.TabletAndroid else Icons.Filled.Smartphone,
                      contentDescription = null,
                      tint = if (d.isOnline) AirDroidGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                  onClick = {
                    onSelectDevice(d.id)
                    showDeviceMenu = false
                  }
                )
              }
            }
          }
        }
      }

      // Center: Dual Connection Mode Pill & Telemetry Indicators
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        selectedDevice?.let { dev ->
          // Mode Toggle Badge
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (dev.connectionMode == ConnectionMode.LOCAL_P2P)
              StatusLocalP2P.copy(alpha = 0.16f)
            else
              StatusCloudRelay.copy(alpha = 0.16f),
            modifier = Modifier
              .clickable { onToggleConnectionMode() }
              .testTag("toggle_connection_mode_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (dev.connectionMode == ConnectionMode.LOCAL_P2P) Icons.Filled.Wifi else Icons.Filled.CloudSync,
                contentDescription = null,
                tint = if (dev.connectionMode == ConnectionMode.LOCAL_P2P) StatusLocalP2P else StatusCloudRelay,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = dev.connectionMode.badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dev.connectionMode == ConnectionMode.LOCAL_P2P) StatusLocalP2P else StatusCloudRelay
              )
              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Switch Mode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
              )
            }
          }

          // Latency & FPS Badge
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (latencyMs < 50) StatusOnline else StatusWarning)
                )
                Text(
                  text = "${latencyMs}ms",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Text(
                text = "$fps FPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // E2EE Lock Icon
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "TLS / E2EE Encrypted Session",
                tint = StatusOnline,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }

      // Right: Theme Toggle & User Session
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        IconButton(
          onClick = onToggleTheme,
          modifier = Modifier
            .size(36.dp)
            .testTag("theme_toggle_button")
        ) {
          Icon(
            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = "Toggle Dark/Light Theme",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        // User Account Chip
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .testTag("user_account_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AirDroidCyan),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = userSession.name.firstOrNull()?.toString() ?: "P",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
            Text(
              text = userSession.name,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}
