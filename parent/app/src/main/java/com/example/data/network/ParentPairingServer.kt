package com.example.data.network

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.UUID

/** Internet pairing/session client. Pairing and device discovery happen through the Game Centre cloud relay. */
class ParentPairingServer(
    private val context: Context,
    private val onChildPaired: (ChildRegistration) -> Unit,
    private val onCodeChanged: (String) -> Unit = {}
) {
    data class ChildRegistration(
        val id: String, val name: String, val model: String, val os: String,
        val ip: String, val token: String, val parentToken: String
    )

    private val prefs = context.getSharedPreferences("parent_pairing", Context.MODE_PRIVATE)
    private val parentId = prefs.getString("parentId", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("parentId", it).apply()
    }
    private val parentToken = prefs.getString("parentToken", null) ?: UUID.randomUUID().toString().replace("-", "").also {
        prefs.edit().putString("parentToken", it).apply()
    }
    @Volatile private var running = false
    private var pollThread: Thread? = null

    fun pairingCode(): String = prefs.getString("code", "--------") ?: "--------"

    fun regenerateCode(): String {
        requestSessionCode()
        return pairingCode()
    }

    fun localIp(): String = "Cloud relay"

    fun start() {
        if (running) return
        running = true
        pollThread = Thread {
            requestSessionCode()
            while (running) {
                pollDevices()
                try { Thread.sleep(2000) } catch (_: InterruptedException) { break }
            }
        }.also { it.start() }
    }

    fun stop() { running = false; pollThread?.interrupt(); pollThread = null }

    private fun requestSessionCode() {
        try {
            val body = JSONObject().put("parentId", parentId).put("parentToken", parentToken).toString()
            val response = post("/api/parent/session", body)
            if (response.code in 200..299) {
                val json = JSONObject(response.body)
                json.optString("code").takeIf { it.length == 8 }?.let {
                    prefs.edit().putString("code", it).apply()
                    onCodeChanged(it)
                }
            }
        } catch (_: Exception) { }
    }

    private fun pollDevices() {
        try {
            val response = get("/api/parent/devices")
            if (response.code !in 200..299) return
            val arr = JSONObject(response.body).optJSONArray("devices") ?: JSONArray()
            val seen = prefs.getStringSet("seenChildren", emptySet())?.toMutableSet() ?: mutableSetOf()
            for (i in 0 until arr.length()) {
                val d = arr.getJSONObject(i)
                val id = d.optString("id")
                if (id.isBlank() || seen.contains(id)) continue
                seen.add(id)
                onChildPaired(ChildRegistration(
                    id = id,
                    name = d.optString("name", "Child Device"),
                    model = d.optString("model", "Android"),
                    os = d.optString("os", "Android"),
                    ip = "",
                    token = parentToken,
                    parentToken = parentToken
                ))
            }
            prefs.edit().putStringSet("seenChildren", seen).apply()
        } catch (_: Exception) { }
    }

    private data class HttpResult(val code: Int, val body: String)

    private fun post(path: String, body: String): HttpResult {
        val conn = (URL(CloudConfig.BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 8000; readTimeout = 8000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
        return readResponse(conn)
    }

    private fun get(path: String): HttpResult {
        val conn = (URL(CloudConfig.BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("Authorization", "Bearer $parentToken")
        }
        return readResponse(conn)
    }

    private fun readResponse(conn: HttpURLConnection): HttpResult {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
        conn.disconnect()
        return HttpResult(code, body)
    }
}
