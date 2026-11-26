package com.harsha.myapplication.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.Lexend

// Lexend on the full M3 type scale (display/headline/body-small were previously falling
// back to the system font), plus the Expressive "Emphasized" variants: same size, heavier
// weight. Use e.g. MaterialTheme.typography.headlineMediumEmphasized for hero text.
// Tip: for Telugu, bundle Noto Sans Telugu and build a FontFamily(Font(Lexend...), Font(NotoTelugu...))
// so fallback glyphs have consistent weight and metrics.
private fun lexend(
    size: TextUnit,
    line: TextUnit,
    spacing: TextUnit,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = Lexend,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
    letterSpacing = spacing,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Typography = Typography(
    displayLarge = lexend(57.sp, 64.sp, (-0.25).sp),
    displayMedium = lexend(45.sp, 52.sp, 0.sp),
    displaySmall = lexend(36.sp, 44.sp, 0.sp),
    headlineLarge = lexend(32.sp, 40.sp, 0.sp),
    headlineMedium = lexend(28.sp, 36.sp, 0.sp),
    headlineSmall = lexend(24.sp, 32.sp, 0.sp),
    titleLarge = lexend(22.sp, 28.sp, 0.sp),
    titleMedium = lexend(16.sp, 24.sp, 0.15.sp, FontWeight.Medium),
    titleSmall = lexend(14.sp, 20.sp, 0.1.sp, FontWeight.Medium),
    bodyLarge = lexend(16.sp, 24.sp, 0.5.sp),
    bodyMedium = lexend(14.sp, 20.sp, 0.25.sp),
    bodySmall = lexend(12.sp, 16.sp, 0.4.sp),
    labelLarge = lexend(14.sp, 20.sp, 0.1.sp, FontWeight.Medium),
    labelMedium = lexend(12.sp, 16.sp, 0.5.sp, FontWeight.Medium),
    labelSmall = lexend(11.sp, 16.sp, 0.5.sp, FontWeight.Medium),

    // ---- Expressive emphasized styles ----
    headlineLargeEmphasized = lexend(32.sp, 40.sp, 0.sp, FontWeight.SemiBold),
    headlineMediumEmphasized = lexend(28.sp, 36.sp, 0.sp, FontWeight.SemiBold),
    headlineSmallEmphasized = lexend(24.sp, 32.sp, 0.sp, FontWeight.SemiBold),
    titleLargeEmphasized = lexend(22.sp, 28.sp, 0.sp, FontWeight.SemiBold),
    titleMediumEmphasized = lexend(16.sp, 24.sp, 0.15.sp, FontWeight.SemiBold),
    titleSmallEmphasized = lexend(14.sp, 20.sp, 0.1.sp, FontWeight.SemiBold),
    labelLargeEmphasized = lexend(14.sp, 20.sp, 0.1.sp, FontWeight.SemiBold),
    labelMediumEmphasized = lexend(12.sp, 16.sp, 0.5.sp, FontWeight.SemiBold),
    labelSmallEmphasized = lexend(11.sp, 16.sp, 0.5.sp, FontWeight.SemiBold),
)