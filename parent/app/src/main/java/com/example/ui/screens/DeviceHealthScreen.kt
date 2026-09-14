package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.ChildDevice
import com.example.data.model.DevicePermissionHealth
import com.example.ui.theme.*

@Composable
fun DeviceHealthScreen(
  device: ChildDevice?,
  permissionHealth: DevicePermissionHealth,
  onSendFixRequest: () -> Unit,
  onRenameDevice: (String) -> Unit,
  onRemoveDevice: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showRenameDialog by remember { mutableStateOf(false) }
  var showRemoveConfirmDialog by remember { mutableStateOf(false) }

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
            imageVector = Icons.Filled.HealthAndSafety,
            contentDescription = null,
            tint = AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "Child Device Health & System Permissions",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${device?.name ?: "No child selected"} • ${device?.ipAddress ?: "—"} • ${device?.osVersion ?: "—"}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = { showRenameDialog = true },
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rename", fontSize = 11.sp)
          }
          Button(
            onClick = { showRemoveConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = StatusOffline.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
          ) {
            Text("Unpair Device", fontSize = 11.sp, color = StatusOffline)
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // 1. Hardware & Battery Telemetry Card
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("BATTERY & THERMAL TELEMETRY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${device?.batteryPercent ?: 84}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AirDroidGreen)
                Text("Battery Level", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("31.4°C", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AirDroidCyan)
                Text("Temperature (Normal)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("4.12 V", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Cell Voltage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Good (97%)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AirDroidGreen)
                Text("Battery Health", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      // 2. Storage Distribution Card
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("STORAGE UTILIZATION (256 GB TOTAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
            ) {
              Box(modifier = Modifier.weight(0.18f).fillMaxHeight().background(Color(0xFFE11D48))) // Apps
              Box(modifier = Modifier.weight(0.10f).fillMaxHeight().background(AirDroidCyan))       // System
              Box(modifier = Modifier.weight(0.08f).fillMaxHeight().background(Color(0xFF8B5CF6))) // Media
              Box(modifier = Modifier.weight(0.64f).fillMaxHeight().background(Color(0xFF334155))) // Free
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Apps: 42 GB (18%)", fontSize = 10.sp)
              Text("System: 24 GB (10%)", fontSize = 10.sp)
              Text("Media: 18 GB (8%)", fontSize = 10.sp)
              Text("Available Free: 172 GB (64%)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AirDroidGreen)
            }
          }
        }
      }

      // 3. Child Background Permissions Audit Card
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("CHILD DAEMON PERMISSION AUDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Button(
                onClick = onSendFixRequest,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
              ) {
                Text("Re-Audit & Repair", fontSize = 11.sp)
              }
            }

            val permissionItems = listOf(
              Pair("Accessibility Service Daemon", permissionHealth.accessibilityService),
              Pair("ScreenCast / MediaProjection Hook", permissionHealth.screenCastService),
              Pair("Notification Listener Service", permissionHealth.notificationListener),
              Pair("Camera & Audio Record Grants", permissionHealth.cameraAndMic),
              Pair("Background Location (ACCESS_BACKGROUND_LOCATION)", permissionHealth.locationAlways),
              Pair("Device Administrator Policy", permissionHealth.deviceAdmin),
              Pair("Battery Optimization Exemption (Doze Whitelist)", permissionHealth.batteryOptimizationDisabled)
            )

            permissionItems.forEach { (name, isActive) ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(
                    imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (isActive) AirDroidGreen else StatusOffline,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (isActive) AirDroidGreen.copy(alpha = 0.2f) else StatusOffline.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = if (isActive) "ACTIVE" else "MISSING",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) AirDroidGreen else StatusOffline,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Rename Dialog
  if (showRenameDialog) {
    var newName by remember { mutableStateOf(device?.name ?: "") }
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = { Text("Rename Child Device") },
      text = {
        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Device Name") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newName.isNotBlank()) {
              onRenameDevice(newName.trim())
              showRenameDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Unpair Dialog
  if (showRemoveConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showRemoveConfirmDialog = false },
      title = { Text("Unpair & Remove Device?") },
      text = {
        Text("Are you sure you want to remove ${device?.name}? All local monitoring data, geofences, and logs will be detached.")
      },
      confirmButton = {
        Button(
          onClick = {
            onRemoveDevice()
            showRemoveConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusOffline)
        ) {
          Text("Unpair Device")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRemoveConfirmDialog = false }) { Text("Cancel") }
      }
    )
  }
}
