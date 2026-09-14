package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.theme.*

data class NavItem(
  val tab: ActiveTab,
  val icon: ImageVector,
  val selectedIcon: ImageVector,
  val badgeCount: Int = 0
)

@Composable
fun NavigationSidebar(
  activeTab: ActiveTab,
  onTabSelected: (ActiveTab) -> Unit,
  devices: List<ChildDevice>,
  selectedDeviceId: String,
  onSelectDevice: (String) -> Unit,
  unreadNotifsCount: Int,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(true) }

  val navItems = listOf(
    NavItem(ActiveTab.DASHBOARD, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    NavItem(ActiveTab.AIR_MIRROR, Icons.Outlined.ScreenShare, Icons.Filled.ScreenShare),
    NavItem(ActiveTab.REMOTE_CAMERA, Icons.Outlined.Videocam, Icons.Filled.Videocam),
    NavItem(ActiveTab.SOCIAL_SAFETY, Icons.Outlined.Security, Icons.Filled.Security),
    NavItem(ActiveTab.FAMILY_CHAT, Icons.Outlined.Forum, Icons.Filled.Forum),
    NavItem(ActiveTab.LOCATION, Icons.Outlined.LocationOn, Icons.Filled.LocationOn),
    NavItem(ActiveTab.APP_MANAGEMENT, Icons.Outlined.Apps, Icons.Filled.Apps),
    NavItem(ActiveTab.EVENTS_USAGE, Icons.Outlined.QueryStats, Icons.Filled.QueryStats),
    NavItem(ActiveTab.REQUESTS_ALERTS, Icons.Outlined.NotificationsActive, Icons.Filled.NotificationsActive),
    NavItem(ActiveTab.WEB_MONITOR, Icons.Outlined.Language, Icons.Filled.Language),
    NavItem(ActiveTab.DRIVING_SAFETY, Icons.Outlined.DirectionsCar, Icons.Filled.DirectionsCar),
    NavItem(ActiveTab.FILE_TRANSFER, Icons.Outlined.FolderShared, Icons.Filled.FolderShared),
    NavItem(ActiveTab.MESSAGES, Icons.Outlined.Chat, Icons.Filled.Chat, badgeCount = unreadNotifsCount),
    NavItem(ActiveTab.CALLS_CONTACTS, Icons.Outlined.Contacts, Icons.Filled.Contacts),
    NavItem(ActiveTab.DEVICE_HEALTH, Icons.Outlined.HealthAndSafety, Icons.Filled.HealthAndSafety),
    NavItem(ActiveTab.SOS_CENTER, Icons.Outlined.Emergency, Icons.Filled.Emergency),
    NavItem(ActiveTab.PAIRING, Icons.Outlined.QrCodeScanner, Icons.Filled.QrCodeScanner)
  )

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier.width(if (isExpanded) 220.dp else 68.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(horizontal = 8.dp, vertical = 12.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        // Sidebar header with Collapse button
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center
        ) {
          if (isExpanded) {
            Text(
              text = "WORKSPACE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(32.dp).testTag("sidebar_toggle_button")
          ) {
            Icon(
              imageVector = if (isExpanded) Icons.Filled.MenuOpen else Icons.Filled.Menu,
              contentDescription = "Toggle Sidebar",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Tab Buttons
        navItems.forEach { item ->
          val isSelected = activeTab == item.tab
          val background = if (isSelected) AirDroidGreen.copy(alpha = 0.15f) else Color.Transparent
          val contentColor = if (isSelected) AirDroidGreen else MaterialTheme.colorScheme.onSurfaceVariant

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = background,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp)
              .clickable { onTabSelected(item.tab) }
              .testTag("nav_tab_${item.tab.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
            ) {
              Box {
                Icon(
                  imageVector = if (isSelected) item.selectedIcon else item.icon,
                  contentDescription = item.tab.title,
                  tint = contentColor,
                  modifier = Modifier.size(20.dp)
                )
                if (item.badgeCount > 0) {
                  Box(
                    modifier = Modifier
                      .offset(x = 10.dp, y = (-6).dp)
                      .size(16.dp)
                      .clip(CircleShape)
                      .background(AirDroidCyan),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "${item.badgeCount}",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }
              }

              if (isExpanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = item.tab.title,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }

        Divider(
          modifier = Modifier.padding(vertical = 12.dp),
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )

        // Bound Child Devices list section
        if (isExpanded) {
          Text(
            text = "CHILD DEVICES (${devices.size})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        devices.forEach { dev ->
          val isSelected = dev.id == selectedDeviceId
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp)
              .clickable { onSelectDevice(dev.id) }
              .testTag("sidebar_device_${dev.id}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(if (dev.isOnline) StatusOnline else StatusOffline)
              )

              if (isExpanded) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = dev.name,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = if (dev.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.Battery5Bar,
                      contentDescription = null,
                      tint = if (dev.isCharging) StatusBatteryCharging else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(12.dp)
                    )
                    Text(
                      text = "${dev.batteryPercent}%",
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = "• ${dev.wifiSignalDbm}dBm",
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Bottom Status: Quick Add Device Button
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = AirDroidGreen.copy(alpha = 0.12f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onTabSelected(ActiveTab.PAIRING) }
          .testTag("sidebar_quick_pair_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Filled.AddLink,
            contentDescription = "Pair New Device",
            tint = AirDroidGreen,
            modifier = Modifier.size(18.dp)
          )
          if (isExpanded) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Add Child Device",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = AirDroidGreen
            )
          }
        }
      }
    }
  }
}
