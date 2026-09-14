package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChildDevice
import com.example.data.model.SosAlert
import com.example.ui.theme.*

@Composable
fun SosEmergencyScreen(
  device: ChildDevice?,
  sosAlert: SosAlert?,
  onResolveSos: () -> Unit,
  onToggleSiren: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isSosActive = sosAlert != null && !sosAlert.isResolved

  val infiniteTransition = rememberInfiniteTransition(label = "sosBlink")
  val alertAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alertAlpha"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Top SOS Banner
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = if (isSosActive) StatusOffline.copy(alpha = alertAlpha) else MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(
            imageVector = Icons.Filled.Emergency,
            contentDescription = null,
            tint = if (isSosActive) Color.White else AirDroidGreen,
            modifier = Modifier.size(28.dp)
          )
          Column {
            Text(
              text = if (isSosActive) "🚨 ACTIVE SOS EMERGENCY TRIGGERED" else "Emergency SOS Guard Active",
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (isSosActive) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (isSosActive) "Child pressed hardware panic button • Immediate response required" else "All emergency triggers, sensors, and siren dispatch are armed",
              fontSize = 11.sp,
              color = if (isSosActive) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (isSosActive) {
          Button(
            onClick = onResolveSos,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.testTag("resolve_sos_button")
          ) {
            Text("Mark Safe / Resolve", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusOffline)
          }
        }
      }
    }

    if (isSosActive && sosAlert != null) {
      // Active SOS Detail Card
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("EMERGENCY SIGNAL DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sosAlert.timestamp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusOffline)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = StatusOffline.copy(alpha = 0.2f),
              modifier = Modifier.size(48.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PersonPinCircle, contentDescription = null, tint = StatusOffline, modifier = Modifier.size(28.dp))
              }
            }

            Column {
              Text("${sosAlert.childName}'s Device", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              Text("📍 ${sosAlert.address}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Coordinates: ${sosAlert.latitude}, ${sosAlert.longitude} • Battery: ${sosAlert.batteryPercent}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

          // Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Call Child
            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+15550192834"))
                context.startActivity(intent)
              },
              colors = ButtonDefaults.buttonColors(containerColor = AirDroidGreen),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("call_child_button")
            ) {
              Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Call Child", fontSize = 12.sp)
            }

            // Call 911
            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                context.startActivity(intent)
              },
              colors = ButtonDefaults.buttonColors(containerColor = StatusOffline),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("call_911_button")
            ) {
              Icon(Icons.Filled.LocalPolice, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Emergency 911", fontSize = 12.sp)
            }

            // Siren Toggle
            Button(
              onClick = onToggleSiren,
              colors = ButtonDefaults.buttonColors(
                containerColor = if (sosAlert.isSirenPlaying) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surface
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("toggle_siren_button")
            ) {
              Icon(
                imageVector = if (sosAlert.isSirenPlaying) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                contentDescription = null,
                tint = if (sosAlert.isSirenPlaying) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (sosAlert.isSirenPlaying) "Siren ON" else "Sound Siren",
                fontSize = 12.sp,
                color = if (sosAlert.isSirenPlaying) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    // Historical SOS Events
    Text(
      "HISTORICAL SOS LOGS",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val sosHistory = emptyList<Triple<String, String, String>>()

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(sosHistory) { (time, desc, status) ->
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(AirDroidGreen)
              )
              Column {
                Text(desc, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Surface(shape = RoundedCornerShape(4.dp), color = AirDroidGreen.copy(alpha = 0.2f)) {
              Text(status, fontSize = 10.sp, color = AirDroidGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }
        }
      }
    }
  }
}
