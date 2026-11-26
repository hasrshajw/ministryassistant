package com.harsha.myapplication.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// ───────────────────────── Models ─────────────────────────

enum class ThemeMode(val shortLabel: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

/**
 * seedColor == Transparent is the sentinel for "Dynamic / system" colors.
 * isBrand = the hand-tuned green Study palette from Theme.kt (not generated from a seed).
 */
data class ThemePalette(
    val name: String,
    val seedColor: Color,
    val isBrand: Boolean = false,
) {
    val isDynamic get() = seedColor == Color.Transparent
    val isMonochrome get() = seedColor == Color(0xFF000000)
}

internal val DefaultSeedColor = Color(0xFFED5564)

/** The app's own hand-tuned green look. */
val StudyPalette = ThemePalette("Study", StudyEmerald, isBrand = true)

/** What a fresh install starts with. Independent of the order of [PaletteColors]. */
val DefaultPalette = StudyPalette

val PaletteColors = listOf(
    ThemePalette("Dynamic", Color.Transparent),             // system (wallpaper) colors, listed first
    StudyPalette,                                           // the default

    ThemePalette("Crimson", Color(0xFFEC5464)),
    ThemePalette("Rose", Color(0xFFD81B60)),
    ThemePalette("Purple", Color(0xFF8E24AA)),
    ThemePalette("Monochrome", Color(0xFF000000)),
    ThemePalette("Deep Purple", Color(0xFF5E35B1)),
    ThemePalette("Indigo", Color(0xFF3949AB)),
    ThemePalette("Blue", Color(0xFF1E88E5)),
    ThemePalette("Sky Blue", Color(0xFF039BE5)),
    ThemePalette("Cyan", Color(0xFF00ACC1)),
    ThemePalette("Teal", Color(0xFF00897B)),
    ThemePalette("Green", Color(0xFF43A047)),
    ThemePalette("Light Green", Color(0xFF7CB342)),
    ThemePalette("Lime", Color(0xFFC0CA33)),
    ThemePalette("Yellow", Color(0xFFFDD835)),
    ThemePalette("Amber", Color(0xFFFFB300)),
    ThemePalette("Orange", Color(0xFFFB8C00)),
    ThemePalette("Deep Orange", Color(0xFFF4511E)),
    ThemePalette("Brown", Color(0xFF6D4C41)),
    ThemePalette("Grey", Color(0xFF757575)),
    ThemePalette("Blue Grey", Color(0xFF546E7A)),
)

// ───────────────────────── Seed -> ColorScheme ─────────────────────────

private fun tone(hue: Float, sat: Float, lightness: Float): Color =
    Color(ColorUtils.HSLToColor(floatArrayOf(((hue % 360f) + 360f) % 360f, sat.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))))

/** Lightweight seed -> ColorScheme generator (HSL tonal ramps), no external library needed. */
internal fun seedColorScheme(seed: Color, dark: Boolean, monochrome: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val h = hsl[0]
    val s = if (monochrome) 0f else hsl[1].coerceIn(0.35f, 0.85f)
    val sSec = s * 0.4f
    val sTer = s * 0.6f
    val hTer = h + 60f
    val sN = s * 0.12f   // neutral surfaces

    return if (!dark) lightColorScheme(
        primary = tone(h, s, 0.40f), onPrimary = Color.White,
        primaryContainer = tone(h, s, 0.90f), onPrimaryContainer = tone(h, s, 0.10f),
        secondary = tone(h, sSec, 0.40f), onSecondary = Color.White,
        secondaryContainer = tone(h, sSec, 0.90f), onSecondaryContainer = tone(h, sSec, 0.10f),
        tertiary = tone(hTer, sTer, 0.40f), onTertiary = Color.White,
        tertiaryContainer = tone(hTer, sTer, 0.90f), onTertiaryContainer = tone(hTer, sTer, 0.10f),
        // Page background is a touch grey so white cards / list rows clearly lift off it.
        background = tone(h, sN, 0.94f), onBackground = tone(h, sN, 0.10f),
        surface = tone(h, sN, 0.94f), onSurface = tone(h, sN, 0.10f),
        surfaceVariant = tone(h, sN * 1.5f, 0.90f), onSurfaceVariant = tone(h, sN * 1.5f, 0.30f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White, surfaceContainer = tone(h, sN, 0.91f),
        surfaceContainerHigh = tone(h, sN, 0.88f), surfaceContainerHighest = tone(h, sN, 0.85f),
        outline = tone(h, sN * 1.5f, 0.50f), outlineVariant = tone(h, sN * 1.5f, 0.80f),
    ) else darkColorScheme(
        primary = tone(h, s, 0.80f), onPrimary = tone(h, s, 0.20f),
        primaryContainer = tone(h, s, 0.30f), onPrimaryContainer = tone(h, s, 0.90f),
        secondary = tone(h, sSec, 0.80f), onSecondary = tone(h, sSec, 0.20f),
        secondaryContainer = tone(h, sSec, 0.30f), onSecondaryContainer = tone(h, sSec, 0.90f),
        tertiary = tone(hTer, sTer, 0.80f), onTertiary = tone(hTer, sTer, 0.20f),
        tertiaryContainer = tone(hTer, sTer, 0.30f), onTertiaryContainer = tone(hTer, sTer, 0.90f),
        background = tone(h, sN, 0.08f), onBackground = tone(h, sN, 0.90f),
        surface = tone(h, sN, 0.08f), onSurface = tone(h, sN, 0.90f),
        surfaceVariant = tone(h, sN * 1.5f, 0.30f), onSurfaceVariant = tone(h, sN * 1.5f, 0.80f),
        surfaceContainerLowest = tone(h, sN, 0.05f),
        surfaceContainerLow = tone(h, sN, 0.14f), surfaceContainer = tone(h, sN, 0.17f),
        surfaceContainerHigh = tone(h, sN, 0.21f), surfaceContainerHighest = tone(h, sN, 0.26f),
        outline = tone(h, sN * 1.5f, 0.60f), outlineVariant = tone(h, sN * 1.5f, 0.30f),
    )
}

// ───────────────────────── Persistence ─────────────────────────

data class AppearancePrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: ThemePalette = DefaultPalette,
    val dynamicColor: Boolean = true,
    val pureBlack: Boolean = false,
)

/**
 * Single source of truth for the appearance settings. Same pattern as NotesRepository / BibleData:
 * call [init] once from MainActivity.onCreate (before setContent) so the saved theme is applied
 * on the very first frame. [prefs] is Compose state, so anything reading it recomposes on change.
 */
object AppearanceRepository {
    private const val FILE = "appearance_prefs"
    private const val K_MODE = "theme_mode"
    private const val K_PALETTE = "palette"
    private const val K_DYNAMIC = "dynamic_color"
    private const val K_PURE_BLACK = "pure_black"

    private var store: SharedPreferences? = null

    var prefs by mutableStateOf(AppearancePrefs())
        private set

    fun init(context: Context) {
        if (store != null) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = sp
        prefs = AppearancePrefs(
            themeMode = sp.getString(K_MODE, null)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            palette = PaletteColors.firstOrNull { it.name == sp.getString(K_PALETTE, null) }
                ?: DefaultPalette,
            dynamicColor = sp.getBoolean(K_DYNAMIC, true),
            pureBlack = sp.getBoolean(K_PURE_BLACK, false),
        )
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setPalette(palette: ThemePalette) = update { it.copy(palette = palette) }
    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }
    fun setPureBlack(enabled: Boolean) = update { it.copy(pureBlack = enabled) }

    private fun update(transform: (AppearancePrefs) -> AppearancePrefs) {
        val next = transform(prefs)
        prefs = next
        store?.edit()
            ?.putString(K_MODE, next.themeMode.name)
            ?.putString(K_PALETTE, next.palette.name)
            ?.putBoolean(K_DYNAMIC, next.dynamicColor)
            ?.putBoolean(K_PURE_BLACK, next.pureBlack)
            ?.apply()
    }
}