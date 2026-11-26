@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.harsha.myapplication.*

/** Long-press label (tooltip) around any control. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Tip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content
    )
}

@Composable
internal fun TipIconButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Tip(label) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
            content = content
        )
    }
}

/** Primary top-bar action (Edit / Done): M3 filled tonal icon button with a long-press label. */
@Composable
internal fun TipTonalIconButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Tip(label) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
            content = content
        )
    }
}
