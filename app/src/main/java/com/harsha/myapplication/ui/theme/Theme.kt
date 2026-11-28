package com.harsha.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoundEffectOnInteraction

internal val LightColorScheme = lightColorScheme(
    primary = StudyPurple,
    onPrimary = Color.White,
    primaryContainer = StudyCardLavender,
    onPrimaryContainer = StudyOnPrimaryContainer,
    // Secondary drives selected states in Expressive components (nav indicator, button groups,
    // tonal buttons). It is now a green tint instead of white, so those states are visible.
    secondary = StudySecondary,
    onSecondary = Color.White,
    secondaryContainer = StudyGreenTint,
    onSecondaryContainer = StudyDeepGreen,
    tertiary = StudyTertiary,
    onTertiary = Color.White,
    tertiaryContainer = StudyTertiaryContainer,
    onTertiaryContainer = StudyOnTertiaryContainer,
    background = StudyBackground,
    onBackground = StudyTextPrimary,
    surface = StudyBackground,
    onSurface = StudyTextPrimary,
    surfaceVariant = StudyNeutralLight,
    onSurfaceVariant = StudyIconGrey,                 // icons + secondary text
    outline = StudyOutline,
    outlineVariant = StudyOutlineVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = StudySurfaceContainerLow,   // bottom sheet – clear lift from bg
    surfaceContainer = StudySurface,
    surfaceContainerHigh = StudyPeekCard,
    surfaceContainerHighest = StudySurfaceContainerHighest,
    error = StudyError,
    onError = StudyOnError,
    errorContainer = StudyErrorContainer,
    onErrorContainer = StudyOnErrorContainer,
    surfaceDim = StudySurfaceDim,
    surfaceBright = StudySurfaceBright,
    inverseSurface = StudyInverseSurface,
    inverseOnSurface = StudyInverseOnSurface,
    inversePrimary = StudyInversePrimary,
    scrim = Color.Black,
    primaryFixed = StudyPrimaryFixed,
    primaryFixedDim = StudyPrimaryFixedDim,
    onPrimaryFixed = StudyOnPrimaryFixed,
    onPrimaryFixedVariant = StudyOnPrimaryFixedVariant,
    secondaryFixed = StudySecondaryFixed,
    secondaryFixedDim = StudySecondaryFixedDim,
    onSecondaryFixed = StudyOnSecondaryFixed,
    onSecondaryFixedVariant = StudyOnSecondaryFixedVariant,
    tertiaryFixed = StudyTertiaryFixed,
    tertiaryFixedDim = StudyTertiaryFixedDim,
    onTertiaryFixed = StudyOnTertiaryFixed,
    onTertiaryFixedVariant = StudyOnTertiaryFixedVariant,
)

internal val DarkColorScheme = darkColorScheme(
    primary = StudyDarkPrimary,
    onPrimary = StudyDarkOnPrimary,
    primaryContainer = StudyDarkPrimaryContainer,
    onPrimaryContainer = StudyDarkOnPrimaryContainer,
    secondary = StudyDarkSecondary,
    onSecondary = StudyDarkOnSecondary,
    secondaryContainer = StudyGreenTrack,
    onSecondaryContainer = StudyGreenTint,
    tertiary = StudyDarkTertiary,
    onTertiary = StudyDarkOnTertiary,
    tertiaryContainer = StudyDarkTertiaryContainer,
    onTertiaryContainer = StudyDarkOnTertiaryContainer,
    background = StudyDarkBackground,
    onBackground = StudyDarkText,
    surface = StudyDarkBackground,
    onSurface = StudyDarkText,
    surfaceVariant = StudyNeutralDark,
    onSurfaceVariant = StudyDarkTextVariant,
    outline = StudyDarkOutline,
    outlineVariant = StudyDarkOutlineVariant,
    surfaceContainerLowest = StudyDarkLowest,
    surfaceContainerLow = StudyDarkSurfaceLow,        // bottom sheet
    surfaceContainer = StudyDarkSurface,
    surfaceContainerHigh = StudyDarkSurfaceHigh,
    surfaceContainerHighest = StudyDarkSurfaceHighest,
    error = StudyDarkError,
    onError = StudyDarkOnError,
    errorContainer = StudyDarkErrorContainer,
    onErrorContainer = StudyDarkOnErrorContainer,
    surfaceDim = StudyDarkSurfaceDim,
    surfaceBright = StudyDarkSurfaceBright,
    inverseSurface = StudyDarkInverseSurface,
    inverseOnSurface = StudyDarkInverseOnSurface,
    inversePrimary = StudyDarkInversePrimary,
    scrim = Color.Black,
    primaryFixed = StudyPrimaryFixed,
    primaryFixedDim = StudyPrimaryFixedDim,
    onPrimaryFixed = StudyOnPrimaryFixed,
    onPrimaryFixedVariant = StudyOnPrimaryFixedVariant,
    secondaryFixed = StudySecondaryFixed,
    secondaryFixedDim = StudySecondaryFixedDim,
    onSecondaryFixed = StudyOnSecondaryFixed,
    onSecondaryFixedVariant = StudyOnSecondaryFixedVariant,
    tertiaryFixed = StudyTertiaryFixed,
    tertiaryFixedDim = StudyTertiaryFixedDim,
    onTertiaryFixed = StudyOnTertiaryFixed,
    onTertiaryFixedVariant = StudyOnTertiaryFixedVariant,
)

/** The scheme a palette produces for light/dark, without dynamic color or pure black applied. */
internal fun paletteColorScheme(palette: ThemePalette, dark: Boolean): ColorScheme = when {
    palette.isBrand -> if (dark) DarkColorScheme else LightColorScheme
    else -> seedColorScheme(
        seed = if (palette.isDynamic) DefaultSeedColor else palette.seedColor,
        dark = dark,
        monochrome = palette.isMonochrome,
    )
}

/**
 * Resolves the final ColorScheme from the user's appearance choices.
 * Used by the app theme AND by the preview card in Appearance settings, so they always match.
 */
@Composable
fun rememberAppColorScheme(
    palette: ThemePalette,
    dark: Boolean,
    pureBlack: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    val context = LocalContext.current
    val generated = remember(palette, dark) { paletteColorScheme(palette, dark) }
    val base = if (palette.isDynamic && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else generated

    return if (pureBlack && dark) {
        base.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceDim = Color.Black,
        )
    } else base
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ThemePalette = DefaultPalette,   // "Study" brand palette
    // Only applies when the "Dynamic" palette is selected (Android 12+).
    dynamicColor: Boolean = false,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = rememberAppColorScheme(palette, darkTheme, pureBlack, dynamicColor)

    // MotionScheme.expressive() gives every Material component spring-based spatial/effects
    // motion (the same family as the tokens in Motion.kt).
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        typography = Typography,
    ) {
        // Compose 1.12+/1.13 plays the system click sound on every clickable by itself. The app already
        // plays its own click through AppFeedback (and honours the "sound effects" toggle in Customize
        // App), so silence the automatic one to avoid a double click and keep that toggle authoritative.
        SoundEffectOnInteraction(enabled = false, content = content)
    }
}