package com.harsha.myapplication.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.Lexend
import com.harsha.myapplication.Purple
import com.harsha.myapplication.TextDark
import com.harsha.myapplication.TextMuted
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.ScriptureReference
import com.harsha.myapplication.scripture.allPassages
import com.harsha.myapplication.scripture.groupByChapterRuns
import com.harsha.myapplication.scripture.localizedKey
import com.harsha.myapplication.scripture.passagesKey
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.revealOnEnter
import kotlinx.coroutines.launch

/** One card: a title (shown only when there are several cards) and the verse text. */
@Immutable
private class PassageGroup(val title: String, val text: String)

/** Everything the sheet draws. Immutable, so the body skips recomposition while scrolling. */
@Immutable
private class SheetContent(
    val title: String,
    val scriptureCount: Int,
    val groups: List<PassageGroup>
)

/**
 * Modal bottom sheet showing the verse text for a reference (Material 3).
 *
 *  - drag handle (default), fixed header with title + count + close icon,
 *  - scrollable content in the middle, fixed full-width Close button at the bottom,
 *  - dividers appear only while content is scrolled under the header / above the action bar.
 *
 * Book names follow the active Bible's language. Consecutive passages of one chapter share a card.
 * Text always comes from the active Bible (BibleData.activeBible).
 *
 * Performance: the text is resolved once per (reference, Bible) through BibleData's chapter index and
 * cached with `remember`, so scrolling and recomposition never look verses up again.
 *
 * Motion:
 *  • ModalBottomSheet already provides its own spring-driven show/hide, drag-to-dismiss,
 *    and predictive-back handling in Material3 1.3+. We lean on that rather than fight it.
 *  • The two scroll-aware dividers fade in/out using Crossfade (no AnimatedVisibility) —
 *    AnimatedVisibility has a ColumnScope overload that's ambiguous inside nested Rows,
 *    and the compiler picks the wrong one. Crossfade has only a top-level version.
 *    The layout is also pixel-stable: the 1 dp slot is always reserved, only the line's
 *    alpha changes.
 *  • Close icon and Close button both shrink on press for the Expressive tactile feel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureBottomSheet(
    reference: ScriptureReference,
    onDismiss: () -> Unit
) {
    // Reading `version` makes the sheet refresh if the Bible is switched while it is open.
    val dataVersion = BibleData.version
    val content = remember(reference, dataVersion) {
        val bookName = BibleData.displayBookName(reference.book)
        val passages = reference.allPassages
        val groups = passages.groupByChapterRuns()
        SheetContent(
            title = reference.localizedKey,
            scriptureCount = passages.size,
            groups = groups.map { group ->
                PassageGroup(
                    title = passagesKey(bookName, group),
                    text = group.joinToString("\n\n") { BibleData.getPassage(it) }
                )
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Animate the sheet away first, then tell the caller. `hide()` is itself spring-driven
    // so the sheet slides down over ~300 ms; onDismiss only fires once it is fully hidden
    // (this is the recommended M3 pattern — dismiss callbacks should never preempt the animation).
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(content.title, content.scriptureCount, onClose = close)

            // Divider slot: the 1 dp space is always reserved, only the line's alpha
            // animates. Before, `if (canScrollBackward) Divider() else Spacer(1.dp)`
            // was a hard swap on scroll, causing a tiny layout jump.
            //
            // Crossfade instead of AnimatedVisibility: inside a Column that contains
            // Rows, AnimatedVisibility can resolve to the ColumnScope extension and
            // fail to compile. Crossfade has no scoped overload.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                Crossfade(
                    targetState = scrollState.canScrollBackward,
                    animationSpec = Motion.effectsFast,
                    label = "topDivider"
                ) { show ->
                    if (show) {
                        HorizontalDivider()
                    } else {
                        Spacer(Modifier.fillMaxWidth())
                    }
                }
            }

            SheetBody(
                content = content,
                scrollState = scrollState,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Same treatment as the top divider.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                Crossfade(
                    targetState = scrollState.canScrollForward,
                    animationSpec = Motion.effectsFast,
                    label = "bottomDivider"
                ) { show ->
                    if (show) {
                        HorizontalDivider()
                    } else {
                        Spacer(Modifier.fillMaxWidth())
                    }
                }
            }

            val closeBtnSrc = remember { MutableInteractionSource() }
            Button(
                onClick = close,
                interactionSource = closeBtnSrc,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .pressScale(closeBtnSrc),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text("Close", fontFamily = Lexend)
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, count: Int, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = Lexend,
                color = Purple
            )
            if (count > 1) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count scriptures",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Lexend,
                    color = TextMuted
                )
            }
        }
        // Small icon button: bigger press-scale delta than a regular button
        // (0.85 vs 0.96) because icon buttons have a smaller visual footprint.
        val iconSrc = remember { MutableInteractionSource() }
        IconButton(
            onClick = onClose,
            interactionSource = iconSrc,
            modifier = Modifier.pressScale(iconSrc, pressedScale = 0.85f),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun SheetBody(
    content: SheetContent,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val severalCards = content.groups.size > 1
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        content.groups.forEachIndexed { index, group ->
            if (index > 0) Spacer(modifier = Modifier.height(12.dp))
            if (severalCards) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    // Cards rise into place one after another once the sheet has settled.
                    modifier = Modifier
                        .fillMaxWidth()
                        .revealOnEnter(index = index + 1, stepMillis = 70, offsetY = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PassageText(group, showTitle = true)
                    }
                }
            } else {
                Box(Modifier.revealOnEnter(index = 1, offsetY = 12.dp)) {
                    Column { PassageText(group, showTitle = false) }
                }
            }
        }
    }
}

@Composable
private fun PassageText(group: PassageGroup, showTitle: Boolean) {
    if (showTitle) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = Lexend,
            color = Purple
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
    Text(
        text = group.text,
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = Lexend,
        color = TextDark,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f
    )
}