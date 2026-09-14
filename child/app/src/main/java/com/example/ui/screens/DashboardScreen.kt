package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AirDroidChildApp
import com.example.data.model.DaemonStatus
import com.example.data.model.PairedParentDevice
import com.example.service.AirDroidAccessibilityService
import com.example.ui.theme.AirDroidCyan
import com.example.ui.theme.AirDroidGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    daemonStatus: DaemonStatus,
    activeDevice: PairedParentDevice?,
    pairingCode: String,
    connectedClients: Int,
    touchesExecuted: Int,
    notificationsSynced: Int,
    recentLogs: List<com.example.data.model.DaemonLog>,
    onToggleDaemon: () -> Unit,
    onPauseResume: () -> Unit,
    onOpenWizard: () -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val screenCastEngine = AirDroidChildApp.screenCastEngine
    val isCasting by (screenCastEngine?.isStreaming?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) })
    val localIp = AirDroidChildApp.daemonServer?.getLocalIpAddress() ?: "127.0.0.1"

    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            screenCastEngine?.startCasting(result.resultCode, result.data!!, context.resources.displayMetrics)
        }
    }

    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        TopAppBar(
            title = { Text("Game Centre", color = TextPrimary, fontWeight = FontWeight.Bold) },
            actions = { IconButton(onClick = onOpenWizard) { Icon(Icons.Default.Security, null, tint = AirDroidCyan) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Child device", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Status: ${daemonStatus.name}", color = if (daemonStatus == DaemonStatus.CONNECTED) AirDroidGreen else TextSecondary)
                    Text("Local address: $localIp:8888", color = TextSecondary, fontSize = 12.sp)
                    if (pairingCode.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Parent pairing code: $pairingCode", color = AirDroidCyan, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(pairingCode)); Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = AirDroidCyan)
                            }
                        }
                    }
                    Text("Connected parents: $connectedClients", color = TextSecondary, fontSize = 12.sp)
                    Text("Remote actions executed: $touchesExecuted", color = TextSecondary, fontSize = 12.sp)
                    Text("Notifications synchronized: $notificationsSynced", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Screen sharing", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        if (isCasting) "Screen sharing is active. Android's MediaProjection permission was granted on this device." else "Start sharing only after Android displays the screen-capture permission dialog.",
                        color = TextSecondary, fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            if (isCasting) screenCastEngine?.stopCasting() else {
                                val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                                captureLauncher.launch(manager.createScreenCaptureIntent())
                            }
                        },
                        Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isCasting) DarkSurfaceVariant else AirDroidCyan, contentColor = if (isCasting) TextPrimary else DarkBackground)
                    ) {
                        Icon(if (isCasting) Icons.Default.Stop else Icons.Default.Cast, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(if (isCasting) "Stop Screen Sharing" else "Start Screen Sharing", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPauseResume, Modifier.weight(1f)) { Text(if (daemonStatus == DaemonStatus.PAUSED) "Resume" else "Pause") }
                OutlinedButton(onClick = onToggleDaemon, Modifier.weight(1f)) { Text(if (daemonStatus == DaemonStatus.STOPPED) "Start" else "Stop") }
            }
            OutlinedButton(onClick = onClearLogs, Modifier.fillMaxWidth()) { Text("Clear local audit log") }
        }
    }
}
