package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChildDevice
import com.example.ui.RemoteCameraUiState
import com.example.ui.theme.AirDroidCyan
import com.example.ui.theme.AirDroidGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface

@Composable
fun RemoteCameraScreen(
    device: ChildDevice?,
    cameraState: RemoteCameraUiState,
    latencyMs: Int,
    fps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.CameraAlt, null, tint = AirDroidCyan, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text("Remote Camera", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(device?.name ?: "No child device selected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Text("${latencyMs}ms • ${fps} FPS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            when {
                cameraState.currentFrame != null -> Image(cameraState.currentFrame, "Live child camera frame", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                cameraState.permissionError != null -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Camera unavailable", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(cameraState.permissionError, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 12.sp)
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AirDroidGreen)
                    Spacer(Modifier.height(10.dp))
                    Text("Waiting for the Child device camera permission and stream…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Text(
            "The Child device must have Android camera permission. The stream is stopped automatically when this screen is left.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
