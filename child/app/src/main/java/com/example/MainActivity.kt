package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.PermissionWizardScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                GameCentreContent(viewModel)
            }
        }
    }
}

@Composable
private fun GameCentreContent(viewModel: MainViewModel) {
    val destination by viewModel.currentDestination.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val activeDevice by viewModel.activeDevice.collectAsState()
    val daemonStatus by viewModel.daemonStatus.collectAsState()
    val connectedClients by viewModel.connectedClientsCount.collectAsState()
    val touchesExecuted by viewModel.touchesExecuted.collectAsState()
    val notificationsSynced by viewModel.notificationsSyncedCount.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()
    val pairingStatusMessage by viewModel.pairingStatusMessage.collectAsState()
    val localIp = AirDroidChildApp.daemonServer?.getLocalIpAddress() ?: "127.0.0.1"

    Scaffold(Modifier.fillMaxSize()) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                AppDestination.Splash -> SplashScreen(
                    onTimeout = {
                        when {
                            pairedDevices.isNotEmpty() && permissionStatus.allEssentialGranted -> viewModel.navigateTo(AppDestination.Dashboard)
                            !permissionStatus.allEssentialGranted -> viewModel.navigateTo(AppDestination.PermissionWizard)
                            else -> viewModel.navigateTo(AppDestination.Pairing)
                        }
                    },
                    isPaired = pairedDevices.isNotEmpty(),
                    allPermissionsGranted = permissionStatus.allEssentialGranted
                )
                AppDestination.PermissionWizard -> {
                    BackHandler { viewModel.navigateTo(AppDestination.Pairing) }
                    PermissionWizardScreen(
                        status = permissionStatus,
                        onRefresh = viewModel::checkPermissions,
                        onRememberAndFinish = viewModel::markSetupCompletedAndRemember,
                        onProceed = {
                            if (pairedDevices.isEmpty()) viewModel.navigateTo(AppDestination.Pairing)
                            else viewModel.navigateTo(AppDestination.Dashboard)
                        }
                    )
                }
                AppDestination.Pairing -> {
                    BackHandler { viewModel.navigateTo(AppDestination.PermissionWizard) }
                    PairingScreen(
                        localIp = localIp,
                        statusMessage = pairingStatusMessage,
                        onPairWithCode = viewModel::pairWithCode,
                        onBack = { viewModel.navigateTo(AppDestination.PermissionWizard) }
                    )
                }
                AppDestination.Dashboard -> DashboardScreen(
                    daemonStatus = daemonStatus,
                    activeDevice = activeDevice ?: pairedDevices.firstOrNull(),
                    pairingCode = pairingCode,
                    connectedClients = connectedClients,
                    touchesExecuted = touchesExecuted,
                    notificationsSynced = notificationsSynced,
                    recentLogs = recentLogs,
                    onToggleDaemon = viewModel::toggleDaemonService,
                    onPauseResume = viewModel::pauseOrResumeDaemon,
                    onOpenWizard = { viewModel.navigateTo(AppDestination.PermissionWizard) },
                    onClearLogs = viewModel::clearAuditLogs
                )
            }
        }
    }
}
