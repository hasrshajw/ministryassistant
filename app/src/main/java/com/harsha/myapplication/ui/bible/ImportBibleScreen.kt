@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.bible

import com.harsha.myapplication.telemetry.CrashReporter
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.core.content.ContextCompat
import com.harsha.myapplication.scripture.BibleDownloads
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
import androidx.compose.material.icons.outlined.Book
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

// The downloadable Bibles (English, Telugu) live in BibleDownloads.catalog. The download and
// import run there, outside this screen, so they continue when the user leaves it.
private enum class DownloadPhase { Idle, Downloading, Importing, Failed, Done }

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

    // Catalog Bibles (English, Telugu) are managed inside "Available to download"; the
    // "Imported Bibles" list only shows Bibles the user imported from a file.
    val customBibles = remember(importedBibles) {
        importedBibles.filter { bible -> BibleDownloads.catalog.none { it.language == bible.name } }
    }

    // Play the exit animation before actually removing the row from storage.
    var removing by remember { mutableStateOf<String?>(null) }

    // Bible the user tapped delete on; non-null while the confirmation dialog is showing.
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Messages from background downloads ("English Bible imported", failures) while this screen is open.
    LaunchedEffect(Unit) {
        BibleDownloads.events.collect { message ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            refreshKey++
            snackbarHostState.currentSnackbarData?.dismiss()
            launch { snackbarHostState.showSnackbar(message) }
        }
    }

    // Android 13+: ask once so the progress notification can be shown. The download itself
    // does not depend on the answer.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val startDownload: (BibleDownloads.Source) -> Unit = { source ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        BibleDownloads.start(context, source)
    }

    var showGuide by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val openJwOrg: () -> Unit = {
        try {
            uriHandler.openUri(JW_BIBLE_URL)
        } catch (e: Exception) {
            CrashReporter.record("ImportBibleScreen", e)
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

    // Shared by both pickers below.
    val onFilePicked: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            importing = true

            scope.launch {
                // The importer reports its own failures, but anything unexpected (an OutOfMemoryError on
                // a low-RAM phone, a provider that dies mid-read) must end as a message, not a crash.
                val result = try {
                    JwpubImporter.importFromUri(context, uri)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    importing = false
                    throw e
                } catch (e: Throwable) {
                    CrashReporter.record("ImportBibleScreen", e, "Import crashed")
                    JwpubImporter.ImportResult(
                        success = false,
                        message = if (e is OutOfMemoryError) {
                            "Not enough free memory to import this Bible. Close other apps and try again."
                        } else {
                            "Import failed"
                        }
                    )
                }
                importing = false

                if (result.success) {
                    try {
                        // Name the Bible by language: verse-text script (Telugu / Hindi) first,
                        // then the file-name code, then the publication title.
                        val language = detectLanguageFromText(result.verses)
                            ?: detectLanguageFromFileName(context, uri)
                            ?: result.translationTitle.ifBlank { "Imported Bible" }

                        BibleData.loadVerses(result.verses, language)
                        refreshKey++
                        snackbarHostState.currentSnackbarData?.dismiss()
                        launch { snackbarHostState.showSnackbar("$language Bible imported") }
                        onImportSuccess(language)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        CrashReporter.record("ImportBibleScreen", e, "Could not save imported Bible")
                        snackbarHostState.showSnackbar("Could not save this Bible")
                    }
                } else {
                    snackbarHostState.showSnackbar(result.message.ifBlank { "Import failed" })
                }
            }
        }
    }

    // GetContent is the usual picker. A few devices have no handler for it (it throws
    // ActivityNotFoundException) but do have the system document picker, so that is the fallback.
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = onFilePicked
    )
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = onFilePicked
    )
    val launchFilePicker: () -> Unit = {
        try {
            filePicker.launch("*/*")
        } catch (first: Throwable) {
            CrashReporter.record("ImportBibleScreen", first, "GetContent picker unavailable")
            try {
                documentPicker.launch(arrayOf("*/*"))
            } catch (second: Throwable) {
                CrashReporter.record("ImportBibleScreen", second, "No file picker on this device")
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("No file manager found. Use \"Available to download\" instead.")
                }
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
                        if (BibleDownloads.catalog.any { it.language == name }) {
                            // Catalog rows have no exit animation: remove right away.
                            BibleData.removeBible(name)
                            refreshKey++
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    "$name Bible removed",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            removing = name
                        }
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
                    onClick = { if (!importing) launchFilePicker() }
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

            // ---------------------------------------------------------- available to download
            // Real downloads. State lives in BibleDownloads (not in this screen), so progress is
            // still there when you come back, and a finished download is imported automatically.
            Column(Modifier.revealOnEnter(index = 2, playOnceKey = "import-bible-available")) {
                SectionHeader("Available to download")
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    BibleDownloads.catalog.forEachIndexed { index, source ->
                        DownloadableBibleRow(
                            source = source,
                            imported = importedBibles.any { it.name == source.language },
                            isActive = activeName == source.language,
                            state = BibleDownloads.states[source.id],
                            index = index,
                            count = BibleDownloads.catalog.size,
                            onStart = { startDownload(source) },
                            onSelect = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                BibleData.setActiveBible(source.language)
                                refreshKey++
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pendingDelete = source.language
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            // ---------------------------------------------------------- imported list
            if (customBibles.isNotEmpty()) {
                Column(Modifier.revealOnEnter(index = 3, playOnceKey = "import-bible-list")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("Imported Bibles", Modifier.weight(1f))
                        Text(
                            "${customBibles.size} Bible${if (customBibles.size == 1) "" else "s"}",
                            style = typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    Column(Modifier.fillMaxWidth()) {
                        customBibles.forEachIndexed { index, bible ->
                            key(bible.name) {
                                ImportedBibleRow(
                                    name = bible.name,
                                    verseCount = bible.verses.size,
                                    isActive = bible.name == activeName,
                                    removing = removing == bible.name,
                                    index = index,
                                    count = customBibles.size,
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
            } else if (!importing && importedBibles.isEmpty()) {
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
 * Download row for a real Bible. Idle: download button. Tap: the download starts in
 * [BibleDownloads] (a foreground service keeps it alive), the ring shows the percentage, a wavy
 * line fills underneath and the tile spins. Then "Importing…" and the Bible is added to Imported
 * Bibles automatically, even if the user has left this screen. Done: check mark.
 * Failed: retry button.
 */
@Composable
private fun DownloadableBibleRow(
    source: BibleDownloads.Source,
    imported: Boolean,
    isActive: Boolean,
    state: BibleDownloads.State?,
    index: Int,
    count: Int,
    onStart: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val phase = when {
        imported -> DownloadPhase.Done
        state is BibleDownloads.State.Downloading -> DownloadPhase.Downloading
        state is BibleDownloads.State.Importing -> DownloadPhase.Importing
        state is BibleDownloads.State.Failed -> DownloadPhase.Failed
        else -> DownloadPhase.Idle
    }
    val busy = phase == DownloadPhase.Downloading || phase == DownloadPhase.Importing
    val rawProgress = (state as? BibleDownloads.State.Downloading)?.progress ?: -1f
    val determinate = phase == DownloadPhase.Downloading && rawProgress >= 0f
    val progress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = tween(250, easing = LinearEasing),
        label = "dlProgress"
    )
    val percent = (progress * 100).toInt()

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberGroupShape(index, count, if (pressed) 20.dp else GroupInner)

    val tileShape = MaterialShapes.Cookie9Sided.toShape()
    val spin by rememberInfiniteTransition(label = "dlSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "dlSpinAngle"
    )
    val highlighted = phase == DownloadPhase.Done && isActive
    val tileColor by animateColorAsState(
        if (highlighted) colors.primary else colors.secondaryContainer,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "dlTile"
    )
    val tileContent by animateColorAsState(
        if (highlighted) colors.onPrimary else colors.onSecondaryContainer,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "dlTileContent"
    )

    Surface(
        // Idle / Failed: start the download. Downloaded: tapping the row selects the Bible.
        onClick = if (phase == DownloadPhase.Done) onSelect else onStart,
        enabled = !busy,
        interactionSource = interaction,
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.97f)
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(
                        source.nativeName ?: source.language,
                        style = typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        when (phase) {
                            DownloadPhase.Idle -> "${source.language} · ${source.subtitle}"
                            DownloadPhase.Downloading ->
                                if (determinate) "Downloading… $percent%" else "Downloading…"
                            DownloadPhase.Importing -> "Importing… this may take a moment"
                            DownloadPhase.Failed -> "Failed · tap to retry"
                            DownloadPhase.Done ->
                                if (isActive) "${source.language} · Active" else "${source.language} · Downloaded"
                        },
                        style = typography.bodyMedium,
                        color = when (phase) {
                            DownloadPhase.Done -> if (isActive) colors.primary else colors.onSurfaceVariant
                            DownloadPhase.Failed -> colors.error
                            else -> colors.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    ShapeTile(
                        shape = tileShape,
                        container = tileColor,
                        size = 44.dp,
                        rotation = if (busy) spin else 0f
                    ) {
                        Icon(
                            Icons.Outlined.Book,
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
                        Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) {
                            when (p) {
                                // Same "Get" button as the setup screen.
                                DownloadPhase.Idle -> FilledTonalButton(
                                    onClick = onStart,
                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Download ${source.language} Bible",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Get")
                                }

                                DownloadPhase.Downloading -> {
                                    if (determinate) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(44.dp),
                                            strokeWidth = 4.dp,
                                            trackColor = colors.secondaryContainer
                                        )
                                        Text(
                                            "$percent",
                                            style = typography.labelSmallEmphasized,
                                            color = colors.onSurface
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(44.dp),
                                            strokeWidth = 4.dp,
                                            trackColor = colors.secondaryContainer
                                        )
                                    }
                                }

                                DownloadPhase.Importing -> CircularProgressIndicator(
                                    modifier = Modifier.size(44.dp),
                                    strokeWidth = 4.dp,
                                    trackColor = colors.secondaryContainer
                                )

                                DownloadPhase.Failed -> FilledTonalButton(
                                    onClick = onStart,
                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                ) {
                                    Text("Retry")
                                }

                                // Downloaded: select + delete buttons replace the download button.
                                DownloadPhase.Done -> BibleRowActions(
                                    name = source.language,
                                    isActive = isActive,
                                    onSelect = onSelect,
                                    onDelete = onDelete
                                )
                            }
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(
                visible = busy,
                enter = fadeIn(Motion.effectsFast) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>())
            ) {
                // Expressive wavy line: determinate while downloading, indeterminate while importing.
                val barModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp)
                if (determinate) {
                    LinearWavyProgressIndicator(progress = { progress }, modifier = barModifier)
                } else {
                    LinearWavyProgressIndicator(modifier = barModifier)
                }
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
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoved: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = rememberGroupShape(index, count, if (pressed) 20.dp else GroupInner)

    val tileShape = MaterialShapes.Cookie9Sided.toShape()
    val tileColor by animateColorAsState(
        if (isActive) colors.primary else colors.secondaryContainer,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "ibTile"
    )
    val tileContent by animateColorAsState(
        if (isActive) colors.onPrimary else colors.onSecondaryContainer,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1600f),
        label = "ibTileContent"
    )

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
            interactionSource = interaction,
            shape = shape,
            color = colors.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GroupGap)
                .pressScale(interaction, pressedScale = 0.97f)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        native ?: name,
                        style = typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        buildString {
                            if (isActive) append("Active · ")
                            if (native != null) append("$name · ")
                            append(verses)
                        },
                        style = typography.bodyMedium,
                        color = if (isActive) colors.primary else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    ShapeTile(
                        shape = tileShape,
                        container = tileColor,
                        size = 44.dp
                    ) {
                        Icon(
                            Icons.Outlined.Book,
                            contentDescription = null,
                            tint = tileContent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                trailingContent = {
                    BibleRowActions(
                        name = name,
                        isActive = isActive,
                        onSelect = onClick,
                        onDelete = onDeleteClick
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Select + delete buttons shared by downloaded and imported Bibles.
 * Select is an expressive toggle: tonal while unselected, filled primary with a check when active.
 * Delete is a tonal error-container button. Both morph their shape while pressed.
 */
@Composable
private fun BibleRowActions(
    name: String,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconToggleButton(
            checked = isActive,
            onCheckedChange = { if (!isActive) onSelect() },
            shapes = IconButtonDefaults.toggleableShapes(),
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = colors.surfaceContainerHighest,
                contentColor = colors.onSurfaceVariant,
                checkedContainerColor = colors.primary,
                checkedContentColor = colors.onPrimary
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = if (isActive) "$name is active" else "Select $name",
                modifier = Modifier.size(20.dp)
            )
        }
        FilledTonalIconButton(
            onClick = onDelete,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = colors.errorContainer,
                contentColor = colors.onErrorContainer
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove $name",
                modifier = Modifier.size(20.dp)
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
