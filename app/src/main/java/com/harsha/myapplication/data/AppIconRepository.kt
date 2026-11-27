package com.harsha.myapplication.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.harsha.myapplication.R

data class AppIconOption(
    val id: String,
    val name: String,
    val aliasName: String,
    val background: Color,
    val accent: Color,
    @DrawableRes val drawableRes: Int
)

val AppIconOptions = listOf(
    AppIconOption("default", "Teal", "IconDefault", Color(0xFF003035), Color(0xFF006870), R.drawable.logo),
    AppIconOption("emerald", "Emerald", "IconEmerald", Color(0xFF064E3B), Color(0xFF059669), R.drawable.logo_emerald),
    AppIconOption("ocean", "Ocean", "IconOcean", Color(0xFF0A2540), Color(0xFF0284C7), R.drawable.logo_ocean),
    AppIconOption("ruby", "Ruby", "IconRuby", Color(0xFF4C0519), Color(0xFF9F1239), R.drawable.logo_ruby),
    AppIconOption("amethyst", "Amethyst", "IconAmethyst", Color(0xFF2E1065), Color(0xFF6B21A8), R.drawable.logo_amethyst),
    AppIconOption("amber", "Amber", "IconAmber", Color(0xFF451A03), Color(0xFFB45309), R.drawable.logo_amber),
    AppIconOption("midnight", "Midnight", "IconMidnight", Color(0xFF0F172A), Color(0xFF334155), R.drawable.logo_midnight),
    AppIconOption("sunset", "Sunset", "IconSunset", Color(0xFF3B0764), Color(0xFF7E22CE), R.drawable.logo_sunset),
)

object AppIconRepository {
    private const val FILE = "app_icon_prefs"
    private const val KEY_ICON = "selected_icon_alias"
    private const val PACKAGE = "com.harsha.myapplication"

    private var store: SharedPreferences? = null

    var currentAlias by mutableStateOf("IconDefault")
        private set

    val currentOption: AppIconOption
        get() = AppIconOptions.firstOrNull { it.aliasName == currentAlias } ?: AppIconOptions.first()

    fun init(context: Context) {
        if (store != null) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = sp
        val saved = sp.getString(KEY_ICON, "IconDefault") ?: "IconDefault"
        currentAlias = saved
    }

    fun setIcon(context: Context, option: AppIconOption) {
        if (currentAlias == option.aliasName) return
        currentAlias = option.aliasName
        store?.edit()?.putString(KEY_ICON, option.aliasName)?.apply()

        val pm = context.packageManager
        AppIconOptions.forEach { opt ->
            val comp = ComponentName(PACKAGE, "$PACKAGE.${opt.aliasName}")
            val state = if (opt.aliasName == option.aliasName) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
            } catch (_: Throwable) {}
        }
    }
}
