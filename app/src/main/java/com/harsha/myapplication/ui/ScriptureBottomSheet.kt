package com.harsha.myapplication.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.ScriptureDetector
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
    val groups: List<PassageGroup>,
    /** Text inserted into the note by "Add scripture text to notes" (reference + verses). */
    val quote: String = "",
    /** Null when the reference exists in the active Bible; otherwise a short explanation. */
    val errorMessage: String? = null
)

/**
 * Modal bottom sheet showing the verse text for a reference (Material 3).
 *
 * Styled like the Add-hours sheet: surfaceContainerLow container, 24dp side margins, a titleLarge
 * header with a bodyMedium subtitle, hairline dividers between sections, 48dp tonal action buttons
 * and a 56dp primary Close button. Colours, shapes and type all come from MaterialTheme.
 *
 *  - fixed header (title + subtitle),
 *  - scrollable passage cards in the middle,
 *  - fixed action area at the bottom (insert / remove quote, correct name, Close),
 *  - the dividers around the scrolling area fade in only while content is scrolled under them.
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
 *  • Buttons shrink on press for the Expressive tactile feel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureBottomSheet(
    reference: ScriptureReference,
    onDismiss: () -> Unit,
    /**
     * Called when the user taps "Correct to …" for a fuzzy-matched name.
     * [start]..[end] is the range in the note text to replace with [corrected].
     */
    onCorrectName: ((start: Int, end: Int, corrected: String) -> Unit)? = null,
    /** True when this reference's verse text is already quoted in the note. */
    isInNote: Boolean = false,
    /** "Add scripture text to notes": receives the quote text to insert after the reference. */
    onInsertQuote: ((reference: ScriptureReference, quote: String) -> Unit)? = null,
    /** "Remove scripture from notes": removes the quote previously inserted for [reference]. */
    onRemoveQuote: ((reference: ScriptureReference) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    // Reading `version` makes the sheet refresh if the Bible is switched while it is open.
    val dataVersion = BibleData.version
    val content = remember(reference, dataVersion) {
        val bookName = BibleData.displayBookName(reference.book)
        val passages = reference.allPassages
        val error = BibleData.validatePassageMessage(reference)
        if (error != null) {
            SheetContent(
                title = reference.localizedKey,
                scriptureCount = passages.size,
                groups = emptyList(),
                errorMessage = error
            )
        } else {
            val groups = passages.groupByChapterRuns().map { group ->
                PassageGroup(
                    title = passagesKey(bookName, group),
                    text = group.joinToString("\n\n") { BibleData.getPassage(it) }
                )
            }
            SheetContent(
                title = reference.localizedKey,
                scriptureCount = passages.size,
                groups = groups,
                quote = groups.joinToString("\n\n") { "“${it.text}” — ${it.title}" }
            )
        }
    }

    // Offer correction whenever the typed text is not already the proper form in the
    // active Bible's language. Covers fuzzy romanization ("2yuvhan"), typos, and
    // short aliases ("Jn", "Matt") so the note can be cleaned up to "2 John" / "యోహాను".
    val correctLabel = remember(reference, dataVersion) {
        val proper = reference.localizedKey
        if (proper.equals(reference.rawText, ignoreCase = true)) null else proper
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
        containerColor = colors.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(content.title, content.scriptureCount)

            // Divider slot: the 1 dp space is always reserved, only the line's alpha
            // animates. Before, `if (canScrollBackward) Divider() else Spacer(1.dp)`
            // was a hard swap on scroll, causing a tiny layout jump.
            //
            // Crossfade instead of AnimatedVisibility: inside a Column that contains
            // Rows, AnimatedVisibility can resolve to the ColumnScope extension and
            // fail to compile. Crossfade has no scoped overload.
            ScrollDivider(visible = scrollState.canScrollBackward, label = "topDivider")

            SheetBody(
                content = content,
                scrollState = scrollState,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Same treatment as the top divider.
            ScrollDivider(visible = scrollState.canScrollForward, label = "bottomDivider")

            // Action area: 12dp between buttons, 24dp side margins (same rhythm as the Add-hours sheet).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Insert / remove the verse text as a quote in the note.
                if (content.errorMessage == null && content.quote.isNotEmpty()) {
                    if (isInNote && onRemoveQuote != null) {
                        val removeSrc = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = {
                                onRemoveQuote(reference)
                                close()
                            },
                            interactionSource = removeSrc,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pressScale(removeSrc),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colors.errorContainer,
                                contentColor = colors.onErrorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(
                                "Remove scripture from notes",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    } else if (!isInNote && onInsertQuote != null) {
                        val insertSrc = remember { MutableInteractionSource() }
                        FilledTonalButton(
                            onClick = {
                                onInsertQuote(reference, content.quote)
                                close()
                            },
                            interactionSource = insertSrc,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pressScale(insertSrc)
                        ) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(
                                "Add scripture text to notes",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Fuzzy name correction: replaces "luka 25:1" with "Luke 25:1" / "లూకా 25:1"
                if (correctLabel != null && onCorrectName != null) {
                    val correctSrc = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            // Teach the matcher so the same spelling resolves faster next time.
                            ScriptureDetector.recordChoice(reference.rawText, reference.book)
                            onCorrectName(reference.startIndex, reference.endIndex, correctLabel)
                            close()
                        },
                        interactionSource = correctSrc,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .pressScale(correctSrc),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(
                            "Correct to $correctLabel",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }

                val closeBtnSrc = remember { MutableInteractionSource() }
                Button(
                    onClick = close,
                    interactionSource = closeBtnSrc,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .pressScale(closeBtnSrc)
                ) {
                    Text("Close", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** 1 dp slot that is always reserved; the hairline only fades in while content scrolls under it. */
@Composable
private fun ScrollDivider(visible: Boolean, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        Crossfade(
            targetState = visible,
            animationSpec = Motion.effectsFast,
            label = label
        ) { show ->
            if (show) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            } else {
                Spacer(Modifier.fillMaxWidth())
            }
        }
    }
}

/** Title + subtitle, like the Add-hours header (the Close button at the bottom dismisses the sheet). */
@Composable
private fun SheetHeader(title: String, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (count > 1) "$count scriptures" else "Scripture",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SheetBody(
    content: SheetContent,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val error = content.errorMessage
    if (error != null) {
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = colors.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(index = 1, offsetY = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Passage not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onErrorContainer
                    )
                    Text(
                        text = "Current Bible: ${BibleData.currentTranslationName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        return
    }

    val severalCards = content.groups.size > 1
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content.groups.forEachIndexed { index, group ->
            if (severalCards) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surfaceContainerHighest,
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
    val colors = MaterialTheme.colorScheme
    if (showTitle) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
    Text(
        text = group.text,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurface,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f
    )
}