package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AirDroidCyan
import com.example.ui.theme.AirDroidGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    pairingCode: String,
    pairingServerIp: String,
    onRefreshCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    Column(modifier.fillMaxSize().background(DarkBackground).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TopAppBar(
            title = { Text("Add Child Device", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = onRefreshCode) { Icon(Icons.Default.Refresh, "New code", tint = AirDroidCyan) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Real local pairing", color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Show this unique code on the Child device. Game Centre will discover this Parent automatically on the same Wi-Fi network.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("Unique pairing code", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pairingCode, color = AirDroidGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    IconButton(onClick = { clipboard.setText(AnnotatedString(pairingCode)) }) { Icon(Icons.Default.ContentCopy, "Copy", tint = AirDroidCyan) }
                }
                Text("Waiting for Child registration…", color = AirDroidGreen, fontSize = 12.sp)
            }
        }
        OutlinedButton(onClick = onRefreshCode, Modifier.fillMaxWidth()) { Text("Generate New Unique Code") }
    }
}
