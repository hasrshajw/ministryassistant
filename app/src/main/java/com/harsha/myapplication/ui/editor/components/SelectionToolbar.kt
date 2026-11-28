@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.editor.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.harsha.myapplication.ui.editor.HighlightColors
import com.harsha.myapplication.ui.editor.highlightColorOf
import com.harsha.myapplication.ui.editor.richtext.InkBlack
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred

// ---------------------------------------------------------------------------------------------
// Custom text-selection toolbar (replaces the system "Cut / Copy / Paste" bubble)
// ---------------------------------------------------------------------------------------------

internal enum class ToolbarMode { Main, Colors, More }

// Always-dark toolbar so it stays clearly visible over light and dark note backgrounds.
private val ToolbarBg = Color(0xFF202124)
private val ToolbarFg = Color(0xFFF1F3F4)
private val ToolbarChecked = Color.White.copy(alpha = 0.20f)
private val ToolbarAccent = Color(0xFF8AB4F8)

/**
 * Plugged into the text field through [LocalTextContextMenuToolbarProvider] (Compose Foundation 1.9+;
 * the old LocalTextToolbar is no longer used by text fields). While the field wants a menu,
 * [showTextContextMenu] is suspended here and [dataProvider] is set; [SelectionToolbar] reads it
 * (it is snapshot-aware, so the toolbar follows the selection) and draws the actions.
 */
internal class SelectionToolbarState : TextContextMenuProvider {
    var dataProvider by mutableStateOf<TextContextMenuDataProvider?>(null)
        private set
    /** Coordinates of the text field; the menu bounds are expressed relative to them. */
    var anchor: LayoutCoordinates? = null
    var mode by mutableStateOf(ToolbarMode.Main)
    /**
     * Where the toolbar sits after a style toggle (Bold / Italic / Underline). Restyling the text
     * changes the reported selection bounds, which made the toolbar jump. While pinned it stays put;
     * it is released as soon as the selection changes.
     */
    var pinnedRect by mutableStateOf<Rect?>(null)
    private var session: TextContextMenuSession? = null

    val isShown: Boolean get() = dataProvider != null

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        val done = CompletableDeferred<Unit>()
        session = object : TextContextMenuSession {
            override fun close() {
                done.complete(Unit)
            }
        }
        this.dataProvider = dataProvider
        try {
            done.await() // stays open until closed here or cancelled by the field (selection cleared)
        } finally {
            this.dataProvider = null
            session = null
            pinnedRect = null
            mode = ToolbarMode.Main
        }
    }

    /** Runs one of the field's own actions (copy / cut / paste / select all). */
    fun click(item: TextContextMenuItem) {
        session?.let { item.onClick(it) }
    }

    fun hide() {
        session?.close()
    }

    /** Pin the toolbar while a style toggle is applied so it does not jump with the selection. */
    fun pin(rect: Rect) {
        pinnedRect = rect
    }
}

@Composable
internal fun SelectionToolbar(
    state: SelectionToolbarState,
    editing: Boolean,
    boldOn: Boolean,
    italicOn: Boolean,
    underlineOn: Boolean,
    highlightArg: Int?,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onHighlight: (Color) -> Unit,
    onHighlightClear: () -> Unit,
    onClearFormatting: () -> Unit,
    onScripture: (() -> Unit)? = null,
) {
    val provider = state.dataProvider ?: return
    val anchor = state.anchor ?: return
    if (!anchor.isAttached) return

    val bounding = provider.boundingBox()
    val local = if (state.pinnedRect != null) state.pinnedRect!! else bounding
    // Convert from text-field coordinates to window coordinates.
    val rect = Rect(
        left = anchor.localToWindow(local.topLeft).x,
        top = anchor.localToWindow(local.topLeft).y,
        right = anchor.localToWindow(local.bottomRight).x,
        bottom = anchor.localToWindow(local.bottomRight).y,
    )

    // Release the pin once the selection has actually moved away from the pinned rect.
    LaunchedEffect(bounding) {
        val pinned = state.pinnedRect
        if (pinned != null && (pinned.left != bounding.left || pinned.top != bounding.top ||
                pinned.right != bounding.right || pinned.bottom != bounding.bottom)
        ) {
            state.pinnedRect = null
        }
    }

    fun keepOpen(action: () -> Unit) {
        state.pin(bounding)
        action()
    }

    val items = provider.data().components.filterIsInstance<TextContextMenuItem>()
    fun actionFor(key: Any): (() -> Unit)? =
        items.firstOrNull { it.key == key }?.let { item -> { state.click(item) } }
    val copyAction = actionFor(TextContextMenuKeys.CopyKey)
    val cutAction = actionFor(TextContextMenuKeys.CutKey)
    val pasteAction = actionFor(TextContextMenuKeys.PasteKey)
    val selectAllAction = actionFor(TextContextMenuKeys.SelectAllKey)
    val density = LocalDensity.current
    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val marginPx = with(density) { 8.dp.roundToPx() }
    val gapPx = with(density) { 12.dp.roundToPx() }
    val handlePx = with(density) { 28.dp.roundToPx() } // selection handles hang below the text
    // The toolbar must never be wider than the window (minus the popup margin), otherwise the
    // colour row (back + "no highlight" + 6 colours) is clipped on 360dp phones. Swatch size is
    // derived from the room we have: 8 items inside the toolbar's own padding/spacing budget.
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val maxToolbarWidth = windowWidthDp - 16.dp
    val swatch: Dp = ((maxToolbarWidth - 32.dp) / 8).coerceIn(34.dp, 44.dp)

    val position = remember(rect, topInsetPx, marginPx, gapPx, handlePx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val w = popupContentSize.width
                val h = popupContentSize.height
                val x = (rect.center.x - w / 2f).roundToInt()
                    .coerceIn(marginPx, maxOf(marginPx, windowSize.width - w - marginPx))
                val above = rect.top.roundToInt() - h - gapPx
                val y = if (above >= topInsetPx + marginPx) above
                else rect.bottom.roundToInt() + gapPx + handlePx
                return IntOffset(x, y.coerceIn(topInsetPx, maxOf(topInsetPx, windowSize.height - h - marginPx)))
            }
        }
    }

    // focusable = false keeps the keyboard and the selection exactly as they are while a button is tapped.
    // Popup is still required so the toolbar can float over the selection handles; width is capped so
    // HorizontalFloatingToolbar does not expand to the full window (the original Surface workaround).
    Popup(
        popupPositionProvider = position,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        val copy = copyAction
        val cut = cutAction
        val paste = pasteAction
        val selectAll = selectAllAction
        val hasSelection = copy != null || cut != null
        val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            containerColor = ToolbarBg,
            contentColor = ToolbarFg,
        )

        AnimatedContent(
            targetState = state.mode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "selectionToolbar",
            modifier = Modifier.widthIn(max = maxToolbarWidth),
        ) { mode ->
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier.wrapContentSize(),
                colors = toolbarColors,
            ) {
                when {
                    // Just a caret: the only useful actions are paste / select all.
                    !hasSelection -> {
                        if (paste != null) ToolbarButton(Icons.Default.ContentPaste, "Paste") { paste(); state.hide() }
                        if (selectAll != null) ToolbarButton(Icons.Default.SelectAll, "Select all") { selectAll() }
                    }

                    // All highlight colours; everything else is hidden so the swatches get the room.
                    mode == ToolbarMode.Colors && editing -> {
                        ToolbarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", size = swatch) { state.mode = ToolbarMode.Main }
                        Box(
                            modifier = Modifier
                                .size(swatch)
                                .clip(CircleShape)
                                .selectable(
                                    selected = highlightArg == null,
                                    role = Role.RadioButton,
                                    onClick = { onHighlightClear(); state.hide() }
                                )
                                .semantics { contentDescription = "No highlight" },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(swatch - 12.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, ToolbarFg.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FormatColorReset,
                                    contentDescription = null,
                                    tint = if (highlightArg == null) ToolbarAccent else ToolbarFg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HighlightColors.forEach { (c, label) ->
                            val selected = highlightArg != null && highlightColorOf(highlightArg) == c
                            Box(
                                modifier = Modifier
                                    .size(swatch)
                                    .clip(CircleShape)
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { onHighlight(c); state.hide() }
                                    )
                                    .semantics { contentDescription = "$label highlight" },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(swatch - 12.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(1.dp, ToolbarFg.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = InkBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Less common actions.
                    mode == ToolbarMode.More -> {
                        ToolbarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { state.mode = ToolbarMode.Main }
                        if (editing) ToolbarButton(Icons.Default.FormatItalic, "Italic", selected = italicOn) { keepOpen(onItalic) }
                        if (cut != null) ToolbarButton(Icons.Default.ContentCut, "Cut") { cut(); state.hide() }
                        if (paste != null) ToolbarButton(Icons.Default.ContentPaste, "Paste") { paste(); state.hide() }
                        if (selectAll != null) ToolbarButton(Icons.Default.SelectAll, "Select all") { selectAll() }
                        if (editing) ToolbarButton(Icons.Default.FormatClear, "Clear formatting") { onClearFormatting(); state.hide() }
                    }

                    else -> {
                        if (editing) {
                            // Fanned swatches like JW Library: yellow in front, the others peeking out
                            // behind it. One tap opens the full colour row.
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable(role = Role.Button) { state.mode = ToolbarMode.Colors }
                                    .semantics { contentDescription = "Highlight colours" }
                                    .padding(start = 10.dp, end = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy((-14).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val fan = HighlightColors.take(4)
                                fan.forEachIndexed { i, (c, _) ->
                                    Box(
                                        modifier = Modifier
                                            .zIndex((fan.size - i).toFloat()) // first colour on top
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                    )
                                }
                            }
                            ToolbarButton(Icons.Default.FormatBold, "Bold", selected = boldOn) { keepOpen(onBold) }
                            ToolbarButton(Icons.Default.FormatUnderlined, "Underline", selected = underlineOn) { keepOpen(onUnderline) }
                        }
                        if (copy != null) ToolbarButton(Icons.Default.ContentCopy, "Copy") { copy(); state.hide() }
                        if (onScripture != null) {
                            ToolbarButton(Icons.AutoMirrored.Filled.MenuBook, "Read scripture") { onScripture(); state.hide() }
                        }
                        ToolbarButton(Icons.Default.MoreVert, "More") { state.mode = ToolbarMode.More }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ToolbarButton(
    icon: ImageVector,
    label: String,
    /** null = plain action (copy, paste ...); true / false = a toggle (bold, italic ...). */
    selected: Boolean? = null,
    size: Dp = 44.dp,
    onClick: () -> Unit
) {
    if (selected == null) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size),
            colors = IconButtonDefaults.iconButtonColors(contentColor = ToolbarFg),
            shapes = IconButtonDefaults.shapes()
        ) { Icon(icon, contentDescription = label) }
    } else {
        IconToggleButton(
            checked = selected,
            onCheckedChange = { onClick() },
            modifier = Modifier.size(size),
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = ToolbarFg,
                checkedContainerColor = ToolbarChecked,
                checkedContentColor = ToolbarAccent
            ),
            shapes = IconButtonDefaults.toggleableShapes()
        ) { Icon(icon, contentDescription = label) }
    }
}
