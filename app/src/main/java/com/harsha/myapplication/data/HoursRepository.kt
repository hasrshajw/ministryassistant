package com.harsha.myapplication.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

/** One block of field-service time. [minutes] is stored (not hours) so there is no rounding drift. */
data class HourEntry(
    val id: Long,
    val dateMillis: Long,
    val minutes: Int,
)

/**
 * Hours logged through AddHoursSheet.
 *
 *  - [entries] is Compose snapshot state, so the Home card and the Monthly progress page
 *    update the moment an entry is added or deleted.
 *  - Saved in SharedPreferences as a small JSON array (a few hundred entries a year at most).
 *  - The monthly TARGET is not stored here: it comes from [SetupRepository.monthlyGoalHours].
 */
object HoursRepository {
    private const val PREFS = "hours_prefs"
    private const val KEY = "entries"

    var entries: List<HourEntry> by mutableStateOf(emptyList())
        private set

    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        entries = try {
            val arr = JSONArray(prefs(context).getString(KEY, "[]") ?: "[]")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                HourEntry(
                    id = o.optLong("id"),
                    dateMillis = o.optLong("date"),
                    minutes = o.optInt("minutes"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Adds [hours] (e.g. 1.5) logged on [date]. Returns false when the amount rounds to 0. */
    fun add(context: Context, hours: Double, date: Date): Boolean {
        init(context)
        val minutes = (hours * 60).roundToInt()
        if (minutes <= 0) return false
        val id = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
        entries = entries + HourEntry(id, date.time, minutes)
        persist(context)
        return true
    }

    fun delete(context: Context, id: Long) {
        init(context)
        entries = entries.filterNot { it.id == id }
        persist(context)
    }

    /** Total minutes logged in the given month. [month] is a Calendar.MONTH value (0-11). */
    fun minutesInMonth(year: Int, month: Int): Int =
        entriesInMonth(year, month).sumOf { it.minutes }

    /** Entries of the given month, newest day first. */
    fun entriesInMonth(year: Int, month: Int): List<HourEntry> {
        val cal = Calendar.getInstance()
        return entries
            .filter {
                cal.timeInMillis = it.dateMillis
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }
            .sortedWith(compareByDescending<HourEntry> { it.dateMillis }.thenByDescending { it.id })
    }

    private fun persist(context: Context) {
        val arr = JSONArray()
        entries.forEach {
            arr.put(JSONObject().put("id", it.id).put("date", it.dateMillis).put("minutes", it.minutes))
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
