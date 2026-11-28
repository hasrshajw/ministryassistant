package com.harsha.myapplication.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Gender(val label: String) { Male("Male"), Female("Female"), Other("Other") }

data class GenZPrefs(
    val enabled: Boolean = false,
    val name: String = "",
    val gender: Gender? = null,
)

/**
 * Gen-Z mode settings, saved to disk (SharedPreferences) and exposed as Compose state,
 * so any screen that reads [prefs] updates automatically. Call [init] once with a context.
 */
object GenZPrefsRepository {
    private const val FILE = "genz_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_NAME = "name"
    private const val KEY_GENDER = "gender"

    private var store: android.content.SharedPreferences? = null

    var prefs by mutableStateOf(GenZPrefs())
        private set

    /** Safe to call on every composition; only the first call reads from disk. */
    fun init(context: Context) {
        if (store != null) return
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = sp
        prefs = GenZPrefs(
            enabled = sp.getBoolean(KEY_ENABLED, false),
            name = sp.getString(KEY_NAME, "").orEmpty(),
            gender = sp.getString(KEY_GENDER, null)?.let { runCatching { Gender.valueOf(it) }.getOrNull() },
        )
    }

    /** Turn the mode on and remember who the user is. */
    fun enable(name: String, gender: Gender) = save(GenZPrefs(enabled = true, name = name.trim(), gender = gender))

    /** Turn the mode off but keep the saved name / gender so the dialog can pre-fill them next time. */
    fun disable() = save(prefs.copy(enabled = false))

    private fun save(new: GenZPrefs) {
        prefs = new
        store?.edit()
            ?.putBoolean(KEY_ENABLED, new.enabled)
            ?.putString(KEY_NAME, new.name)
            ?.putString(KEY_GENDER, new.gender?.name)
            ?.apply()
    }
}
