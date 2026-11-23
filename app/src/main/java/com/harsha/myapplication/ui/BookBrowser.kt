package com.harsha.myapplication.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.harsha.myapplication.ui.theme.revealOnEnter
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.harsha.myapplication.Lexend
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.ScriptureReference
import com.harsha.myapplication.scripture.VerseItem
import com.harsha.myapplication.ui.components.predictiveBackTransform
import com.harsha.myapplication.ui.components.rememberPredictiveBackState
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.pressScaleClickable

// ---------------------------------------------------------------------------
// Hard-coded book data (66 books, canonical order)
// ---------------------------------------------------------------------------

/** Canonical English names, in Bible order. Index 0..38 = Hebrew-Aramaic, 39..65 = Greek. */
private val CanonicalBooks = listOf(
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
    "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
    "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
    "Malachi",
    "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians",
    "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
    "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter",
    "1 John", "2 John", "3 John", "Jude", "Revelation"
)

private const val FirstGreekIndex = 39

private val EnglishShort = listOf(
    "Gen", "Ex", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth",
    "1 Sam", "2 Sam", "1 Ki", "2 Ki", "1 Chr", "2 Chr", "Ezra",
    "Neh", "Esth", "Job", "Ps", "Prov", "Eccl", "Song",
    "Isa", "Jer", "Lam", "Ezek", "Dan", "Hos", "Joel", "Amos",
    "Obad", "Jonah", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech",
    "Mal",
    "Matt", "Mark", "Luke", "John", "Acts", "Rom", "1 Cor", "2 Cor",
    "Gal", "Eph", "Phil", "Col", "1 Thess", "2 Thess",
    "1 Tim", "2 Tim", "Titus", "Philem", "Heb", "Jas", "1 Pet", "2 Pet",
    "1 John", "2 John", "3 John", "Jude", "Rev"
)

private val TeluguShort = listOf(
    "ఆది", "నిర్గ", "లేవీ", "సం", "ద్వితీ", "యెహో", "న్యా", "రూతు",
    "1స", "2స", "1రా", "2రా", "1ది", "2ది", "ఎజ్రా",
    "నెహె", "ఎస్తే", "యోబు", "కీర్త", "సామె", "ప్రస", "పర",
    "యెష", "యిర్మీ", "విలా", "యెహె", "దాని", "హోషే", "యోవే", "ఆమో",
    "ఓబ", "యోనా", "మీకా", "నహూ", "హబ", "జెఫ", "హగ్గ", "జెక",
    "మలా",
    "మత్త", "మార్కు", "లూకా", "యోహా", "అపొ", "రోమా", "1కొ", "2కొ",
    "గల", "ఎఫె", "ఫిలి", "కొలొ", "1థె", "2థె",
    "1తి", "2తి", "తీతు", "ఫిలే", "హెబ్రీ", "యాకో", "1పే", "2పే",
    "1యో", "2యో", "3యో", "యూదా", "ప్రక"
)

private enum class LabelMode { English, Telugu, Full }

/** Decides how books are shown, based on the active Bible's name (set from the jwpub language). */
private fun labelModeFor(bibleName: String): LabelMode {
    val n = bibleName.trim()
    return when {
        n.startsWith("English", ignoreCase = true) || n.startsWith("Sample", ignoreCase = true) -> LabelMode.English
        n.startsWith("తెలుగు") || n.startsWith("Telugu", ignoreCase = true) -> LabelMode.Telugu
        else -> LabelMode.Full
    }
}

private data class BrowserBook(
    val index: Int?,            // position in CanonicalBooks, null if it couldn't be mapped
    val fullName: String,       // the active Bible's own name for the book
    val canonicalName: String   // used when building a ScriptureReference
)

/**
 * Maps the active Bible's books onto the canonical 66.
 * 1) match by canonicalBook / book name, 2) if the Bible has exactly 66 books, fall back to order.
 */
private fun buildBooks(verses: List<VerseItem>): List<BrowserBook> {
    val distinct = verses.distinctBy { it.book }
    val byOrder = distinct.size == CanonicalBooks.size
    return distinct.mapIndexed { i, v ->
        val idx = CanonicalBooks.indexOfFirst { it.equals(v.canonicalBook, ignoreCase = true) }.takeIf { it >= 0 }
            ?: CanonicalBooks.indexOfFirst { it.equals(v.book, ignoreCase = true) }.takeIf { it >= 0 }
            ?: if (byOrder) i else null
        BrowserBook(idx, v.book, v.canonicalBook)
    }
}

private enum class Level { Books, Picker }

/** One ticked verse. [order] = position of its book in the browser, used for sorting. */
private data class PickedVerse(val book: String, val order: Int, val chapter: Int, val verse: Int)

/**
 * Compact text for the note: sorted, consecutive verses merged, chapter repeated only when it changes.
 * Same book:  "Songs 1:1-5,7,3:7"      Different books are joined with "; ":  "Songs 1:1-5,7,3:7; John 3:16"
 */
private fun formatPicked(picked: List<PickedVerse>): String {
    val sorted = picked.sortedWith(compareBy({ it.order }, { it.chapter }, { it.verse }))
    return sorted.groupBy { it.book }.map { (book, inBook) ->
        val parts = mutableListOf<String>()
        inBook.groupBy { it.chapter }.forEach { (chapter, inChapter) ->
            val verses = inChapter.map { it.verse }.distinct().sorted()
            val runs = mutableListOf<String>()
            var i = 0
            while (i < verses.size) {
                var j = i
                while (j + 1 < verses.size && verses[j + 1] == verses[j] + 1) j++
                runs += if (j > i) "${verses[i]}-${verses[j]}" else "${verses[i]}"
                i = j + 1
            }
            runs.forEachIndexed { k, run -> parts += if (k == 0) "$chapter:$run" else run }
        }
        "$book ${parts.joinToString(",")}"
    }.joinToString("; ")
}

// ---------------------------------------------------------------------------
// Full-screen browser (Material 3 Expressive motion)
// ---------------------------------------------------------------------------

/**
 * Full-screen book / chapter / verse browser.
 *
 * Motion, compared with the previous version:
 *  • Books → Picker uses a shared-axis-X slide (200 ms spring, 30% parallax)
 *    so the grid "moves out of the way" and the split picker "comes in from
 *    the side" — the same hierarchical-navigation pattern Material 3 uses for
 *    Settings or list→detail.
 *  • Back gesture (picker level) shrinks/fades the whole surface live via
 *    predictive back, and cancelling springs it back.
 *  • Tapping a chapter slides the verse grid in from the direction of travel.
 *  • Verse selection animates container colour, border width, font weight —
 *    no state flips.
 *  • Every tappable has press-scale feedback.
 *  • SelectionBar springs up from the bottom edge when pick mode activates,
 *    springs back when it deactivates.
 *
 * @param onVersePicked when set, the browser is in "pick" mode: verses can be ticked (across chapters
 *   and books) and a bottom "Add" button hands back the text, e.g. "Songs 1:1-5,7,3:7; John 3:16"
 *   (or "సామెతలు 1:2" for a Telugu Bible). Consecutive verses are merged into a range.
 *   The caller is responsible for closing the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookBrowserDialog(
    onClose: () -> Unit,
    onVersePicked: ((String) -> Unit)? = null
) {
    // BibleData caches these per Bible (built once on a background thread).
    val dataVersion = BibleData.version
    val bibleName = BibleData.currentTranslationName

    val mode = remember(bibleName) { labelModeFor(bibleName) }
    val books = remember(dataVersion) { buildBooks(BibleData.firstVersePerBook()) }

    // book -> chapter -> verse numbers
    val grouped = remember(dataVersion) { BibleData.outline() }

    // Grid only when every book's position is known; otherwise fall back to the full-name list.
    val useGrid = mode != LabelMode.Full && books.isNotEmpty() && books.all { it.index != null }
    val orderedBooks = remember(books, useGrid) {
        if (useGrid) books.sortedBy { it.index } else books
    }

    var level by remember { mutableStateOf(Level.Books) }
    var selectedBook by remember { mutableStateOf<BrowserBook?>(null) }
    var selectedChapter by remember { mutableStateOf(1) }
    var tappedReference by remember { mutableStateOf<ScriptureReference?>(null) }

    // Multi-select (pick mode only). Survives moving between chapters and books.
    val isPick = onVersePicked != null
    val picked = remember { mutableStateListOf<PickedVerse>() }

    /**
     * Deliberately does NOT clear `selectedBook` when going back to Books — the
     * exiting Picker animation needs that reference for the whole slide-out.
     * `openBook()` overwrites it on the next book tap, and the dialog owns the
     * state anyway, so nothing leaks.
     */
    // The Dialog window has no enter/exit motion of its own, so the surface animates itself:
    // it scales/fades in on open and plays the exit BEFORE onClose() removes the Dialog.
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    fun requestClose() { visibleState.targetState = false }
    LaunchedEffect(visibleState.currentState, visibleState.isIdle) {
        if (visibleState.isIdle && !visibleState.currentState) onClose()
    }

    fun goBack() {
        when (level) {
            Level.Picker -> level = Level.Books
            Level.Books -> requestClose()
        }
    }

    fun openBook(book: BrowserBook) {
        selectedBook = book
        selectedChapter = grouped[book.fullName]?.keys?.firstOrNull() ?: 1
        level = Level.Picker
    }

    val colors = MaterialTheme.colorScheme

    // Predictive back: while inside a book, the gesture shrinks the entire dialog.
    // At the Books level, `enabled = false` lets the system close the Dialog normally.
    val backState = rememberPredictiveBackState(
        enabled = level == Level.Picker,
        onBack = { goBack() },
    )

    Dialog(
        onDismissRequest = { goBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(Motion.effectsDefault) +
                    scaleIn(spring(dampingRatio = 0.88f, stiffness = 380f), initialScale = 0.92f) +
                    slideInVertically(Motion.spatialDefault<IntOffset>()) { it / 14 },
            exit = fadeOut(Motion.effectsFast) +
                    scaleOut(tween(180), targetScale = 0.94f),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .predictiveBackTransform(backState, minScale = 0.94f, minAlpha = 0.75f),
                color = colors.surface,
            ) {
                Scaffold(
                    containerColor = colors.surface,
                    bottomBar = {
                        // SelectionBar rises from the bottom on pick activation; falls back on exit.
                        AnimatedVisibility(
                            visible = isPick && (level == Level.Picker || picked.isNotEmpty()),
                            enter = slideInVertically(Motion.spatialDefault<IntOffset>()) { it } +
                                    fadeIn(Motion.effectsFast),
                            exit = slideOutVertically(Motion.spatialDefault<IntOffset>()) { it } +
                                    fadeOut(Motion.effectsFast),
                        ) {
                            SelectionBar(
                                count = picked.size,
                                onClear = { picked.clear() },
                                onAdd = { onVersePicked?.invoke(formatPicked(picked)) }
                            )
                        }
                    },
                    topBar = {
                        // TopAppBar stays fixed; only its title changes. This is intentional:
                        // shared-axis moves the *content* between levels, not the chrome.
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (level) {
                                        Level.Books -> if (onVersePicked != null) "Add scripture" else "Books of the Bible"
                                        Level.Picker -> selectedBook?.fullName.orEmpty()
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = Lexend,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                if (level == Level.Picker) {
                                    IconButton(onClick = { goBack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to books")
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { requestClose() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            },
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colors.surface,
                                titleContentColor = colors.onSurface,
                                navigationIconContentColor = colors.onSurface,
                                actionIconContentColor = colors.onSurfaceVariant
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding).fillMaxSize()) {
                        // Shared-axis X: forward = slide in from right, back = slide in from left.
                        // 30% parallax (`it / 3`) keeps both layers visible during the swap,
                        // which is what M3's SharedAxisTransition does under the hood.
                        AnimatedContent(
                            targetState = level,
                            transitionSpec = {
                                val forward = targetState == Level.Picker
                                val enterFrom = if (forward) 1 else -1
                                val exitTo = if (forward) -1 else 1
                                (
                                        slideInHorizontally(Motion.spatialFast<IntOffset>()) {
                                            enterFrom * (it / 3)
                                        } + fadeIn(Motion.effectsFast)
                                        ).togetherWith(
                                        slideOutHorizontally(Motion.spatialFast<IntOffset>()) {
                                            exitTo * (it / 3)
                                        } + fadeOut(Motion.effectsFast)
                                    )
                            },
                            label = "bookBrowserLevel"
                        ) { targetLevel ->
                            when (targetLevel) {
                                Level.Books ->
                                    if (useGrid) {
                                        BookGrid(orderedBooks, mode, onBookClick = ::openBook)
                                    } else {
                                        BookList(orderedBooks, onBookClick = ::openBook)
                                    }

                                Level.Picker -> {
                                    val book = selectedBook
                                    val order = orderedBooks.indexOfFirst { it.fullName == book?.fullName }
                                    SplitPicker(
                                        chapters = grouped[book?.fullName].orEmpty(),
                                        selectedChapter = selectedChapter,
                                        onChapterSelected = { selectedChapter = it },
                                        multiSelect = isPick,
                                        pickedVerses = picked
                                            .filter { it.book == book?.fullName && it.chapter == selectedChapter }
                                            .map { it.verse }.toSet(),
                                        chaptersWithPicks = picked
                                            .filter { it.book == book?.fullName }
                                            .map { it.chapter }.toSet(),
                                        onVerseClick = { v ->
                                            val b = book ?: return@SplitPicker
                                            if (isPick) {
                                                val item = PickedVerse(b.fullName, order, selectedChapter, v)
                                                if (!picked.remove(item)) picked.add(item)
                                            } else {
                                                tappedReference = ScriptureReference(
                                                    book = b.canonicalName,
                                                    chapter = selectedChapter,
                                                    verse = v,
                                                    rawText = "${b.fullName} $selectedChapter:$v"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Verse text opens in the existing bottom sheet
    tappedReference?.let { ref ->
        ScriptureBottomSheet(reference = ref, onDismiss = { tappedReference = null })
    }
}

// ---------------------------------------------------------------------------
// Books
// ---------------------------------------------------------------------------

/** Original book colors (kept fixed, white text on top). */
private fun bookColor(index: Int): Color = when (index) {
    in 0..4 -> Color(0xFF3B3A4A)       // Law
    in 5..16 -> Color(0xFF6B5F8A)      // History
    in 17..21 -> Color(0xFF3B3A4A)     // Poetry
    in 22..38 -> Color(0xFF2E2D3A)     // Prophets
    in 39..43 -> Color(0xFF55506B)     // Gospels + Acts
    in 44..57 -> Color(0xFF4A4660)     // Letters
    else -> Color(0xFF3B3A4A)          // General letters + Revelation
}

@Composable
private fun BookGrid(
    books: List<BrowserBook>,
    mode: LabelMode,
    onBookClick: (BrowserBook) -> Unit
) {
    val hebrew = books.filter { (it.index ?: 0) < FirstGreekIndex }
    val greek = books.filter { (it.index ?: 0) >= FirstGreekIndex }
    val isTelugu = mode == LabelMode.Telugu

    val hebrewTitle = if (isTelugu) "హీబ్రూ-అరామిక్ లేఖనాలు" else "Hebrew-Aramaic Scriptures"
    val greekTitle = if (isTelugu) "క్రైస్తవ గ్రీకు లేఖనాలు" else "Christian Greek Scriptures"

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (hebrew.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(hebrewTitle) }
            itemsIndexed(hebrew) { i, b -> BookCell(b, mode, onBookClick, gridReveal(i)) }
        }
        if (greek.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(greekTitle) }
            itemsIndexed(greek) { i, b -> BookCell(b, mode, onBookClick, gridReveal(i)) }
        }
    }
}

/** Row-by-row cascade for the first screenful of the book grid (6 cells per row); later rows stay still. */
private fun gridReveal(i: Int): Modifier =
    if (i < 30) Modifier.revealOnEnter(index = i / 6, stepMillis = 35, maxSteps = 6, offsetY = 12.dp)
    else Modifier

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = Lexend,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun BookCell(
    book: BrowserBook,
    mode: LabelMode,
    onClick: (BrowserBook) -> Unit,
    modifier: Modifier = Modifier,
) {
    val idx = book.index ?: 0
    val label = when (mode) {
        LabelMode.English -> EnglishShort.getOrNull(idx)
        LabelMode.Telugu -> TeluguShort.getOrNull(idx)
        LabelMode.Full -> null
    } ?: book.fullName

    val src = remember { MutableInteractionSource() }

    Surface(
        onClick = { onClick(book) },
        interactionSource = src,
        shape = RoundedCornerShape(4.dp),
        color = bookColor(idx),
        contentColor = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressScale(src, pressedScale = 0.92f)   // grid cells shrink more than buttons
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Full-name list, used for every language other than English / Telugu. */
@Composable
private fun BookList(books: List<BrowserBook>, onBookClick: (BrowserBook) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(books) { book ->
            ListItem(
                headlineContent = {
                    Text(book.fullName, style = MaterialTheme.typography.bodyLarge, fontFamily = Lexend)
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                // pressScaleClickable wraps press-scale + ripple in one call.
                modifier = Modifier.pressScaleClickable(pressedScale = 0.98f) { onBookClick(book) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// ---------------------------------------------------------------------------
// Chapters (left) + verses (right)
// ---------------------------------------------------------------------------

/**
 * Left: scrollable chapter column. Right: scrollable verse grid for the selected chapter.
 * Each side scrolls independently; the verse grid slides in from the direction of travel
 * when the chapter changes.
 */
@Composable
private fun SplitPicker(
    chapters: Map<Int, List<Int>>,
    selectedChapter: Int,
    onChapterSelected: (Int) -> Unit,
    multiSelect: Boolean,
    pickedVerses: Set<Int>,
    chaptersWithPicks: Set<Int>,
    onVerseClick: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    if (chapters.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Nothing loaded here.",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = Lexend,
                color = colors.onSurfaceVariant
            )
        }
        return
    }

    val chapterNumbers = remember(chapters) { chapters.keys.toList() }
    val chapterListState = rememberLazyListState()

    // Keep the selected chapter visible on the left.
    LaunchedEffect(selectedChapter) {
        val i = chapterNumbers.indexOf(selectedChapter)
        if (i >= 0) chapterListState.animateScrollToItem(i + 1) // +1 for the header item
    }

    Row(Modifier.fillMaxSize()) {

        // ---- Chapters ----
        LazyColumn(
            state = chapterListState,
            modifier = Modifier
                .width(104.dp)
                .fillMaxHeight()
                .padding(start = 8.dp, end = 4.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    "Chapter",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = Lexend,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(chapterNumbers) { ch ->
                val selected = ch == selectedChapter
                val src = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onChapterSelected(ch) },
                    interactionSource = src,
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) colors.secondaryContainer else Color.Transparent,
                    contentColor = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pressScale(src, pressedScale = 0.94f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$ch",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = Lexend,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        // small dot: this chapter has ticked verses
                        if (multiSelect && ch in chaptersWithPicks) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 12.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                        }
                    }
                }
            }
        }

        VerticalDivider(color = colors.outlineVariant)

        // ---- Verses ----
        // AnimatedContent keyed on the chapter: forward chapter = slide in from right,
        // back = slide in from left. Each chapter gets a fresh scroll state automatically.
        AnimatedContent(
            targetState = selectedChapter,
            transitionSpec = {
                val forward = targetState > initialState
                val enterFrom = if (forward) 1 else -1
                val exitTo = if (forward) -1 else 1
                (
                        slideInHorizontally(Motion.spatialFast<IntOffset>()) { enterFrom * (it / 3) } +
                                fadeIn(Motion.effectsFast)
                        ).togetherWith(
                        slideOutHorizontally(Motion.spatialFast<IntOffset>()) { exitTo * (it / 3) } +
                                fadeOut(Motion.effectsFast)
                    )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = "chapterVerses"
        ) { chapter ->
            val verseNumbers = chapters[chapter].orEmpty()

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Chapter $chapter · ${verseNumbers.size} verses",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = Lexend,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(verseNumbers) { v ->
                    val picked = multiSelect && v in pickedVerses
                    val src = remember { MutableInteractionSource() }

                    // Animate selection rather than flipping.
                    val container by animateColorAsState(
                        targetValue = if (picked) colors.primaryContainer else colors.surfaceContainerHigh,
                        animationSpec = tween(200),
                        label = "verseContainer"
                    )
                    val content by animateColorAsState(
                        targetValue = if (picked) colors.onPrimaryContainer else colors.onSurface,
                        animationSpec = tween(200),
                        label = "verseContent"
                    )
                    val borderWidth by animateDpAsState(
                        targetValue = if (picked) 2.dp else 0.dp,
                        animationSpec = Motion.spatialFastDp,
                        label = "verseBorderWidth"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (picked) colors.primary else Color.Transparent,
                        animationSpec = tween(200),
                        label = "verseBorderColor"
                    )
                    val weightFloat by animateFloatAsState(
                        targetValue = if (picked) 700f else 400f,
                        animationSpec = Motion.effectsFast,
                        label = "verseWeight"
                    )

                    Surface(
                        onClick = { onVerseClick(v) },
                        interactionSource = src,
                        shape = MaterialTheme.shapes.medium,
                        color = container,
                        contentColor = content,
                        border = BorderStroke(borderWidth, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressScale(src, pressedScale = 0.92f)
                            .semantics { selected = picked }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "$v",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = Lexend,
                                fontWeight = FontWeight(weightFloat.toInt())
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom action bar for pick mode (M3 tonal surface): optional Clear, and a wide filled
 * "Add" button that carries the selected count. Entrance/exit is driven by the parent
 * AnimatedVisibility — this composable is only about layout.
 */
@Composable
private fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    onAdd: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (count > 0) {
                TextButton(onClick = onClear) {
                    Text("Clear", fontFamily = Lexend)
                }
            }
            Button(
                onClick = onAdd,
                enabled = count > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(
                    text = if (count > 0) "Add ($count)" else "Add",
                    fontFamily = Lexend,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen() {
    val dataVersion = BibleData.version
    val bibleName = BibleData.currentTranslationName

    val mode = remember(bibleName) { labelModeFor(bibleName) }
    val books = remember(dataVersion) { buildBooks(BibleData.firstVersePerBook()) }
    val grouped = remember(dataVersion) { BibleData.outline() }

    val useGrid = mode != LabelMode.Full && books.isNotEmpty() && books.all { it.index != null }
    val orderedBooks = remember(books, useGrid) {
        if (useGrid) books.sortedBy { it.index } else books
    }

    var level by remember { mutableStateOf(Level.Books) }
    var selectedBook by remember { mutableStateOf<BrowserBook?>(null) }
    var selectedChapter by remember { mutableStateOf(1) }
    var tappedReference by remember { mutableStateOf<ScriptureReference?>(null) }

    fun openBook(book: BrowserBook) {
        selectedBook = book
        selectedChapter = grouped[book.fullName]?.keys?.firstOrNull() ?: 1
        level = Level.Picker
    }

    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.surface,
    ) {
        Scaffold(
            containerColor = colors.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (level) {
                                Level.Books -> "Bible"
                                Level.Picker -> selectedBook?.fullName.orEmpty()
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = Lexend,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (level == Level.Picker) {
                            IconButton(onClick = { level = Level.Books }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to books")
                            }
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.onSurface,
                        navigationIconContentColor = colors.onSurface,
                        actionIconContentColor = colors.onSurfaceVariant
                    )
                )
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                AnimatedContent(
                    targetState = level,
                    transitionSpec = {
                        val forward = targetState == Level.Picker
                        val enterFrom = if (forward) 1 else -1
                        val exitTo = if (forward) -1 else 1
                        (
                                slideInHorizontally(Motion.spatialFast<IntOffset>()) {
                                    enterFrom * (it / 3)
                                } + fadeIn(Motion.effectsFast)
                                ).togetherWith(
                                slideOutHorizontally(Motion.spatialFast<IntOffset>()) {
                                    exitTo * (it / 3)
                                } + fadeOut(Motion.effectsFast)
                            )
                    },
                    label = "bibleReaderLevel"
                ) { targetLevel ->
                    when (targetLevel) {
                        Level.Books ->
                            if (useGrid) {
                                BookGrid(orderedBooks, mode, onBookClick = ::openBook)
                            } else {
                                BookList(orderedBooks, onBookClick = ::openBook)
                            }

                        Level.Picker -> {
                            val book = selectedBook
                            SplitPicker(
                                chapters = grouped[book?.fullName].orEmpty(),
                                selectedChapter = selectedChapter,
                                onChapterSelected = { selectedChapter = it },
                                multiSelect = false,
                                pickedVerses = emptySet(),
                                chaptersWithPicks = emptySet(),
                                onVerseClick = { v ->
                                    val b = book ?: return@SplitPicker
                                    tappedReference = ScriptureReference(
                                        book = b.canonicalName,
                                        chapter = selectedChapter,
                                        verse = v,
                                        rawText = "${b.fullName} $selectedChapter:$v"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    tappedReference?.let { ref ->
        ScriptureBottomSheet(reference = ref, onDismiss = { tappedReference = null })
    }
}