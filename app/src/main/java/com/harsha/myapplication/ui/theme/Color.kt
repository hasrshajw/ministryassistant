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
val StudyBackground = Color(0xFFF0F3F8)          // app background
val StudyButtonContainer = Color(0xFFFFFFFF)     // buttons + list rows / sheets sitting on the background
val StudyIconGrey = Color(0xFF454746)            // icons + secondary content color
val StudySurface = Color(0xFFEEF1EF)
// Surface container ladder (lowest → highest). Bottom sheet uses Low so it
// sits clearly above the page background without looking like a second card.
val StudySurfaceContainerLow = StudyButtonContainer // sheet / list rows: white, lifts off the background
val StudySurfaceContainerHighest = Color(0xFFDCE0DE) // icon wells, strongest fill
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
val StudyDarkSurfaceLow = Color(0xFF161918)     // sheet in dark
val StudyDarkSurfaceHigh = StudyNeutralDark
val StudyDarkSurfaceHighest = Color(0xFF323735) // icon wells in dark
val StudyDarkPrimary = StudyEmerald
val StudyDarkOnPrimary = StudyDeepGreen
val StudyDarkPrimaryContainer = StudyGreenTrack
val StudyDarkOnPrimaryContainer = StudyGreenTint
val StudyDarkText = Color(0xFFE3E8E6)
val StudyDarkTextVariant = StudyOnNeutralDark
val StudyDarkOutlineVariant = StudyOnNeutralLight
// ============================================================================
//  Material 3 Expressive additions: secondary / tertiary / outline roles.
//  Expressive components lean on these (selected states, loaders, button groups),
//  and any role left unset falls back to the stock purple baseline.
// ============================================================================
val StudySecondary = Color(0xFF3F6653)
val StudyTertiary = Color(0xFF2E6B73)
val StudyTertiaryContainer = Color(0xFFCDE8EC)
val StudyOnTertiaryContainer = Color(0xFF0B3A41)
val StudyOutline = Color(0xFF727975)

val StudyDarkSecondary = Color(0xFFA8CDB9)
val StudyDarkOnSecondary = Color(0xFF123324)
val StudyDarkTertiary = Color(0xFF8CCFD8)
val StudyDarkOnTertiary = Color(0xFF00363C)
val StudyDarkTertiaryContainer = Color(0xFF1F4E55)
val StudyDarkOnTertiaryContainer = Color(0xFFCDE8EC)
val StudyDarkOutline = Color(0xFF8B938F)