package com.harsha.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
//  Brand palette taken from the Home screen ("Daily text" + "Monthly progress"):
//  deep green card, emerald accent, soft grey pills.
// ============================================================================
val StudyEmerald = Color(0xFF2FBF71)            // progress ring, "Completed", active dots
val StudyEmeraldDeep = Color(0xFF1F9D5B)        // emerald for icons / text on light surfaces (better contrast)
val StudyDeepGreen = Color(0xFF001F1E)          // today's Daily text card, Monthly progress card
val StudyDeepGreenDarkMode = Color(0xFF1A1A1A)  // Monthly progress card in dark theme
val StudyGreenTrack = Color(0xFF1E3F37)         // unfilled ring track, inactive dots (dark)
val StudyGreenTint = Color(0xFFC5DDD0)          // inactive dots (light), soft green container

val StudyNeutralLight = Color(0xFFE2E5E3)       // Daily text side pills / Monthly panel (light)
val StudyOnNeutralLight = Color(0xFF3A4140)
val StudyNeutralDark = Color(0xFF2B302F)        // Daily text side pills / Monthly panel (dark)
val StudyOnNeutralDark = Color(0xFFC9CFCD)

// ============================================================================
//  Theme palette. Names are unchanged (Theme.kt keeps compiling); the values are now the
//  green / neutral Home palette instead of the old lavender one.
// ============================================================================

// ---- Light palette (Study brand) ----
val StudyBackground = Color(0xFFF7F9F8)
val StudySurface = Color(0xFFEEF1EF)
val StudyCardLavender = StudyGreenTint          // (legacy name) primary container
val StudyPeekCard = StudyNeutralLight           // (legacy name) card / pill grey, same as Home
val StudyTextPrimary = Color(0xFF191C1B)
val StudyTextSecondary = StudyOnNeutralLight
val StudyPurple = StudyDeepGreen                // (legacy name) primary
val StudyOnPrimaryContainer = StudyDeepGreen
val StudyOutlineVariant = StudyOnNeutralDark

// ---- Dark palette ----
val StudyDarkBackground = Color(0xFF121413)
val StudyDarkLowest = Color(0xFF0D100F)
val StudyDarkSurface = Color(0xFF1A1D1C)
val StudyDarkSurfaceHigh = StudyNeutralDark
val StudyDarkPrimary = StudyEmerald
val StudyDarkOnPrimary = StudyDeepGreen
val StudyDarkPrimaryContainer = StudyGreenTrack
val StudyDarkOnPrimaryContainer = StudyGreenTint
val StudyDarkText = Color(0xFFE3E8E6)
val StudyDarkTextVariant = StudyOnNeutralDark
val StudyDarkOutlineVariant = StudyOnNeutralLight