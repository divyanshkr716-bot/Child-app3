package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PermissionStatus
import com.example.ui.theme.AirDroidBlue
import com.example.ui.theme.AirDroidCyan
import com.example.ui.theme.AirDroidGreen
import com.example.ui.theme.AirDroidOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

data class PermissionStep(
    val id: String,
    val title: String,
    val description: String,
    val whyNeeded: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val actionLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionWizardScreen(
    status: PermissionStatus,
    onRefresh: () -> Unit,
    onRememberAndFinish: () -> Unit = {},
    onProceed: () -> Unit
) {
    val context = LocalContext.current

    // Batch permissions launcher for 1-Tap Grant
    val batchPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefresh()
        onRememberAndFinish()
        Toast.makeText(context, "Permission result recorded", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    val steps = listOf(
        PermissionStep(
            id = "accessibility",
            title = "1. Accessibility Service",
            description = "Settings -> Accessibility -> Game Centre -> Enable",
            whyNeeded = "Used for parent-requested navigation and app-restriction actions after the child enables the Android Accessibility Service.",
            icon = Icons.Default.TouchApp,
            isGranted = status.accessibility,
            actionLabel = if (status.accessibility) "Active" else "Enable Service"
        ),
        PermissionStep(
            id = "usage",
            title = "2. Usage Access",
            description = "Settings -> Special app access -> Usage access -> Game Centre",
            whyNeeded = "Provides real app usage time so daily limits are based on Android UsageStats rather than simulated values.",
            icon = Icons.Default.Layers,
            isGranted = status.usageStats,
            actionLabel = if (status.usageStats) "Allowed" else "Grant Usage Access"
        ),
        PermissionStep(
            id = "notification",
            title = "3. Notification Listener Service",
            description = "Notification Access in System Settings",
            whyNeeded = "Intercepts and mirrors incoming device messages (WhatsApp, SMS, calls) to the parent desktop client and executes remote quick replies.",
            icon = Icons.Default.NotificationsActive,
            isGranted = status.notificationListener,
            actionLabel = if (status.notificationListener) "Allowed" else "Grant Access"
        ),
        PermissionStep(
            id = "battery",
            title = "4. Background Keep-Alive (Battery Exemption)",
            description = "Ignore Battery Optimizations",
            whyNeeded = "Prevents aggressive Android OEM battery managers (MIUI, OneUI) from killing the child daemon so it runs 24/7 silently.",
            icon = Icons.Default.BatteryAlert,
            isGranted = status.batteryOptimization,
            actionLabel = if (status.batteryOptimization) "Exempted" else "Disable Optimization"
        ),
        PermissionStep(
            id = "storage",
            title = "5. Storage & File System Access",
            description = "Manage External Storage / Scoped Files",
            whyNeeded = "Allows parent browser to browse photos, documents, and manage files on child device remotely.",
            icon = Icons.Default.FolderShared,
            isGranted = status.storage,
            actionLabel = if (status.storage) "Granted" else "Allow Storage"
        ),
        PermissionStep(
            id = "overlay",
            title = "6. Display Over Other Apps",
            description = "System Alert Window overlay permission",
            whyNeeded = "Allows child protection alerts and restricted app block overlays to appear instantly.",
            icon = Icons.Default.Layers,
            isGranted = status.overlay,
            actionLabel = if (status.overlay) "Allowed" else "Permit Overlay"
        ),
        PermissionStep(
            id = "camera",
            title = "7. Remote Camera & Visuals",
            description = "Camera hardware access",
            whyNeeded = "Permits parent to switch between front and back camera for remote environmental viewing.",
            icon = Icons.Default.CameraAlt,
            isGranted = status.camera,
            actionLabel = if (status.camera) "Granted" else "Enable Camera"
        ),
        PermissionStep(
            id = "audio",
            title = "8. Microphone",
            description = "Audio recording for ambient listening",
            whyNeeded = "Allows parents to listen to one-way real-time ambient surrounding sounds of child.",
            icon = Icons.Default.Mic,
            isGranted = status.audio,
            actionLabel = if (status.audio) "Granted" else "Enable Mic"
        ),
        PermissionStep(
            id = "location",
            title = "9. Live GPS Tracking & Geofence",
            description = "Fine & Coarse GPS Location",
            whyNeeded = "Reports child's real-time physical coordinates and movement speed to parent console.",
            icon = Icons.Default.MyLocation,
            isGranted = status.location,
            actionLabel = if (status.location) "Granted" else "Enable GPS"
        )
    )

    fun launchBatchRequest() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        permissionsToRequest.add(Manifest.permission.READ_SMS)
        permissionsToRequest.add(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        batchPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        onRememberAndFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("permission_wizard_screen")
    ) {
        TopAppBar(
            title = {
                Text(
                    "AirDroid Permission Manager",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Permissions",
                        tint = AirDroidCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface
            )
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // ⚡ Permission setup
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(AirDroidCyan, AirDroidGreen)),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(AirDroidGreen.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = AirDroidGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Request required permissions",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    "Android will ask for each permission normally",
                                    fontSize = 12.sp,
                                    color = AirDroidCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "Request the permissions required by the features you choose. Android controls when permissions are granted or revoked.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { launchBatchRequest() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("batch_grant_permissions_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AirDroidCyan,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "REQUEST PERMISSIONS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            // Individual special settings permission tiles
            items(steps) { step ->
                PermissionTile(
                    step = step,
                    onAction = {
                        handlePermissionAction(context, step.id)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Bottom Action Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    if (status.allEssentialGranted) onRememberAndFinish()
                    onProceed()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("finish_setup_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (status.allEssentialGranted) AirDroidGreen else AirDroidBlue,
                    contentColor = if (status.allEssentialGranted) DarkBackground else Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (status.allEssentialGranted) Icons.Default.Check else Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (status.allEssentialGranted) "CONTINUE TO GAME CENTRE" else "CONFIRM PERMISSIONS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PermissionTile(
    step: PermissionStep,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isGranted) DarkSurface else DarkSurfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (step.isGranted) listOf(SuccessGreen.copy(alpha = 0.4f), SuccessGreen.copy(alpha = 0.4f))
                else listOf(DarkCardBorder, DarkCardBorder)
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (step.isGranted) SuccessGreen.copy(alpha = 0.2f) else AirDroidCyan.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = if (step.isGranted) SuccessGreen else AirDroidCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = step.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = step.description,
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                if (step.isGranted) {
                    Box(
                        modifier = Modifier
                            .background(SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onAction,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AirDroidCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(step.actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = step.whyNeeded,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

private fun handlePermissionAction(context: Context, id: String) {
    when (id) {
        "accessibility" -> {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Please open Settings -> Accessibility", Toast.LENGTH_SHORT).show()
            }
        }
        "usage" -> {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Please enable Usage Access in Settings", Toast.LENGTH_SHORT).show()
            }
        }
        "notification" -> {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Please enable Notification Access in Settings", Toast.LENGTH_SHORT).show()
            }
        }
        "battery" -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
        "storage" -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
        "overlay" -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Please allow Display Over Other Apps", Toast.LENGTH_SHORT).show()
            }
        }
        "camera" -> {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        "audio" -> {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
        "location" -> {
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
