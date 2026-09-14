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
import com.example.data.model.ChildDevice
import com.example.data.model.DrivingTrip
import com.example.ui.theme.*

@Composable
fun DrivingDetectionScreen(
  device: ChildDevice?,
  trips: List<DrivingTrip>,
  modifier: Modifier = Modifier
) {
  var speedAlertLimit by remember { mutableStateOf(65) }

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
            imageVector = Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = AirDroidGreen,
            modifier = Modifier.size(22.dp)
          )
          Column {
            Text(
              text = "Driving Safety & Crash Telemetry • ${device?.name ?: "Teen Driver"}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Speed Limit Alert: >${speedAlertLimit} MPH • ${trips.size} trips logged",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(shape = RoundedCornerShape(6.dp), color = AirDroidGreen.copy(alpha = 0.2f)) {
          Text(
            "CRASH SENSORS ACTIVE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AirDroidGreen,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }

    // Safety Score Card & Key Telemetry
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Score Card
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.weight(1f)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = AirDroidGreen.copy(alpha = 0.2f),
            modifier = Modifier.size(50.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("94", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AirDroidGreen)
            }
          }
          Column {
            Text("DRIVING SAFETY SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Safe Driver Rating", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("No speeding or hard braking detected today", fontSize = 10.sp, color = AirDroidGreen)
          }
        }
      }

      // Speed Threshold Card
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SPEED ALERT LIMIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${speedAlertLimit} MPH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AirDroidCyan)
          }
          Slider(
            value = speedAlertLimit.toFloat(),
            onValueChange = { speedAlertLimit = it.toInt() },
            valueRange = 45f..90f,
            steps = 8
          )
        }
      }
    }

    // Trips List
    Text(
      "LOGGED DRIVING TRIPS",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(trips) { trip ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          modifier = Modifier.fillMaxWidth().testTag("trip_item_${trip.id}")
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Icon(Icons.Filled.AltRoute, contentDescription = null, tint = AirDroidCyan, modifier = Modifier.size(18.dp))
                  }
                }
                Column {
                  Text(
                    text = "${trip.origin} ➔ ${trip.destination}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "${trip.date} • ${trip.startTime} - ${trip.endTime} (${trip.durationMinutes} mins)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Surface(shape = RoundedCornerShape(4.dp), color = AirDroidGreen.copy(alpha = 0.2f)) {
                Text(
                  "${trip.distanceMiles} Miles",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = AirDroidGreen,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            // Metrics row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              Text("Top: ${trip.topSpeedMph} MPH", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Avg: ${trip.avgSpeedMph} MPH", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Hard Brakes: ${trip.hardBrakingEvents}", fontSize = 11.sp, color = if (trip.hardBrakingEvents > 0) StatusOffline else AirDroidGreen)
              Text("Phone In Hand: ${trip.phoneUsageMinutes}m", fontSize = 11.sp, color = if (trip.phoneUsageMinutes > 0) StatusOffline else AirDroidGreen)
            }
          }
        }
      }
    }
  }
}
