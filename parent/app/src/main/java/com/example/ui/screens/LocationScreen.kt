package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChildDevice
import com.example.data.model.ChildLocation
import com.example.data.model.Geofence
import com.example.ui.theme.*

@Composable
fun LocationScreen(
  device: ChildDevice?,
  location: ChildLocation?,
  geofences: List<Geofence>,
  onAddGeofence: (String, String, Int) -> Unit,
  onDeleteGeofence: (Geofence) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedSubTab by remember { mutableStateOf("MAP") } // "MAP", "GEOFENCES", "HISTORY"
  var showAddGeofenceDialog by remember { mutableStateOf(false) }
  var zoomLevel by remember { mutableStateOf(1f) }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseRadius by infiniteTransition.animateFloat(
    initialValue = 18f,
    targetValue = 48f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseRadius"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseAlpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Header & Subtabs
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
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "Live GPS Tracking • ${device?.name ?: "Child Device"}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = location?.address ?: "Resolving satellite telemetry...",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }
        }

        // Subtabs
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilterChip(
            selected = selectedSubTab == "MAP",
            onClick = { selectedSubTab = "MAP" },
            label = { Text("Live Map", fontSize = 11.sp) },
            modifier = Modifier.testTag("location_tab_map")
          )
          FilterChip(
            selected = selectedSubTab == "GEOFENCES",
            onClick = { selectedSubTab = "GEOFENCES" },
            label = { Text("Geofences (${geofences.size})", fontSize = 11.sp) },
            modifier = Modifier.testTag("location_tab_geofences")
          )
          FilterChip(
            selected = selectedSubTab == "HISTORY",
            onClick = { selectedSubTab = "HISTORY" },
            label = { Text("Route Trail", fontSize = 11.sp) },
            modifier = Modifier.testTag("location_tab_history")
          )
        }
      }
    }

    // Telemetry strip
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Text("📡 Accuracy: ±${location?.accuracyMeters ?: 4.2}m", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("🕒 ${location?.timestamp ?: "2 mins ago"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("🔋 Battery: ${location?.batteryAtLocation ?: 84}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (location?.isMoving == true) AirDroidCyan.copy(alpha = 0.2f) else AirDroidGreen.copy(alpha = 0.2f)
        ) {
          Text(
            text = if (location?.isMoving == true) "🚗 Moving (In Transit)" else "📍 Stationary",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (location?.isMoving == true) AirDroidCyan else AirDroidGreen,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }

    when (selectedSubTab) {
      "MAP" -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
        ) {
          // Canvas rendering vector map layout
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background tiles (Streets & Parks)
            drawRect(Color(0xFF1E293B), Offset.Zero, size)

            // Draw a park
            drawRect(
              Color(0xFF14532D).copy(alpha = 0.4f),
              topLeft = Offset(w * 0.1f, h * 0.15f),
              size = Size(w * 0.35f, h * 0.3f)
            )

            // Draw a river / lake
            val waterPath = Path().apply {
              moveTo(0f, h * 0.7f)
              cubicTo(w * 0.3f, h * 0.65f, w * 0.7f, h * 0.85f, w, h * 0.75f)
              lineTo(w, h)
              lineTo(0f, h)
              close()
            }
            drawPath(waterPath, Color(0xFF0369A1).copy(alpha = 0.4f))

            // Main roads
            val roadColor = Color(0xFF334155)
            // Vertical avenues
            for (i in 1..4) {
              drawLine(roadColor, Offset(w * (i * 0.2f), 0f), Offset(w * (i * 0.2f), h), strokeWidth = 10f * zoomLevel)
            }
            // Horizontal streets
            for (j in 1..4) {
              drawLine(roadColor, Offset(0f, h * (j * 0.2f)), Offset(w, h * (j * 0.2f)), strokeWidth = 10f * zoomLevel)
            }

            // Highway
            drawLine(
              Color(0xFF475569),
              Offset(0f, h * 0.35f),
              Offset(w, h * 0.45f),
              strokeWidth = 18f * zoomLevel
            )

            // Draw Geofences
            geofences.forEachIndexed { idx, geo ->
              val cx = w * (0.3f + (idx * 0.25f))
              val cy = h * (0.3f + (idx * 0.18f))
              val radius = (geo.radiusMeters * 0.6f * zoomLevel).coerceIn(40f, 160f)

              drawCircle(
                color = AirDroidGreen.copy(alpha = 0.15f),
                radius = radius,
                center = Offset(cx, cy)
              )
              drawCircle(
                color = AirDroidGreen.copy(alpha = 0.8f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
              )
            }

            // Child center point
            val childCenter = Offset(w * 0.52f, h * 0.48f)

            // Animated Pulse ring
            drawCircle(
              color = AirDroidGreen.copy(alpha = pulseAlpha),
              radius = pulseRadius * zoomLevel,
              center = childCenter
            )

            // Pin Core
            drawCircle(
              color = AirDroidGreen,
              radius = 12f * zoomLevel,
              center = childCenter
            )
            drawCircle(
              color = Color.White,
              radius = 5f * zoomLevel,
              center = childCenter
            )
          }

          // Overlay: Map Controls (+ / -, Center)
          Column(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FloatingActionButton(
              onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.5f) },
              modifier = Modifier.size(38.dp),
              containerColor = MaterialTheme.colorScheme.surface
            ) {
              Icon(Icons.Filled.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }
            FloatingActionButton(
              onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.75f) },
              modifier = Modifier.size(38.dp),
              containerColor = MaterialTheme.colorScheme.surface
            ) {
              Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
            FloatingActionButton(
              onClick = { zoomLevel = 1.0f },
              modifier = Modifier.size(38.dp),
              containerColor = AirDroidGreen
            ) {
              Icon(Icons.Filled.MyLocation, contentDescription = "Center on Child", tint = Color.White, modifier = Modifier.size(20.dp))
            }
          }

          // Overlay: Location Pin Banner
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(12.dp)
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
                  .background(AirDroidGreen)
              )
              Text(
                text = if (location != null) "${device?.name ?: "Child"}: ${location.latitude}, ${location.longitude}" else "No live location received",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      "GEOFENCES" -> {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "CONFIGURED SAFE ZONES & PERIMETERS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
              onClick = { showAddGeofenceDialog = true },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier = Modifier.testTag("add_geofence_button")
            ) {
              Icon(Icons.Filled.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Safe Zone", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(geofences) { geo ->
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
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Surface(
                      shape = CircleShape,
                      color = if (geo.isCurrentlyInside) AirDroidGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                      modifier = Modifier.size(40.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = if (geo.isCurrentlyInside) Icons.Filled.VerifiedUser else Icons.Filled.ShareLocation,
                          contentDescription = null,
                          tint = if (geo.isCurrentlyInside) AirDroidGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }

                    Column {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Text(
                          text = geo.name,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        if (geo.isCurrentlyInside) {
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AirDroidGreen.copy(alpha = 0.2f)
                          ) {
                            Text(
                              "CURRENTLY HERE",
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = AirDroidGreen,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                          }
                        }
                      }
                      Text(
                        text = geo.address,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Text(
                        text = "Radius: ${geo.radiusMeters}m • Alerts: ${if (geo.isTriggerOnEnter) "Enter" else ""} ${if (geo.isTriggerOnExit) "Exit" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  IconButton(
                    onClick = { onDeleteGeofence(geo) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = "Delete Geofence",
                      tint = StatusOffline,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      "HISTORY" -> {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Text(
            text = "TODAY'S TRAVEL ROUTE & STOPS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(8.dp))
          val trailStops = emptyList<Triple<String, String, String>>()

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(trailStops) { (time, place, detail) ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Box(
                    modifier = Modifier
                      .size(12.dp)
                      .clip(CircleShape)
                      .background(AirDroidCyan)
                  )
                  Box(
                    modifier = Modifier
                      .width(2.dp)
                      .height(44.dp)
                      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                  )
                }

                Column {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(text = time, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AirDroidCyan)
                    Text(text = place, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                  }
                  Text(text = detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }
    }
  }

  // Add Geofence Dialog
  if (showAddGeofenceDialog) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(150) }

    AlertDialog(
      onDismissRequest = { showAddGeofenceDialog = false },
      title = { Text("Add Safe Zone / Geofence", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Zone Name (e.g. Soccer Club)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Street Address") },
            modifier = Modifier.fillMaxWidth()
          )
          Text("Radius: ${radius}m", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Slider(
            value = radius.toFloat(),
            onValueChange = { radius = it.toInt() },
            valueRange = 50f..500f,
            steps = 8
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isNotBlank()) {
              onAddGeofence(name, address.ifBlank { "Custom Location" }, radius)
              showAddGeofenceDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen)
        ) {
          Text("Save Zone")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddGeofenceDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
