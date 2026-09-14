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
import com.example.data.model.AlertSeverity
import com.example.data.model.ChildAppRequest
import com.example.data.model.ChildDevice
import com.example.data.model.DeviceAlert
import com.example.data.model.RequestStatus
import com.example.ui.theme.*

@Composable
fun RequestsAlertsScreen(
  device: ChildDevice?,
  requests: List<ChildAppRequest>,
  alerts: List<DeviceAlert>,
  onRespondToRequest: (String, Boolean) -> Unit,
  onResolveAlert: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf("REQUESTS") } // "REQUESTS" or "ALERTS"

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
            imageVector = Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = if (alerts.any { !it.isResolved }) StatusOffline else AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "Parental Approvals & Security Center",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${requests.count { it.status == RequestStatus.PENDING }} pending requests • ${alerts.count { !it.isResolved }} active alerts",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Subtabs
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilterChip(
            selected = selectedTab == "REQUESTS",
            onClick = { selectedTab = "REQUESTS" },
            label = { Text("Requests (${requests.count { it.status == RequestStatus.PENDING }})", fontSize = 11.sp) },
            modifier = Modifier.testTag("tab_child_requests")
          )
          FilterChip(
            selected = selectedTab == "ALERTS",
            onClick = { selectedTab = "ALERTS" },
            label = { Text("Alerts (${alerts.count { !it.isResolved }})", fontSize = 11.sp) },
            modifier = Modifier.testTag("tab_security_alerts")
          )
        }
      }
    }

    when (selectedTab) {
      "REQUESTS" -> {
        if (requests.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending requests from ${device?.name ?: "Child"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(requests) { req ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (req.status == RequestStatus.PENDING) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth().testTag("req_item_${req.id}")
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Surface(
                        shape = CircleShape,
                        color = AirDroidCyan.copy(alpha = 0.2f),
                        modifier = Modifier.size(34.dp)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = AirDroidCyan, modifier = Modifier.size(18.dp))
                        }
                      }
                      Column {
                        Text(
                          text = "${req.childName} requested extra time",
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                          text = "${req.appName} • +${req.requestedMinutes} Minutes • ${req.timestamp}",
                          fontSize = 11.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = when (req.status) {
                        RequestStatus.PENDING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        RequestStatus.APPROVED -> AirDroidGreen.copy(alpha = 0.2f)
                        else -> StatusOffline.copy(alpha = 0.2f)
                      }
                    ) {
                      Text(
                        text = req.status.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (req.status) {
                          RequestStatus.PENDING -> Color(0xFFF59E0B)
                          RequestStatus.APPROVED -> AirDroidGreen
                          else -> StatusOffline
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }

                  // Child Reason Note
                  if (req.reason.isNotBlank()) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.surface,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Text(
                        text = "\"${req.reason}\"",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                      )
                    }
                  }

                  // Action Buttons if pending
                  if (req.status == RequestStatus.PENDING) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedButton(
                        onClick = { onRespondToRequest(req.id, false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOffline),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("reject_req_${req.id}")
                      ) {
                        Text("Decline")
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Button(
                        onClick = { onRespondToRequest(req.id, true) },
                        colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("approve_req_${req.id}")
                      ) {
                        Text("Approve +${req.requestedMinutes}m")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      "ALERTS" -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(alerts) { alt ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (!alt.isResolved) StatusOffline.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier.fillMaxWidth().testTag("alert_item_${alt.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = when (alt.severity) {
                      AlertSeverity.CRITICAL -> StatusOffline.copy(alpha = 0.2f)
                      AlertSeverity.HIGH -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                      else -> AirDroidCyan.copy(alpha = 0.2f)
                    },
                    modifier = Modifier.size(38.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = when (alt.alertType) {
                          "BLOCKED_APP_ATTEMPT" -> Icons.Filled.Security
                          "GEOFENCE_EXIT" -> Icons.Filled.ExitToApp
                          "LOW_BATTERY" -> Icons.Filled.BatteryAlert
                          else -> Icons.Filled.Warning
                        },
                        contentDescription = null,
                        tint = when (alt.severity) {
                          AlertSeverity.CRITICAL -> StatusOffline
                          AlertSeverity.HIGH -> Color(0xFFF59E0B)
                          else -> AirDroidCyan
                        },
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }

                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(text = alt.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (alt.severity) {
                          AlertSeverity.CRITICAL -> StatusOffline
                          AlertSeverity.HIGH -> Color(0xFFF59E0B)
                          else -> AirDroidCyan
                        }
                      ) {
                        Text(
                          alt.severity.name,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color.White,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }
                    Text(text = alt.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = alt.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                if (!alt.isResolved) {
                  Button(
                    onClick = { onResolveAlert(alt.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("resolve_alert_${alt.id}")
                  ) {
                    Text("Resolve", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                  }
                } else {
                  Surface(shape = RoundedCornerShape(4.dp), color = AirDroidGreen.copy(alpha = 0.2f)) {
                    Text("RESOLVED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AirDroidGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
