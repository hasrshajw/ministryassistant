package com.harsha.myapplication.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.*
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.JwpubImporter
import com.harsha.myapplication.scripture.VerseItem
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScaleClickable
import kotlinx.coroutines.launch

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

private fun TextStyle.lexend(weight: FontWeight? = null): TextStyle =
    copy(fontFamily = Lexend, fontWeight = weight ?: fontWeight)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBibleScreen(
    onBack: () -> Unit = {},
    onImportSuccess: (String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

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
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error)
            },
            title = {
                Text("Remove $name?", style = type.headlineSmall.lexend())
            },
            text = {
                Text(
                    "It will be deleted from this device.",
                    style = type.bodyMedium.lexend(),
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
                ) {
                    Text("Remove", style = type.labelLarge.lexend(FontWeight.SemiBold))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", style = type.labelLarge.lexend())
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Import Bible",
                        style = type.titleLarge.lexend(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // The parent screen already applies the system-bar insets (same as SettingsScreen).
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                )
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ---------------------------------------------------------- import card
            ImportCard(
                importing = importing,
                onClick = { if (!importing) filePicker.launch("*/*") }
            )

            Spacer(Modifier.height(24.dp))

            // ---------------------------------------------------------- imported list
            if (importedBibles.isNotEmpty()) {
                Text(
                    "Imported Bibles",
                    style = type.titleSmall.lexend(FontWeight.SemiBold),
                    color = colors.primary
                )
                Spacer(Modifier.height(12.dp))

                importedBibles.forEach { bible ->
                    key(bible.name) {
                        ImportedBibleRow(
                            name = bible.name,
                            verseCount = bible.verses.size,
                            isActive = bible.name == activeName,
                            removing = removing == bible.name,
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
            } else if (!importing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No Bibles imported yet",
                        style = type.titleMedium.lexend(FontWeight.SemiBold),
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pick a .jwpub file above to add one.",
                        style = type.bodyMedium.lexend(),
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Card with a short description and the import button underneath. */
@Composable
private fun ImportCard(importing: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = importing, label = "importText") { busy ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (busy) "Importing…" else "Import a Bible",
                        style = type.titleLarge.lexend(FontWeight.SemiBold),
                        color = colors.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (busy) "This may take a moment" else "Choose a .jwpub file from your device",
                        style = type.bodyMedium.lexend(),
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onClick,
                enabled = !importing,
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
                    if (importing) "Importing…" else "Import a Bible from a file",
                    style = type.labelLarge.lexend(FontWeight.SemiBold)
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

/** One row in the "Imported Bibles" list; handles its own enter / exit animation. */
@Composable
private fun ImportedBibleRow(
    name: String,
    verseCount: Int,
    isActive: Boolean,
    removing: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoved: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

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
        val container by animateColorAsState(
            targetValue = if (isActive) colors.secondaryContainer else colors.surfaceContainerHigh,
            animationSpec = tween<Color>(durationMillis = 150, easing = FastOutLinearInEasing),
            label = "bibleRowBg"
        )
        val contentColor = if (isActive) colors.onSecondaryContainer else colors.onSurface
        val secondaryColor = if (isActive) colors.onSecondaryContainer else colors.onSurfaceVariant

        val native = nativeNameOf(name)
        val verses = "%,d verses".format(verseCount)

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = container,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .pressScaleClickable(
                    pressedScale = 0.98f,
                    onClickLabel = "Activate $name",
                    onClick = onClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = null,
                    tint = if (isActive) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        native ?: name,
                        style = type.titleMedium.lexend(FontWeight.SemiBold),
                        color = contentColor
                    )
                    Text(
                        if (native != null) "$name · $verses" else verses,
                        style = type.bodySmall.lexend(),
                        color = secondaryColor
                    )
                }

                AnimatedVisibility(
                    visible = isActive,
                    enter = fadeIn(Motion.effectsFast),
                    exit = fadeOut(Motion.effectsFast)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = colors.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove $name",
                        tint = secondaryColor
                    )
                }
            }
        }
    }
}