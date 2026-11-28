package com.harsha.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * Everything the Notification settings screen controls.
 * Times are stored as "minutes since midnight" (0..1439), durations as minutes,
 * and [weeklyDay] as a [Calendar] day constant (Calendar.SUNDAY = 1 ... Calendar.SATURDAY = 7).
 */
data class NotificationPrefs(
    /** Master switch. When false nothing is delivered, but the individual choices below are kept. */
    val masterEnabled: Boolean = true,

    val dailySummaryEnabled: Boolean = true,
    val dailySummaryMinutes: Int = 20 * 60,

    val dailyTextEnabled: Boolean = true,
    val dailyTextMinutes: Int = 7 * 60,

    val visitBeforeEnabled: Boolean = true,
    /** How long BEFORE a return visit / study the reminder fires. */
    val visitBeforeLeadMinutes: Int = 60,

    val visitAfterEnabled: Boolean = true,
    /** How long AFTER a return visit / study the follow-up fires. */
    val visitAfterDelayMinutes: Int = 120,

    val weeklySummaryEnabled: Boolean = true,
    val weeklyDay: Int = Calendar.SUNDAY,
    val weeklyMinutes: Int = 18 * 60,
) {
    val enabledCount: Int
        get() = listOf(
            dailySummaryEnabled, dailyTextEnabled, visitBeforeEnabled,
            visitAfterEnabled, weeklySummaryEnabled
        ).count { it }

    // Use these (not the raw flags) when deciding whether to schedule / post something.
    val dailySummaryActive get() = masterEnabled && dailySummaryEnabled
    val dailyTextActive get() = masterEnabled && dailyTextEnabled
    val visitBeforeActive get() = masterEnabled && visitBeforeEnabled
    val visitAfterActive get() = masterEnabled && visitAfterEnabled
    val weeklySummaryActive get() = masterEnabled && weeklySummaryEnabled

    companion object {
        const val TOTAL_OPTIONS = 5
    }
}

/**
 * Single source of truth for notification preferences. Same pattern as AppearanceRepository:
 * call [init] once from MainActivity.onCreate. [prefs] is Compose state, so UI recomposes on change.
 *
 * Set [onChanged] (e.g. from your scheduler) to re-schedule alarms / WorkManager jobs whenever
 * anything changes.
 */
object NotificationPrefsRepository {
    private const val FILE = "notification_prefs"

    private const val K_MASTER = "master"
    private const val K_DS_ON = "daily_summary_on"
    private const val K_DS_AT = "daily_summary_at"
    private const val K_DT_ON = "daily_text_on"
    private const val K_DT_AT = "daily_text_at"
    private const val K_VB_ON = "visit_before_on"
    private const val K_VB_MIN = "visit_before_min"
    private const val K_VA_ON = "visit_after_on"
    private const val K_VA_MIN = "visit_after_min"
    private const val K_WS_ON = "weekly_on"
    private const val K_WS_DAY = "weekly_day"
    private const val K_WS_AT = "weekly_at"

    private var store: SharedPreferences? = null

    var prefs by mutableStateOf(NotificationPrefs())
        private set

    /** Called after every change with the new value. */
    var onChanged: ((NotificationPrefs) -> Unit)? = null

    /**
     * Synchronized: alarms, the boot receiver and the UI can all start this on different threads in a fresh process.
     * [store] is assigned last, so no caller can ever see "initialised" while [prefs] still holds the defaults.
     */
    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val d = NotificationPrefs()
        prefs = NotificationPrefs(
            masterEnabled = sp.getBoolean(K_MASTER, d.masterEnabled),
            dailySummaryEnabled = sp.getBoolean(K_DS_ON, d.dailySummaryEnabled),
            dailySummaryMinutes = sp.getInt(K_DS_AT, d.dailySummaryMinutes).coerceIn(0, 1439),
            dailyTextEnabled = sp.getBoolean(K_DT_ON, d.dailyTextEnabled),
            dailyTextMinutes = sp.getInt(K_DT_AT, d.dailyTextMinutes).coerceIn(0, 1439),
            visitBeforeEnabled = sp.getBoolean(K_VB_ON, d.visitBeforeEnabled),
            visitBeforeLeadMinutes = sp.getInt(K_VB_MIN, d.visitBeforeLeadMinutes).coerceAtLeast(1),
            visitAfterEnabled = sp.getBoolean(K_VA_ON, d.visitAfterEnabled),
            visitAfterDelayMinutes = sp.getInt(K_VA_MIN, d.visitAfterDelayMinutes).coerceAtLeast(1),
            weeklySummaryEnabled = sp.getBoolean(K_WS_ON, d.weeklySummaryEnabled),
            weeklyDay = sp.getInt(K_WS_DAY, d.weeklyDay).coerceIn(Calendar.SUNDAY, Calendar.SATURDAY),
            weeklyMinutes = sp.getInt(K_WS_AT, d.weeklyMinutes).coerceIn(0, 1439),
        )
        store = sp
    }

    fun update(transform: (NotificationPrefs) -> NotificationPrefs) {
        val next = transform(prefs)
        if (next == prefs) return
        prefs = next
        store?.edit()
            ?.putBoolean(K_MASTER, next.masterEnabled)
            ?.putBoolean(K_DS_ON, next.dailySummaryEnabled)
            ?.putInt(K_DS_AT, next.dailySummaryMinutes)
            ?.putBoolean(K_DT_ON, next.dailyTextEnabled)
            ?.putInt(K_DT_AT, next.dailyTextMinutes)
            ?.putBoolean(K_VB_ON, next.visitBeforeEnabled)
            ?.putInt(K_VB_MIN, next.visitBeforeLeadMinutes)
            ?.putBoolean(K_VA_ON, next.visitAfterEnabled)
            ?.putInt(K_VA_MIN, next.visitAfterDelayMinutes)
            ?.putBoolean(K_WS_ON, next.weeklySummaryEnabled)
            ?.putInt(K_WS_DAY, next.weeklyDay)
            ?.putInt(K_WS_AT, next.weeklyMinutes)
            ?.apply()
        onChanged?.invoke(next)
    }
}
