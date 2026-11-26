package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Link between this phone app and the GEBS school software on the school PC.
 *  - Everything the user does is saved on the phone first (works offline).
 *  - Changes wait in an "outbox" and are sent whenever the school PC can be reached.
 *  - Each sync also brings back the newest data from the school software.
 */
object PcSync {
    private const val PREFS = "gebs_pc_sync"
    private const val DEFAULT_PORT = 47832
    private lateinit var prefs: SharedPreferences

    enum class Status { OK, BAD_LOGIN, UNREACHABLE, NOT_CONFIGURED, SERVER_BUSY }

    data class Result(val status: Status, val bundle: JSONObject? = null)

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    /** School PC address, e.g. 192.168.1.20 (port 47832 is added automatically). */
    var peer: String
        get() = prefs.getString("peer", "") ?: ""
        set(value) { prefs.edit().putString("peer", value.trim()).apply() }

    /** Whose data is currently stored in the phone tables ("name|reg"). */
    var dataOwner: String
        get() = prefs.getString("dataOwner", "") ?: ""
        set(value) { prefs.edit().putString("dataOwner", value).apply() }

    var basicPay: Double
        get() = prefs.getFloat("basicPay", 0f).toDouble()
        set(value) { prefs.edit().putFloat("basicPay", value.toFloat()).apply() }

    var allowances: Double
        get() = prefs.getFloat("allowances", 0f).toDouble()
        set(value) { prefs.edit().putFloat("allowances", value.toFloat()).apply() }

    /** Current signed-in user key (set at login). */
    @Volatile var owner: String = ""

    fun ownerKey(name: String, reg: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase() + "|" +
            reg.replace("-", "").replace(" ", "").uppercase()

    fun newClientId(): String = UUID.randomUUID().toString().replace("-", "")

    // ---------------- outbox (changes waiting to be sent) ----------------
    @Synchronized
    private fun readOutbox(): JSONArray =
        try { JSONArray(prefs.getString("outbox", "[]")) } catch (_: Exception) { JSONArray() }

    @Synchronized
    fun enqueue(item: JSONObject) {
        if (item.optString("clientId").isBlank()) item.put("clientId", newClientId())
        item.put("owner", owner)
        val arr = readOutbox()
        arr.put(item)
        prefs.edit().putString("outbox", arr.toString()).apply()
    }

    @Synchronized
    fun pendingCount(forOwner: String = owner): Int {
        val arr = readOutbox()
        var n = 0
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("owner") == forOwner) n++
        return n
    }

    @Synchronized
    private fun itemsFor(ownerKey: String): List<JSONObject> {
        val arr = readOutbox()
        return (0 until arr.length()).map { arr.getJSONObject(it) }.filter { it.optString("owner") == ownerKey }
    }

    @Synchronized
    private fun removeSent(ids: Set<String>) {
        val arr = readOutbox()
        val keep = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("clientId") !in ids) keep.put(o)
        }
        prefs.edit().putString("outbox", keep.toString()).apply()
    }

    // ---------------- saved logins (so a user can open the app offline again) ----------------
    @Synchronized
    fun saveSession(profile: JSONObject, reg: String) {
        val arr = try { JSONArray(prefs.getString("sessions", "[]")) } catch (_: Exception) { JSONArray() }
        val key = ownerKey(profile.optString("name"), reg)
        val keep = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("key") != key) keep.put(o)
        }
        keep.put(JSONObject().put("key", key).put("reg", reg).put("profile", profile))
        prefs.edit().putString("sessions", keep.toString()).apply()
    }

    @Synchronized
    fun findSavedSession(name: String, reg: String): JSONObject? {
        val arr = try { JSONArray(prefs.getString("sessions", "[]")) } catch (_: Exception) { JSONArray() }
        val key = ownerKey(name, reg)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("key") == key) return o.optJSONObject("profile")
        }
        return null
    }

    // ---------------- network ----------------
    private fun baseUrl(): String? {
        var p = peer.trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
        if (p.isBlank()) return null
        if (!p.contains(":")) p += ":$DEFAULT_PORT"
        return "http://$p"
    }

    /** Send waiting changes + sign-in, and get back the newest data for this user. */
    suspend fun sync(name: String, reg: String): Result = withContext(Dispatchers.IO) {
        val base = baseUrl() ?: return@withContext Result(Status.NOT_CONFIGURED)
        val key = ownerKey(name, reg)
        val items = itemsFor(key)
        try {
            val body = JSONObject()
                .put("name", name.trim())
                .put("reg", reg.trim())
                .put("items", JSONArray(items.map { it }))
            val conn = (URL("$base/app/sync").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            conn.disconnect()
            when {
                code == 200 -> {
                    val json = JSONObject(text)
                    removeSent(items.map { it.optString("clientId") }.toSet())
                    Result(Status.OK, json.optJSONObject("bundle"))
                }
                code == 401 -> Result(Status.BAD_LOGIN)
                else -> Result(Status.SERVER_BUSY)
            }
        } catch (_: Exception) {
            Result(Status.UNREACHABLE)
        }
    }
}
