package com.example.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/** Advertises the Parent pairing endpoint on the local network using Android NSD. */
class ParentPairingDiscovery(private val context: Context, private val codeProvider: () -> String) {
    companion object { const val SERVICE_TYPE = "_gamecentre._tcp." }
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun start(port: Int) {
        stop()
        val code = codeProvider().uppercase()
        val info = NsdServiceInfo().apply {
            serviceName = "GameCentre-$code"
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("code", code)
            setAttribute("version", "1")
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) { Log.i("GC-Nsd", "Parent advertised: ${serviceInfo.serviceName}") }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { Log.e("GC-Nsd", "Registration failed: $errorCode") }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) { }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { }
        }
        registrationListener = listener
        try { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) } catch (e: Exception) { Log.e("GC-Nsd", "registerService", e) }
    }

    fun stop() {
        registrationListener?.let { try { nsd.unregisterService(it) } catch (_: Exception) {} }
        registrationListener = null
    }
}
