@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.bible

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import com.harsha.myapplication.ui.theme.pressScale
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.JwpubImporter
import com.harsha.myapplication.scripture.VerseItem
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.revealOnEnter
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import kotlinx.coroutines.launch

// ── Design tokens (same family as Home / Notes / Settings / People) ─────────
private val ScreenMargin = 16.dp
private val SectionGap = 24.dp
private val CardRadius = 28.dp          // = Settings GroupOuter
private val GroupInner = 4.dp
private val GroupGap = 2.dp

/** Demo catalogue for the "Available to download" section (nothing is really downloaded). */
private data class DemoBible(val name: String, val nativeName: String, val size: String)

private val demoBibles = listOf(
    DemoBible("Hindi", "हिन्दी", "4.2 MB"),
    DemoBible("Tamil", "தமிழ்", "4.6 MB"),
)

private enum class DownloadPhase { Idle, Downloading, Done }

private const val JW_BIBLE_URL = "https://www.jw.org/en/library/bible/"

/** One numbered step in the "How to download" guide sheet. */
private data class GuideStep(val title: String, val body: String)

private val downloadGuideSteps = listOf(
    GuideStep(
        "Open jw.org",
        "Tap Open jw.org below. The Bibles page opens in your browser."
    ),
    GuideStep(
        "Choose your language",
        "Use the language box near the top (type or pick, e.g. Telugu or Hindi)."
    ),
    GuideStep(
        "Pick a Bible",
        "Select a Bible from the list, such as the New World Translation."
    ),
    GuideStep(
        "Download the JWPUB file",
        "Tap the download icon next to the Bible and choose JWPUB. Not PDF or EPUB."
    ),
    GuideStep(
        "Import it here",
        "Come back to this screen, tap Import from file and select the .jwpub file from your Downloads folder."
    )
)

// ---------------------------------------------------------------------------
// Languages
// ---------------------------------------------------------------------------

/** A language the importer can name a Bible after. [name] is what gets stored in BibleData. */
private data class BibleLanguage(val name: String, val nativeName: String? = null) {
    /** "Telugu · తెలుగు" for non-Latin languages, plain name otherwise. */
    val label: String get() = if (nativeName != null) "$name · $nativeName" else name
}

// Telugu and Hindi first: they are the primary languages for this app.
private val bibleLanguages = listOf(
    BibleLanguage("English"),
    BibleLanguage("Telugu", "తెలుగు"),
    BibleLanguage("Hindi", "हिन्दी"),
    BibleLanguage("Spanish"),
    BibleLanguage("Portuguese"),
    BibleLanguage("French"),
    BibleLanguage("Italian"),
    BibleLanguage("German"),
    BibleLanguage("Dutch"),
    BibleLanguage("Polish"),
    BibleLanguage("Japanese"),
    BibleLanguage("Korean")
)

private fun nativeNameOf(language: String): String? =
    bibleLanguages.firstOrNull { it.name == language }?.nativeName

/** JW language suffix in the file name (nwt_E.jwpub -> E) mapped to a language name. */
private val jwLanguageByCode = mapOf(
    "E" to "English",
    "S" to "Spanish",
    "T" to "Portuguese",
    "F" to "French",
    "I" to "Italian",
    "X" to "German",
    "O" to "Dutch",
    "P" to "Polish",
    "J" to "Japanese",
    "KO" to "Korean",
    "H" to "Hindi",
    "HI" to "Hindi",
    "TE" to "Telugu",
    "TU" to "Telugu"
)

/**
 * Detects Telugu / Hindi from the verse text itself by counting characters in each Unicode
 * script block. This does not depend on the file name, so it works even if the file was renamed
 * or uses a code we do not know. Returns null for Latin / other scripts.
 */
private fun detectLanguageFromText(verses: List<VerseItem>): String? {
    var telugu = 0
    var devanagari = 0
    for (verse in verses.asSequence().take(300)) {
        for (ch in verse.text) {
            when (ch.code) {
                in 0x0C00..0x0C7F -> telugu++
                in 0x0900..0x097F -> devanagari++
            }
        }
    }
    val minChars = 30
    return when {
        telugu >= minChars && telugu >= devanagari -> "Telugu"
        devanagari >= minChars -> "Hindi"
        else -> null
    }
}

/** Fallback for Latin-script languages: the language code in the file name (nwt_E.jwpub). */
private fun detectLanguageFromFileName(context: Context, uri: Uri): String? {
    val fileName = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        ?: uri.lastPathSegment
        ?: return null

    val code = Regex("^[A-Za-z0-9]+_([A-Z]{1,3})(?![A-Za-z])")
        .find(fileName)
        ?.groupValues
        ?.get(1)
        ?: return null

    return jwLanguageByCode[code]
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun ImportBibleScreen(
    onBack: () -> Unit = {},
    onImportSuccess: (String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var importing by remember { mutableStateOf(false) }

    var refreshKey by remember { mutableIntStateOf(0) }
    val dataVersion = BibleData.version
    val importedBibles = remember(refreshKey, dataVersion) { BibleData.importedBibles }
    val activeName = remember(refreshKey, dataVersion) { BibleData.currentTranslationName }

    // Play the exit animation before actually removing the row from storage.
    var removing by remember { mutableStateOf<String?>(null) }

    // Bible the user tapped delete on; non-null while the confirmation dialog is showing.
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showGuide by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val openJwOrg: () -> Unit = {
        try {
            uriHandler.openUri(JW_BIBLE_URL)
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("No browser found to open jw.org")
            }
        }
    }

    if (showGuide) {
        DownloadGuideSheet(
            onDismiss = { showGuide = false },
            onOpenSite = {
                showGuide = false
                openJwOrg()
            }
        )
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        importing = true

        scope.launch {
            val result = JwpubImporter.importFromUri(context, uri)
            if (result.success) {
                // Name the Bible by language: verse-text script (Telugu / Hindi) first,
                // then the file-name code, then the publication title.
                val language = detectLanguageFromText(result.verses)
                    ?: detectLanguageFromFileName(context, uri)
                    ?: result.translationTitle.ifBlank { "Imported Bible" }

                BibleData.loadVerses(result.verses, language)
                importing = false
                refreshKey++
                snackbarHostState.currentSnackbarData?.dismiss()
                launch { snackbarHostState.showSnackbar("$language Bible imported") }
                onImportSuccess(language)
            } else {
                importing = false
                snackbarHostState.showSnackbar(result.message.ifBlank { "Import failed" })
            }
        }
    }

    // ---------------------------------------------------------- delete confirmation
    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error) },
            title = { Text("Remove $name?", style = typography.headlineSmallEmphasized) },
            text = {
                Text(
                    "It will be deleted from this device.",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        removing = name
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("Remove", style = typography.labelLargeEmphasized) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", style = typography.labelLarge)
                }
            },
            shape = MaterialTheme.shapes.extraLargeIncreased,
            containerColor = colors.surfaceContainerHigh
        )
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Import Bible",
                navigationIcon = {
                    StudyBarIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            // ---------------------------------------------------------- import card
            Box(Modifier.revealOnEnter(index = 0, playOnceKey = "import-bible-card")) {
                ImportCard(
                    importing = importing,
                    onClick = { if (!importing) filePicker.launch("*/*") }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---------------------------------------------------------- download help
            Box(Modifier.revealOnEnter(index = 1, playOnceKey = "import-bible-help")) {
                DownloadHelpButtons(
                    onGuideClick = { showGuide = true },
                    onOpenSiteClick = openJwOrg
                )
            }

            Spacer(Modifier.height(SectionGap))

            // ---------------------------------------------------------- available to download (demo)
            // Phase lives in remember (not saved): leaving and reopening the screen resets to the
            // default un-downloaded view, and nothing here touches the imported list.
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "import-bible-available")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Available to download", Modifier.weight(1f))
                    Text(
                        "Demo",
                        style = typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    demoBibles.forEachIndexed { index, bible ->
                        DownloadableBibleRow(
                            bible = bible,
                            index = index,
                            count = demoBibles.size,
                            onFinished = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar("${bible.name} Bible downloaded")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ---------------------------------------------------------- imported list
            if (importedBibles.isNotEmpty()) {
                Column(Modifier.revealOnEnter(index = 3, playOnceKey = "import-bible-list")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("Imported Bibles", Modifier.weight(1f))
                        Text(
                            "${importedBibles.size} Bible${if (importedBibles.size == 1) "" else "s"}",
                            style = typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    Column(Modifier.fillMaxWidth()) {
                        importedBibles.forEachIndexed { index, bible ->
                            key(bible.name) {
                                ImportedBibleRow(
                                    name = bible.name,
                                    verseCount = bible.verses.size,
                                    isActive = bible.name == activeName,
                                    removing = removing == bible.name,
                                    shape = groupShape(index, importedBibles.size),
                                    onClick = {
                                        BibleData.setActiveBible(bible.name)
                                        refreshKey++
                                    },
                                    onDeleteClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        pendingDelete = bible.name
                                    },
                                    onRemoved = {
                                        val removedName = bible.name
                                        BibleData.removeBible(removedName)
                                        removing = null
                                        refreshKey++
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                "$removedName Bible removed",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (!importing) {
                EmptyBibles()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Components
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp)
    )
}

/** Grouped-list shapes: big outer corners, small joins between rows. */
private fun groupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) CardRadius else GroupInner
    val bottom = if (index == count - 1) CardRadius else GroupInner
    return RoundedCornerShape(
        topStart = top, topEnd = top,
        bottomStart = bottom, bottomEnd = bottom
    )
}

/** Same, but the joined corners spring open while pressed (Expressive shape morph). */
@Composable
private fun rememberGroupShape(index: Int, count: Int, innerTarget: Dp): Shape {
    val inner by animateDpAsState(innerTarget, Motion.spatialFastDp, label = "groupInner")
    val safe = inner.coerceAtLeast(0.dp)
    val top = if (index == 0) CardRadius else safe
    val bottom = if (index == count - 1) CardRadius else safe
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Expressive icon tile: a Material shape instead of a plain rounded square. */
@Composable
private fun ShapeTile(
    shape: Shape,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    rotation: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .rotate(rotation)
                .clip(shape)
                .background(container)
        )
        content()
    }
}

/** Card with an icon, a short description and the import button underneath. */
@Composable
private fun ImportCard(importing: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val tileShape = MaterialShapes.Clover4Leaf.toShape()
    val spin by rememberInfiniteTransition(label = "importSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart),
        label = "importSpinAngle"
    )

    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShapeTile(
                    shape = tileShape,
                    container = colors.secondaryContainer,
                    size = 52.dp,
                    rotation = if (importing) spin else 0f
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = colors.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Crossfade(
                    targetState = importing,
                    modifier = Modifier.weight(1f),
                    label = "importText"
                ) { busy ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (busy) "Importing…" else "Import a Bible",
                            style = typography.titleMediumEmphasized,
                            color = colors.onSurface
                        )
                        Text(
                            if (busy) "This may take a moment" else "Choose a .jwpub file from your device",
                            style = typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onClick,
                enabled = !importing,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    if (importing) "Importing…" else "Import from file",
                    style = typography.labelLargeEmphasized
                )
            }

            AnimatedVisibility(
                visible = importing,
                enter = fadeIn(Motion.effectsFast) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>())
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

/** Two side-by-side secondary actions that live outside the import card. */
@Composable
private fun DownloadHelpButtons(
    onGuideClick: () -> Unit,
    onOpenSiteClick: () -> Unit
) {
    val typography = MaterialTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onGuideClick,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Guide", style = typography.labelLargeEmphasized, maxLines = 1)
        }
        FilledTonalButton(
            onClick = onOpenSiteClick,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Open jw.org", style = typography.labelLargeEmphasized, maxLines = 1)
        }
    }
}

/**
 * Demo download row. Idle: download icon. Tap: ring fills with a percentage while the tile
 * spins. Done: check mark replaces the icon and the row says "Downloaded". The phase is plain
 * `remember`, so it is NOT stored and NOT added to Imported Bibles; reopening the screen shows
 * the default un-downloaded view again.
 */
@Composable
private fun DownloadableBibleRow(
    bible: DemoBible,
    index: Int,
    count: Int,
    onFinished: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(DownloadPhase.Idle) }
    val progress = remember { Animatable(0f) }

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = rememberGroupShape(index, count, if (pressed) 20.dp else GroupInner)

    val tileShape = MaterialShapes.Cookie9Sided.toShape()
    val spin by rememberInfiniteTransition(label = "dlSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "dlSpinAngle"
    )
    val tileColor by animateColorAsState(
        if (phase == DownloadPhase.Done) colors.primary else colors.secondaryContainer,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "dlTile"
    )
    val tileContent by animateColorAsState(
        if (phase == DownloadPhase.Done) colors.onPrimary else colors.onSecondaryContainer,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "dlTileContent"
    )

    fun start() {
        if (phase != DownloadPhase.Idle) return
        phase = DownloadPhase.Downloading
        scope.launch {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2800, easing = FastOutSlowInEasing))
            phase = DownloadPhase.Done
            onFinished()
        }
    }

    val percent = (progress.value * 100).toInt()

    Surface(
        onClick = ::start,
        enabled = phase == DownloadPhase.Idle,
        interactionSource = source,
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.97f)
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(
                        bible.nativeName,
                        style = typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        when (phase) {
                            DownloadPhase.Idle -> "${bible.name} · ${bible.size}"
                            DownloadPhase.Downloading -> "Downloading… $percent%"
                            DownloadPhase.Done -> "${bible.name} · Downloaded"
                        },
                        style = typography.bodyMedium,
                        color = if (phase == DownloadPhase.Done) colors.primary else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    ShapeTile(
                        shape = tileShape,
                        container = tileColor,
                        size = 44.dp,
                        rotation = if (phase == DownloadPhase.Downloading) spin else 0f
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = tileContent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                trailingContent = {
                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = {
                            (fadeIn(Motion.effectsDefault) + scaleIn(Motion.spatialFast, initialScale = 0.4f)) togetherWith
                                    (fadeOut(Motion.effectsFast) + scaleOut(targetScale = 0.4f))
                        },
                        label = "dlTrailing"
                    ) { p ->
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            when (p) {
                                DownloadPhase.Idle -> FilledTonalIconButton(onClick = ::start) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Download ${bible.name} Bible"
                                    )
                                }

                                DownloadPhase.Downloading -> {
                                    CircularProgressIndicator(
                                        progress = { progress.value },
                                        modifier = Modifier.size(44.dp),
                                        strokeWidth = 4.dp,
                                        trackColor = colors.secondaryContainer
                                    )
                                    Text(
                                        "$percent",
                                        style = typography.labelSmallEmphasized,
                                        color = colors.onSurface
                                    )
                                }

                                // Downloaded: the download icon is simply gone, replaced by a check.
                                DownloadPhase.Done -> Surface(
                                    shape = CircleShape,
                                    color = colors.primary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Downloaded",
                                            tint = colors.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(
                visible = phase == DownloadPhase.Downloading,
                enter = fadeIn(Motion.effectsFast) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>())
            ) {
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp)
                )
            }
        }
    }
}

/** M3 modal bottom sheet explaining how to get a .jwpub Bible from jw.org. */
@Composable
private fun DownloadGuideSheet(
    onDismiss: () -> Unit,
    onOpenSite: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val badge = MaterialShapes.Cookie6Sided.toShape()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "How to download a Bible",
                style = typography.titleLargeEmphasized,
                color = colors.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Get the free .jwpub file from jw.org, then import it here.",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            downloadGuideSteps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(badge)
                                .background(colors.secondaryContainer)
                        )
                        Text(
                            "${index + 1}",
                            style = typography.labelLargeEmphasized,
                            color = colors.onSecondaryContainer
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(step.title, style = typography.titleSmallEmphasized, color = colors.onSurface)
                        Text(step.body, style = typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            FilledTonalButton(
                onClick = onOpenSite,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Open jw.org", style = typography.labelLargeEmphasized)
            }
        }
    }
}

/** One row in the "Imported Bibles" list; handles its own enter / exit animation. */
@Composable
private fun ImportedBibleRow(
    name: String,
    verseCount: Int,
    isActive: Boolean,
    removing: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoved: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(removing) { if (removing) visible.targetState = false }
    LaunchedEffect(visible.currentState, visible.isIdle) {
        if (visible.isIdle && !visible.currentState) onRemoved()
    }

    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
        exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>())
    ) {
        val native = nativeNameOf(name)
        val verses = "%,d verses".format(verseCount)

        Surface(
            onClick = onClick,
            shape = shape,
            color = colors.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GroupGap)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        native ?: name,
                        style = typography.bodyLarge,
                        fontFamily = Lexend,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        if (native != null) "$name · $verses" else verses,
                        style = typography.bodyMedium,
                        fontFamily = Lexend,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = colors.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ActiveMark(selected = isActive)
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove $name",
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Radio-style "active Bible" indicator, identical to the one in Settings. */
@Composable
private fun ActiveMark(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        if (selected) colors.primary else Color.Transparent,
        animationSpec = tween(150),
        label = "markFill"
    )
    val ring by animateColorAsState(
        if (selected) colors.primary else colors.outline,
        animationSpec = tween(150),
        label = "markRing"
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(fill)
            .border(width = 2.dp, color = ring, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(tween(150)) + fadeIn(tween(120)),
            exit = scaleOut(tween(120)) + fadeOut(tween(120))
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Active",
                tint = colors.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Empty state, same look as the Notes / People empty states. */
@Composable
private fun EmptyBibles() {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = colors.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No Bibles imported yet",
            style = typography.titleMedium,
            fontFamily = Lexend,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick a .jwpub file above to add one.",
            style = typography.bodyMedium,
            fontFamily = Lexend,
            color = colors.onSurfaceVariant
        )
    }
}