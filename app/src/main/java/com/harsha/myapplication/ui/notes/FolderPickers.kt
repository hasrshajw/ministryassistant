@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.data.DEFAULT_CATEGORY
import com.harsha.myapplication.ui.theme.pressScale
import kotlinx.coroutines.launch

/**
 * Folder button under the note title. Same look as the Add-hours buttons: 40dp tall, fully round,
 * surfaceContainerHighest container; tapping it opens the "Move note to" bottom sheet.
 */
@Composable
internal fun CategorySelector(
    selected: String,
    folderColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = colors.surfaceContainerHighest,
        contentColor = colors.onSurface,
        interactionSource = source,
        modifier = Modifier
            .pressScale(source, 0.95f, enabled)
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(start = 14.dp, end = if (enabled) 10.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = if (selected == DEFAULT_CATEGORY) colors.onSurfaceVariant else folderColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                selected,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
            if (enabled) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change folder",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// "Move note to" modal bottom sheet. The sheet wraps its content, so one or two folders give a
// small sheet; a long list is capped at FolderListMaxHeight and scrolls inside the sheet instead of
// growing to full screen.
internal val FolderRowHeight = 64.dp
internal val FolderListMaxHeight = 352.dp   // ~5 rows; the half-visible 6th row hints that it scrolls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderSheet(
    folders: List<String>,
    selected: String,
    colorOf: (String) -> Color,
    onSelect: (String) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // "Uncategorized" always comes first, the rest keep their order.
    val ordered = remember(folders) { folders.sortedBy { if (it == DEFAULT_CATEGORY) 0 else 1 } }

    // Slide the sheet away first, then report back (so there is no abrupt disappearance).
    fun close(after: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header: title + hint on the left, tonal close button on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Move note to",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )
                    Text(
                        "Choose a folder for this note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = { close() }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Folder cards: wraps content up to FolderListMaxHeight, then scrolls.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = FolderListMaxHeight)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(ordered, key = { _, name -> name }) { _, name ->
                    FolderRow(
                        name = name,
                        isDefault = name == DEFAULT_CATEGORY,
                        color = colorOf(name),
                        selected = name == selected,
                        onClick = { close { onSelect(name) } }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // New folder: same button as "Detected scriptures" (48dp tonal button, icon + labelLarge text),
            // coloured from the theme so it follows the sheet's green.
            FilledTonalButton(
                onClick = { close { onNewFolder() } },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("New folder", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** One folder card: tinted folder tile, name, and a radio-style indicator that fills when selected. */
@Composable
internal fun FolderRow(
    name: String,
    isDefault: Boolean,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    // Selected rows round off more (spatial spring), colours fade with the effects spring.
    val corner by animateDpAsState(
        if (selected) 28.dp else 16.dp, motion.defaultSpatialSpec(), label = "folderCorner"
    )
    val shape = RoundedCornerShape(corner)
    val source = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) colors.primaryContainer else colors.surfaceContainerLowest,
        motion.defaultEffectsSpec(),
        label = "folderContainer"
    )
    val borderColor by animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        motion.defaultEffectsSpec(),
        label = "folderBorder"
    )
    val tile = if (isDefault) colors.onSurfaceVariant else color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FolderRowHeight)
            .pressScale(source, 0.97f)
            .clip(shape)
            .background(container)
            .border(1.5.dp, borderColor, shape)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tile.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = tile,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        // Radio-style indicator: empty ring -> filled primary circle with a check
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(colors.primary)
                    else Modifier.border(2.dp, colors.outline, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
