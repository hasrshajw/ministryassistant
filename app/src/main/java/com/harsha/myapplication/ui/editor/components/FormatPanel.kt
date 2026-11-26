@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.editor.HighlightColors
import com.harsha.myapplication.ui.editor.highlightColorOf
import com.harsha.myapplication.ui.editor.richtext.FmtType
import com.harsha.myapplication.ui.editor.richtext.FontColors
import com.harsha.myapplication.ui.editor.richtext.InkBlack
import com.harsha.myapplication.ui.editor.richtext.TextSize

// ---------------------------------------------------------------------------------------------
// Format panel (B / I / U / S / highlight, font size, font colour)
// ---------------------------------------------------------------------------------------------

internal val toggleItems: List<Triple<FmtType, ImageVector, String>> = listOf(
    Triple(FmtType.Bold, Icons.Default.FormatBold, "Bold"),
    Triple(FmtType.Italic, Icons.Default.FormatItalic, "Italic"),
    Triple(FmtType.Underline, Icons.Default.FormatUnderlined, "Underline"),
    Triple(FmtType.Strike, Icons.Default.StrikethroughS, "Strikethrough")
)

@Composable
internal fun FormatPanel(
    isOn: (FmtType) -> Boolean,
    onToggle: (FmtType) -> Unit,
    sizeSelected: TextSize,
    onSize: (TextSize) -> Unit,
    colorSelected: Int,
    onColor: (Color) -> Unit,
    /** ARGB of the highlight under the selection, or null when none. */
    highlightSelected: Int?,
    onHighlight: (Color) -> Unit,
    onHighlightClear: () -> Unit,
    onClear: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    PanelScaffold {
        PanelSection("Style") {
            ConnectedButtonGroup(
                items = toggleItems.map { (type, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = isOn(type),
                        onClick = { onToggle(type) },
                        icon = icon,
                        role = Role.Checkbox
                    )
                } + GroupItem(
                    description = "Clear formatting",
                    selected = false,
                    onClick = onClear,
                    icon = Icons.Default.FormatClear,
                    role = Role.Button
                )
            )
        }

        PanelDivider()

        PanelSection("Font size") {
            ConnectedButtonGroup(
                items = TextSize.entries.map { size ->
                    GroupItem(
                        description = size.label,
                        selected = size == sizeSelected,
                        onClick = { onSize(size) },
                        label = size.label
                    )
                }
            )
        }

        PanelDivider()

        PanelSection("Font color") {
            SwatchRow {
                FontColors.forEach { (c, label) ->
                    // "Default" follows the theme, so it never disappears on a dark panel
                    val shown = if (c == InkBlack) colors.onSurface else c
                    Swatch(
                        color = shown,
                        label = label,
                        selected = c.toArgb() == colorSelected,
                        checkTint = if (shown.luminance() > 0.5f) InkBlack else Color.White,
                        onClick = { onColor(c) }
                    )
                }
            }
        }

        PanelDivider()

        PanelSection("Highlight") {
            SwatchRow {
                Swatch(
                    color = null,
                    label = "No highlight",
                    selected = highlightSelected == null,
                    checkTint = colors.primary,
                    onClick = onHighlightClear
                )
                HighlightColors.forEach { (c, label) ->
                    Swatch(
                        color = c,
                        label = "$label highlight",
                        selected = highlightSelected != null && highlightColorOf(highlightSelected) == c,
                        checkTint = InkBlack,
                        onClick = { onHighlight(c) }
                    )
                }
            }
        }
    }
}
