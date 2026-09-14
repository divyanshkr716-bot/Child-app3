package com.example.data.network

import android.content.Context
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** Keeps the Child connected outbound to the relay so it works behind mobile-carrier NAT. */
class CloudRelayClient(private val context: Context) {
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var started = false

    fun startIfPaired() {
        val prefs = context.getSharedPreferences("child_pairing", Context.MODE_PRIVATE)
        val token = prefs.getString("authToken", null) ?: return
        val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: return
        if (started) return
        started = true
        connect(deviceId, token)
    }

    fun stop() { started = false; socket?.close(1000, "stopped"); socket = null }

    private fun connect(deviceId: String, token: String) {
        val request = Request.Builder()
            .url(CloudConfig.WS_URL + "?role=child&id=" + java.net.URLEncoder.encode(deviceId, "UTF-8"))
            .header("Authorization", "Bearer $token")
            .build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socket = webSocket
            }
            override fun onMessage(webSocket: WebSocket, text: String) { handleRequest(text, token) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                if (started) android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ connect(deviceId, token) }, 3000)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                if (started) android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ connect(deviceId, token) }, 3000)
            }
        })
    }

    private fun handleRequest(text: String, token: String) {
        try {
            val req = JSONObject(text)
            if (req.optString("type") != "http_request") return
            val id = req.getString("requestId")
            val method = req.optString("method", "GET")
            val path = req.optString("path", "/api/heartbeat")
            val body = req.optString("bodyBase64", "")
            val result = proxyToLocal(method, path, body, token)
            val out = JSONObject().put("type", "http_response").put("requestId", id)
                .put("status", result.first).put("contentType", result.second.first)
                .put("bodyBase64", result.second.second)
            socket?.send(out.toString())
        } catch (_: Exception) { }
    }

    private fun proxyToLocal(method: String, path: String, bodyBase64: String, token: String): Pair<Int, Pair<String, String>> {
        val conn = (URL("http://127.0.0.1:8888" + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5000; readTimeout = 30000; doInput = true
            setRequestProperty("X-AirDroid-Auth", "Bearer $token")
            if (bodyBase64.isNotEmpty()) doOutput = true
        }
        if (bodyBase64.isNotEmpty()) {
            Base64.decode(bodyBase64, Base64.DEFAULT).also { conn.outputStream.use { out -> out.write(it) } }
        }
        val status = conn.responseCode
        val type = conn.contentType ?: "application/octet-stream"
        val stream = try { conn.inputStream } catch (_: Exception) { conn.errorStream }
        val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
        if (bytes.size > 32 * 1024 * 1024) { conn.disconnect(); return 413 to ("application/json" to Base64.encodeToString("{\"error\":\"response too large\"}".toByteArray(), Base64.NO_WRAP)) }
        conn.disconnect()
        return status to (type to Base64.encodeToString(bytes, Base64.NO_WRAP))
    }
}
