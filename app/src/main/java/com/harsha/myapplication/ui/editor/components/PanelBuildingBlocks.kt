@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.ui.notes.PanelHeight
import com.harsha.myapplication.ui.theme.pressScale

// ---------------------------------------------------------------------------------------------
// Shared M3 building blocks
// ---------------------------------------------------------------------------------------------

@Composable
internal fun BarToggle(
    checked: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = colors.onSurfaceVariant,
            checkedContainerColor = colors.secondaryContainer,
            checkedContentColor = colors.onSecondaryContainer
        ),
        shapes = IconButtonDefaults.toggleableShapes(), // round -> squircle when checked
        content = content
    )
}

// Hours-sheet style building blocks shared by the Aa, Align and Image panels: caption sections
// split by hairline dividers, connected button groups (primary-filled selection, spring-animated
// corners) and ringed colour swatches. Everything comes from MaterialTheme.

/** One button of a [ConnectedButtonGroup]. */
internal class GroupItem(
    val description: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val label: String? = null,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val role: Role = Role.RadioButton
)

/**
 * Material 3 Expressive connected button group built from the real [ToggleButton]:
 * equal-width buttons, 2dp apart. Pressing squares a button off, selecting rounds it fully and
 * fills it with primary - the corner morph is driven by the theme's MotionScheme.
 * [stacked] puts the icon above its label (taller buttons, used for the list types).
 */
@Composable
internal fun ConnectedButtonGroup(
    items: List<GroupItem>,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    stacked: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val last = items.lastIndex
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val shapes = when {
                last == 0 -> ToggleButtonDefaults.shapesFor(height)
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == last -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = item.selected,
                onCheckedChange = { item.onClick() },
                enabled = item.enabled,
                shapes = shapes,
                colors = ToggleButtonDefaults.colors(
                    containerColor = colors.surfaceContainerHighest,
                    contentColor = colors.onSurface,
                    checkedContainerColor = colors.primary,
                    checkedContentColor = colors.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = height)
                    .semantics { contentDescription = item.description }
            ) {
                val icon = item.icon
                val label = item.label
                if (stacked) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (icon != null) Icon(icon, null, modifier = Modifier.size(20.dp))
                        if (label != null) {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                } else {
                    if (icon != null) Icon(icon, null, modifier = Modifier.size(20.dp))
                    if (icon != null && label != null) Spacer(Modifier.width(6.dp))
                    if (label != null) {
                        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Fixed-height scrolling body of a docked panel (all three panels share [PanelHeight]). */
@Composable
internal fun PanelScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** A labelled block: small caption on top, content 8dp below it. */
@Composable
internal fun PanelSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
internal fun PanelDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun SwatchRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

/** Round colour swatch in a 44dp touch target. [color] null = "none" (shows the reset icon). */
@Composable
internal fun Swatch(
    color: Color?,
    label: String,
    selected: Boolean,
    checkTint: Color,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val corner by animateDpAsState(
        if (selected) 10.dp else 16.dp,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "swatchCorner"
    )
    val swatchShape = RoundedCornerShape(corner)
    val swatchSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(swatchSource, 0.88f)
            .size(44.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = swatchSource,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(swatchShape)
                .then(if (color != null) Modifier.background(color) else Modifier)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) colors.primary else colors.outline.copy(alpha = 0.5f),
                    swatchShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                color == null -> Icon(
                    Icons.Default.FormatColorReset,
                    contentDescription = null,
                    tint = if (selected) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                selected -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = checkTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
