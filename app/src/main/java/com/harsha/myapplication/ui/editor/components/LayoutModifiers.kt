@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.offset
import com.harsha.myapplication.*

// ---------------------------------------------------------------------------------------------
// Layout helpers
// ---------------------------------------------------------------------------------------------

/**
 * Adds `insetPx() + extraPx` of empty space below the content, like Modifier.padding(bottom = ...),
 * but [insetPx] is read during LAYOUT. A changing value (the animating keyboard) then only re-runs
 * layout instead of recomposing the whole screen on every frame.
 */
internal fun Modifier.bottomInset(insetPx: () -> Int, extraPx: Int = 0): Modifier =
    layout { measurable, constraints ->
        val pad = insetPx() + extraPx
        val placeable = measurable.measure(constraints.offset(vertical = -pad))
        layout(placeable.width, placeable.height + pad) { placeable.placeRelative(0, 0) }
    }
