package com.harsha.myapplication.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ─────────────────────────────────────────────────────────────────────────
//  DATA MODELS — mirror repository.dart
// ─────────────────────────────────────────────────────────────────────────
data class VisitLog(
    val id: String = "",
    val timestamp: Long = 0L,
    val nextVisitDate: String = "",
    val nextVisitTime: String = "",
    val topic: String = "",
    val notes: String = "",
    val logType: String = "rv",
    val isImported: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("nextVisitDate", nextVisitDate)
        put("nextVisitTime", nextVisitTime)
        put("topic", topic)
        put("notes", notes)
        put("logType", logType)
        put("isImported", isImported)
    }

    companion object {
        fun fromJson(o: JSONObject) = VisitLog(
            id = o.optString("id", ""),
            timestamp = o.optLong("timestamp", 0L),
            nextVisitDate = o.optString("nextVisitDate", ""),
            nextVisitTime = o.optString("nextVisitTime", ""),
            topic = o.optString("topic", ""),
            notes = o.optString("notes", ""),
            logType = o.optString("logType", "rv"),
            isImported = o.optBoolean("isImported", false),
        )
    }
}

/** Reads a nullable string — null for both missing keys and JSON nulls. */
private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key, "").ifEmpty { null }

data class PersonRecord(
    val id: String = "",
    val name: String = "",
    val gender: String? = null,
    val age: String? = null,
    val mobile: String? = null,
    val address: String = "",
    val language: String? = null,
    val interestLevel: String? = null,
    val nextVisitDate: String = "",
    val nextVisitTime: String? = null,
    val notes: String? = null,
    val type: String = "rv",
    val studyDay: String? = null,
    val visitLogs: List<VisitLog> = emptyList(),
) {
    val hasPhone: Boolean get() = !mobile.isNullOrEmpty()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        gender?.let { put("gender", it) }
        age?.let { put("age", it) }
        mobile?.let { put("mobile", it) }
        put("address", address)
        language?.let { put("language", it) }
        interestLevel?.let { put("interestLevel", it) }
        put("nextVisitDate", nextVisitDate)
        nextVisitTime?.let { put("nextVisitTime", it) }
        notes?.let { put("notes", it) }
        put("type", type)
        studyDay?.let { put("studyDay", it) }
        put("visitLogs", JSONArray(visitLogs.map { it.toJson() }))
    }

    companion object {
        fun fromJson(o: JSONObject): PersonRecord {
            val logs = o.optJSONArray("visitLogs")?.let { arr ->
                List(arr.length()) { VisitLog.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList()
            return PersonRecord(
                id = o.optString("id", ""),
                name = o.optString("name", ""),
                gender = o.optStringOrNull("gender"),
                age = o.optStringOrNull("age"),
                mobile = o.optStringOrNull("mobile"),
                address = o.optString("address", ""),
                language = o.optStringOrNull("language"),
                interestLevel = o.optStringOrNull("interestLevel"),
                nextVisitDate = o.optString("nextVisitDate", ""),
                nextVisitTime = o.optStringOrNull("nextVisitTime"),
                notes = o.optStringOrNull("notes"),
                type = o.optString("type", "rv"),
                studyDay = o.optStringOrNull("studyDay"),
                visitLogs = logs,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  REPOSITORY
// ─────────────────────────────────────────────────────────────────────────
/** Reads/writes the same `flutter.all_records` blob the notification layer uses. */
object PeopleRepository {
    private const val PREFS = "FlutterSharedPreferences"
    private const val KEY = "flutter.all_records"

    private fun Context.prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<PersonRecord> = try {
        val arr = JSONArray(context.prefs().getString(KEY, "[]") ?: "[]")
        List(arr.length()) { PersonRecord.fromJson(arr.getJSONObject(it)) }
    } catch (_: Exception) {
        emptyList()
    }

    /** Upserts a record by id. */
    fun save(context: Context, record: PersonRecord): Boolean = try {
        val prefs = context.prefs()
        val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        val out = JSONArray()
        var replaced = false
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optString("id") == record.id) {
                out.put(record.toJson())
                replaced = true
            } else {
                out.put(obj)
            }
        }
        if (!replaced) out.put(record.toJson())
        prefs.edit().putString(KEY, out.toString()).apply()
        true
    } catch (_: Exception) {
        false
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  SHARED DATE/TIME HELPERS (used by list + form)
// ─────────────────────────────────────────────────────────────────────────
internal const val DAY_MS = 24L * 60 * 60 * 1000

internal fun dateFormat(utc: Boolean = false) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    if (utc) timeZone = TimeZone.getTimeZone("UTC")
}

internal fun timeFormat() = SimpleDateFormat("h:mm a", Locale.US)