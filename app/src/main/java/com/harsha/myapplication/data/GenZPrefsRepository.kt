package com.harsha.myapplication.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Gender(val label: String) { Male("Male"), Female("Female"), Other("Other") }

/**
 * Which notification categories use Gen-Z tone when the master Gen-Z switch is on.
 * Defaults are all true so enabling Gen-Z covers everything until the user opts out.
 */
data class GenZTypeFlags(
    val dailySummary: Boolean = true,
    val dailyText: Boolean = true,
    val visitReminder: Boolean = true,
    val afterVisit: Boolean = true,
    val overdue: Boolean = true,
    val weeklySummary: Boolean = true,
)

data class GenZPrefs(
    val enabled: Boolean = false,
    val name: String = "",
    val gender: Gender? = null,
    val types: GenZTypeFlags = GenZTypeFlags(),
)

/**
 * Gen-Z mode settings, saved to disk and exposed as Compose state.
 */
object GenZPrefsRepository {
    private const val FILE = "genz_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_NAME = "name"
    private const val KEY_GENDER = "gender"
    private const val KEY_T_DAILY = "t_daily_summary"
    private const val KEY_T_TEXT = "t_daily_text"
    private const val KEY_T_VISIT = "t_visit"
    private const val KEY_T_AFTER = "t_after"
    private const val KEY_T_OVERDUE = "t_overdue"
    private const val KEY_T_WEEKLY = "t_weekly"

    private var store: android.content.SharedPreferences? = null

    var prefs by mutableStateOf(GenZPrefs())
        private set

    fun init(context: Context) {
        if (store != null) return
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = sp
        prefs = GenZPrefs(
            enabled = sp.getBoolean(KEY_ENABLED, false),
            name = sp.getString(KEY_NAME, "").orEmpty(),
            gender = sp.getString(KEY_GENDER, null)?.let { runCatching { Gender.valueOf(it) }.getOrNull() },
            types = GenZTypeFlags(
                dailySummary = sp.getBoolean(KEY_T_DAILY, true),
                dailyText = sp.getBoolean(KEY_T_TEXT, true),
                visitReminder = sp.getBoolean(KEY_T_VISIT, true),
                afterVisit = sp.getBoolean(KEY_T_AFTER, true),
                overdue = sp.getBoolean(KEY_T_OVERDUE, true),
                weeklySummary = sp.getBoolean(KEY_T_WEEKLY, true),
            ),
        )
    }

    fun enable(name: String, gender: Gender) =
        save(prefs.copy(enabled = true, name = name.trim(), gender = gender))

    fun disable() = save(prefs.copy(enabled = false))

    fun updateTypes(transform: (GenZTypeFlags) -> GenZTypeFlags) =
        save(prefs.copy(types = transform(prefs.types)))

    fun updateProfile(name: String, gender: Gender) =
        save(prefs.copy(name = name.trim(), gender = gender))

    /** True when master Gen-Z is on AND this category is opted in. */
    fun usesGenZ(category: GenZCategory): Boolean {
        if (!prefs.enabled) return false
        return when (category) {
            GenZCategory.DailySummary -> prefs.types.dailySummary
            GenZCategory.DailyText -> prefs.types.dailyText
            GenZCategory.VisitReminder -> prefs.types.visitReminder
            GenZCategory.AfterVisit -> prefs.types.afterVisit
            GenZCategory.Overdue -> prefs.types.overdue
            GenZCategory.WeeklySummary -> prefs.types.weeklySummary
        }
    }

    private fun save(new: GenZPrefs) {
        prefs = new
        store?.edit()
            ?.putBoolean(KEY_ENABLED, new.enabled)
            ?.putString(KEY_NAME, new.name)
            ?.putString(KEY_GENDER, new.gender?.name)
            ?.putBoolean(KEY_T_DAILY, new.types.dailySummary)
            ?.putBoolean(KEY_T_TEXT, new.types.dailyText)
            ?.putBoolean(KEY_T_VISIT, new.types.visitReminder)
            ?.putBoolean(KEY_T_AFTER, new.types.afterVisit)
            ?.putBoolean(KEY_T_OVERDUE, new.types.overdue)
            ?.putBoolean(KEY_T_WEEKLY, new.types.weeklySummary)
            ?.apply()
    }
}

enum class GenZCategory {
    DailySummary,
    DailyText,
    VisitReminder,
    AfterVisit,
    Overdue,
    WeeklySummary,
}
