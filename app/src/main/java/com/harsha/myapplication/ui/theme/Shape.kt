package com.harsha.myapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Expressive shape scale: rounder than M3 baseline, with the extra "Increased" steps
// that new components (button groups, FABs, toolbars, sheets) read from.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(40.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)
// Non-composable accessors for the expressive "Increased" steps, so call sites outside composition
// (and without opting in individually) can use the theme scale instead of hard-coded corner radii.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ShapeLargeIncreased: CornerBasedShape = AppShapes.largeIncreased

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ShapeExtraLargeIncreased: CornerBasedShape = AppShapes.extraLargeIncreased

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ShapeExtraExtraLarge: CornerBasedShape = AppShapes.extraExtraLarge
