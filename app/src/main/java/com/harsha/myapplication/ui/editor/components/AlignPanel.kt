@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.editor.richtext.ListKind

// ---------------------------------------------------------------------------------------------
// Align panel (alignment, list types, indent). Same height as the format panel.
// ---------------------------------------------------------------------------------------------

internal val alignItems = listOf(
    Triple(TextAlign.Start, Icons.AutoMirrored.Filled.FormatAlignLeft, "Align left"),
    Triple(TextAlign.Center, Icons.Default.FormatAlignCenter, "Align center"),
    Triple(TextAlign.End, Icons.AutoMirrored.Filled.FormatAlignRight, "Align right"),
    Triple(TextAlign.Justify, Icons.Default.FormatAlignJustify, "Justify")
)

internal val listItems = listOf(
    Triple(ListKind.Bullet, Icons.AutoMirrored.Filled.FormatListBulleted, "Bullets"),
    Triple(ListKind.Number, Icons.Default.FormatListNumbered, "Numbers"),
    Triple(ListKind.Letter, Icons.Default.SortByAlpha, "Letters"),
    Triple(ListKind.Check, Icons.Default.CheckBox, "Tasks")
)

internal val indentItems = listOf(
    Triple(-1, Icons.AutoMirrored.Filled.FormatIndentDecrease, "Outdent"),
    Triple(1, Icons.AutoMirrored.Filled.FormatIndentIncrease, "Indent")
)

@Composable
internal fun AlignPanel(
    align: TextAlign,
    onAlign: (TextAlign) -> Unit,
    activeList: ListKind?,
    onList: (ListKind) -> Unit,
    onIndent: (Int) -> Unit
) {
    PanelScaffold {
        PanelSection("Alignment") {
            ConnectedButtonGroup(
                items = alignItems.map { (value, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = value == align,
                        onClick = { onAlign(value) },
                        icon = icon
                    )
                }
            )
        }

        PanelDivider()

        PanelSection("Lists") {
            ConnectedButtonGroup(
                items = listItems.map { (kind, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = kind == activeList,
                        onClick = { onList(kind) },
                        label = label,
                        icon = icon
                    )
                },
                height = 56.dp,
                stacked = true
            )
        }

        PanelDivider()

        PanelSection("Indent") {
            ConnectedButtonGroup(
                items = indentItems.map { (delta, icon, label) ->
                    GroupItem(
                        description = label,
                        selected = false,
                        onClick = { onIndent(delta) },
                        label = label,
                        icon = icon,
                        enabled = activeList != null,
                        role = Role.Button
                    )
                }
            )
        }
    }
}
