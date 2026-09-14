package com.example.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** Finds the Parent by the unique Game Centre pairing code; no IP address is entered by the user. */
class ParentDiscoveryClient(private val context: Context) {
    companion object { const val SERVICE_TYPE = "_gamecentre._tcp." }
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    suspend fun resolveParent(code: String, timeoutMs: Long = 12_000L): Pair<String, Int>? =
        suspendCancellableCoroutine { cont ->
            val wanted = code.trim().uppercase()
            val done = AtomicBoolean(false)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            var listener: NsdManager.DiscoveryListener? = null
            lateinit var timeoutRunnable: Runnable
            fun finish(value: Pair<String, Int>?) {
                if (done.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeoutRunnable)
                    listener?.let { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
                    cont.resume(value)
                }
            }
            timeoutRunnable = Runnable { finish(null) }
            handler.postDelayed(timeoutRunnable, timeoutMs)

            listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) { Log.i("GC-Nsd", "Searching Parent for $wanted") }
                override fun onServiceFound(service: NsdServiceInfo) {
                    if (!service.serviceName.equals("GameCentre-$wanted", true)) return
                    try {
                        nsd.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) { }
                            override fun onServiceResolved(si: NsdServiceInfo) {
                                val host = si.host?.hostAddress
                                if (!host.isNullOrBlank() && si.port > 0) finish(host to si.port)
                            }
                        })
                    } catch (_: Exception) { }
                }
                override fun onServiceLost(service: NsdServiceInfo) { }
                override fun onDiscoveryStopped(serviceType: String) { }
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { finish(null) }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { }
            }
            try { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener!!) }
            catch (e: Exception) { Log.e("GC-Nsd", "discoverServices", e); finish(null) }
            cont.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                listener?.let { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
            }
        }
}
