@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.harsha.myapplication.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.harsha.myapplication.*
import com.harsha.myapplication.data.DEFAULT_CATEGORIES
import com.harsha.myapplication.data.DEFAULT_CATEGORY
import com.harsha.myapplication.data.FolderRepository
import com.harsha.myapplication.data.NoteDraft
import com.harsha.myapplication.data.NoteImages
import com.harsha.myapplication.data.NoteItem
import com.harsha.myapplication.data.NotesRepository
import com.harsha.myapplication.data.formatNoteDate
import com.harsha.myapplication.scripture.BibleData
import com.harsha.myapplication.scripture.ScriptureDetector
import com.harsha.myapplication.scripture.ScriptureReference
import com.harsha.myapplication.scripture.findScriptureSpans
import com.harsha.myapplication.scripture.localizedKey
import com.harsha.myapplication.ui.bible.BookBrowserDialog
import com.harsha.myapplication.ui.bible.DetectedScripturesScreen
import com.harsha.myapplication.ui.bible.ScriptureBottomSheet
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.editor.ScriptureBlue
import com.harsha.myapplication.ui.editor.ScriptureBlueDark
import com.harsha.myapplication.ui.editor.ScriptureRed
import com.harsha.myapplication.ui.editor.ScriptureRedDark
import com.harsha.myapplication.ui.editor.components.AlignPanel
import com.harsha.myapplication.ui.editor.components.BarToggle
import com.harsha.myapplication.ui.editor.components.FindBar
import com.harsha.myapplication.ui.editor.components.FormatPanel
import com.harsha.myapplication.ui.editor.components.ImagePanel
import com.harsha.myapplication.ui.editor.components.ImagePlaceholder
import com.harsha.myapplication.ui.editor.components.ImageViewer
import com.harsha.myapplication.ui.editor.components.PlacedImage
import com.harsha.myapplication.ui.editor.components.SelectionToolbar
import com.harsha.myapplication.ui.editor.components.SelectionToolbarState
import com.harsha.myapplication.ui.editor.components.Tip
import com.harsha.myapplication.ui.editor.components.TipIconButton
import com.harsha.myapplication.ui.editor.components.TipTonalIconButton
import com.harsha.myapplication.ui.editor.components.bottomInset
import com.harsha.myapplication.ui.editor.highlightColorOf
import com.harsha.myapplication.ui.editor.richtext.BOX_OFF
import com.harsha.myapplication.ui.editor.richtext.BOX_ON
import com.harsha.myapplication.ui.editor.richtext.DEFAULT_ASPECT
import com.harsha.myapplication.ui.editor.richtext.DEFAULT_PCT
import com.harsha.myapplication.ui.editor.richtext.EditorSnapshot
import com.harsha.myapplication.ui.editor.richtext.FmtType
import com.harsha.myapplication.ui.editor.richtext.FormatSpan
import com.harsha.myapplication.ui.editor.richtext.ImagePlacement
import com.harsha.myapplication.ui.editor.richtext.InkBlack
import com.harsha.myapplication.ui.editor.richtext.ListKind
import com.harsha.myapplication.ui.editor.richtext.OBJ
import com.harsha.myapplication.ui.editor.richtext.SizeOverride
import com.harsha.myapplication.ui.editor.richtext.SyncedLayout
import com.harsha.myapplication.ui.editor.richtext.TextEdit
import com.harsha.myapplication.ui.editor.richtext.TextSize
import com.harsha.myapplication.ui.editor.richtext.alignFromInt
import com.harsha.myapplication.ui.editor.richtext.alignSpansOf
import com.harsha.myapplication.ui.editor.richtext.alignToInt
import com.harsha.myapplication.ui.editor.richtext.alignsSaver
import com.harsha.myapplication.ui.editor.richtext.applyEdits
import com.harsha.myapplication.ui.editor.richtext.argAt
import com.harsha.myapplication.ui.editor.richtext.buildDisplayPlan
import com.harsha.myapplication.ui.editor.richtext.buildParas
import com.harsha.myapplication.ui.editor.richtext.buildStyled
import com.harsha.myapplication.ui.editor.richtext.compactImages
import com.harsha.myapplication.ui.editor.richtext.countWords
import com.harsha.myapplication.ui.editor.richtext.covers
import com.harsha.myapplication.ui.editor.richtext.decodeAligns
import com.harsha.myapplication.ui.editor.richtext.decodeSpans
import com.harsha.myapplication.ui.editor.richtext.encodeSpans
import com.harsha.myapplication.ui.editor.richtext.fitAligns
import com.harsha.myapplication.ui.editor.richtext.imgAlign
import com.harsha.myapplication.ui.editor.richtext.imgIdx
import com.harsha.myapplication.ui.editor.richtext.imgPct
import com.harsha.myapplication.ui.editor.richtext.imgRounded
import com.harsha.myapplication.ui.editor.richtext.imgWrap
import com.harsha.myapplication.ui.editor.richtext.keepCaretOffImages
import com.harsha.myapplication.ui.editor.richtext.lineEndOf
import com.harsha.myapplication.ui.editor.richtext.lineIndexAt
import com.harsha.myapplication.ui.editor.richtext.lineStartOf
import com.harsha.myapplication.ui.editor.richtext.mapOffset
import com.harsha.myapplication.ui.editor.richtext.markerOf
import com.harsha.myapplication.ui.editor.richtext.nextMarkerText
import com.harsha.myapplication.ui.editor.richtext.normalizeImages
import com.harsha.myapplication.ui.editor.richtext.packImage
import com.harsha.myapplication.ui.editor.richtext.prefixFor
import com.harsha.myapplication.ui.editor.richtext.renumber
import com.harsha.myapplication.ui.editor.richtext.shiftLineAligns
import com.harsha.myapplication.ui.editor.richtext.shiftSpans
import com.harsha.myapplication.ui.editor.richtext.shortcutPrefix
import com.harsha.myapplication.ui.editor.richtext.spansSaver
import com.harsha.myapplication.ui.editor.richtext.stringListSaver
import com.harsha.myapplication.ui.editor.richtext.subtract
import com.harsha.myapplication.ui.editor.richtext.textFmtTypes
import com.harsha.myapplication.ui.editor.richtext.toShown
import com.harsha.myapplication.ui.editor.richtext.upgradeLegacy
import com.harsha.myapplication.ui.editor.richtext.usedImageNames
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.MotionTransitions
import com.harsha.myapplication.ui.theme.RollingCount
import com.harsha.myapplication.ui.theme.ScriptureDetectionMotion
import com.harsha.myapplication.ui.theme.detectionGlow
import com.harsha.myapplication.ui.theme.detectionHalo
import com.harsha.myapplication.ui.theme.detectionIconTilt
import com.harsha.myapplication.ui.theme.detectionPop
import com.harsha.myapplication.ui.theme.detectionShake
import com.harsha.myapplication.ui.theme.revealOnEnter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Colours come from MaterialTheme.colorScheme: cursor = primary, selected / "on" states =
// secondaryContainer, meta dot = outline. Only content colours (highlight, scripture links) stay fixed.

/** Both bottom panels (Aa + Align) share this exact height so the layout never jumps. */
internal val PanelHeight = 360.dp

/**
 * Optional fine-tuning: shaves this much off the space under the controls while the keyboard
 * is open (only needed if your keyboard draws shorter than the height it reports). Default 0.
 */
private val KeyboardGapTrim = 0.dp

private const val UNDO_COALESCE_MS = 700L
private const val MAX_UNDO = 100
internal const val INDENT_STEP = 2
internal const val MAX_INDENT = 8
private const val MAX_PICK = 10

/** Line height = font size x this. 1.4 = a slight gap between lines; 1.2 is tight, 1.6 is airy. */
internal const val BodyLineHeightFactor = 1.4f

private enum class Panel { None, Format, Align, Image }

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

/**
 * Note editor.
 *
 * Existing notes open in VIEW mode (read only); a brand-new note opens straight in EDIT mode.
 * The tick saves and returns to view mode, the pencil switches back to edit mode.
 *
 * Editor features: bold / italic / underline / strikethrough / highlight / size / colour, clear
 * formatting, PER-PARAGRAPH alignment (left / centre / right / justify), bullet / numbered / lettered
 * lists with a hanging indent, tappable checklists ("[] " shortcut), indent / outdent, inline images,
 * undo / redo, find in note, word + character count, share / copy / delete, scripture detection.
 *
 * @param initialNote note being shown, or null for a new one.
 * @param onSave persist the draft and return the stored note. [existingId] is null until the note
 *               has been saved once; the editor remembers the id afterwards, so saving again
 *               updates the same note. The caller must NOT close the screen here.
 * @param onBack leave the screen. The editor itself asks "Save changes?" before calling this
 *               when there are unsaved edits, so the caller just closes the screen.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun NoteEditorView(
    initialNote: NoteItem?,
    onSave: (draft: NoteDraft, existingId: Int?) -> NoteItem,
    onBack: () -> Unit,
    categories: List<String> = DEFAULT_CATEGORIES,
    /** Folder colours (ARGB) already stored by the host, keyed by folder name. */
    folderColors: Map<String, Int> = emptyMap(),
    /** Called when the user creates a folder in the "New folder" popup, so the host can store its colour. */
    onCreateFolder: (name: String, colorArgb: Int) -> Unit = { _, _ -> },
    /** Folder preselected for a NEW note (e.g. the folder open in the notes list). Ignored for existing notes. */
    initialFolder: String? = null,
    /**
     * Called (after the user confirms) to delete the open note. When null, or for a note that was never
     * saved, the "Delete note" menu item is hidden. The host removes the note and closes the screen,
     * e.g. `onDelete = { id -> NotesRepository.delete(id); navController.popBackStack() }`.
     */
    onDelete: ((noteId: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Make sure the notes store and the saved folders are loaded (no-ops after the first call).
    remember(context) { NotesRepository.init(context); FolderRepository.init(context); true }
    val savedFolders = FolderRepository.folders

    // ---- initial values (used for dirty detection) ----
    // A note saved without a title is stored as "Untitled Note". Show that as a hint, never as
    // real text in the field, so the user can simply start typing a title.
    val initialTitle = remember {
        initialNote?.title?.takeUnless { it == NotesRepository.UNTITLED } ?: ""
    }
    val rawContent = remember { initialNote?.content ?: "" }
    // Old notes (images but no placeholders) are upgraded in memory so they are not "dirty".
    val upgraded = remember {
        upgradeLegacy(
            rawContent,
            decodeSpans(initialNote?.formatting.orEmpty(), rawContent.length)
                .filter { it.type != FmtType.Align }, // alignment is kept per line, not as a text span
            initialNote?.images.orEmpty()
        )
    }
    val initialContent = upgraded.first
    val initialSpans = upgraded.second
    val initialCategory = remember {
        initialNote?.category ?: initialFolder?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY
    }
    val initialImages = remember { initialNote?.images.orEmpty() }
    val initialAligns = remember {
        decodeAligns(initialNote?.formatting.orEmpty(), initialContent, initialNote?.alignment ?: 0)
    }
    val initialFormatting = remember { encodeSpans(initialSpans) }
    val initialUsedNames = remember { usedImageNames(initialSpans, initialImages) }

    // ---- editable state ----
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var bodyValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialContent))
    }
    var spans by rememberSaveable(stateSaver = spansSaver) { mutableStateOf(initialSpans) }
    // One alignment per line (paragraph): 0 left, 1 centre, 2 right, 3 justify.
    var lineAligns by rememberSaveable(stateSaver = alignsSaver) { mutableStateOf(initialAligns) }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var images by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(initialImages) }
    // Images imported during this editing session (so unsaved ones can be cleaned up).
    var sessionAdded by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(emptyList<String>())
    }
    var customCategories by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(emptyList<String>())
    }
    var importing by remember { mutableIntStateOf(0) } // images currently being copied
    val typing = remember { mutableStateMapOf<FmtType, Int>() } // style for newly typed text

    // Saved baseline. It moves forward on every save, so "dirty" means "changed since last save".
    var baseTitle by rememberSaveable { mutableStateOf(initialTitle) }
    var baseContent by rememberSaveable { mutableStateOf(initialContent) }
    var baseCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var baseAlignKey by rememberSaveable { mutableStateOf(initialAligns.joinToString("")) }
    var baseFormatting by rememberSaveable { mutableStateOf(initialFormatting) }
    var baseUsedNames by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(initialUsedNames)
    }
    // Id of the stored note (null until a new note is saved the first time).
    var noteId by rememberSaveable { mutableStateOf(initialNote?.id) }
    // New notes start in edit mode, existing notes in view mode.
    var editing by rememberSaveable { mutableStateOf(initialNote == null) }
    // Same format as the notes list: the note's last-edited time, or "now" for a new note.
    var dateText by rememberSaveable {
        mutableStateOf(
            initialNote?.date?.takeIf { it.isNotBlank() } ?: formatNoteDate(System.currentTimeMillis())
        )
    }

    val content = bodyValue.text
    val usedNames = usedImageNames(spans, images)
    val formattingJson = remember(spans) { encodeSpans(spans) }
    val alignKey = remember(lineAligns) { lineAligns.joinToString("") }
    val isDirty = title != baseTitle ||
            content != baseContent ||
            category != baseCategory ||
            alignKey != baseAlignKey ||
            formattingJson != baseFormatting ||
            usedNames != baseUsedNames
    val canSave = title.isNotBlank() || content.isNotBlank() || usedNames.isNotEmpty()

    var selectedReference by remember { mutableStateOf<ScriptureReference?>(null) }
    var showAddScripture by remember { mutableStateOf(false) }
    var showScriptureList by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var findOpen by rememberSaveable { mutableStateOf(false) }
    var findQuery by rememberSaveable { mutableStateOf("") }
    var findIndex by remember { mutableIntStateOf(0) }
    var replaceText by rememberSaveable { mutableStateOf("") }
    // Colours of folders created in this session, stored as "name<US>argb" so they survive rotation.
    var customFolderColors by rememberSaveable(stateSaver = stringListSaver) {
        mutableStateOf(emptyList<String>())
    }
    var viewerImage by remember { mutableStateOf<String?>(null) }
    var imagePanelIdx by remember { mutableStateOf<Int?>(null) } // image shown in the docked image panel
    var pendingRemoveIdx by rememberSaveable { mutableStateOf<Int?>(null) } // image awaiting delete confirmation
    val snackbar = remember { SnackbarHostState() }

    // Same sources as the notes list menu: default/hosted categories, every folder the host has a
    // colour for (i.e. created in either screen), folders created here, and the note's own folder.
    val allFolderColors = remember(folderColors, savedFolders) {
        folderColors + savedFolders.associate { it.name to it.colorArgb }
    }
    val categoryOptions = remember(categories, allFolderColors, customCategories, category) {
        (categories + allFolderColors.keys + customCategories + category)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    }
    val folderColorMap = remember(allFolderColors, customFolderColors) {
        val m = allFolderColors.mapValues { Color(it.value) }.toMutableMap()
        customFolderColors.forEach { e ->
            val i = e.lastIndexOf('\u001F')
            if (i > 0) e.substring(i + 1).toIntOrNull()?.let { m[e.substring(0, i)] = Color(it) }
        }
        m
    }
    val folderColorOf: (String) -> Color = { name ->
        folderColorMap[name]
            ?: folderColorMap.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
            ?: DefaultFolderColor
    }

    // ---- panels / focus ----
    var panel by remember { mutableStateOf(Panel.None) }
    var focusOnEdit by remember { mutableStateOf(false) } // open the keyboard after tapping Edit
    var lastPanel by remember { mutableStateOf(Panel.Format) }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val bodyFocus = remember { FocusRequester() }
    val imeVisible = WindowInsets.isImeVisible
    val emptyTapSource = remember { MutableInteractionSource() }

    // ---- undo / redo (full snapshots, typing is grouped into bursts) ----
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }
    var lastUndoPushAt by remember { mutableLongStateOf(0L) }

    fun snapshot() = EditorSnapshot(
        title, bodyValue.text, spans, bodyValue.selection, lineAligns, category, images
    )

    /** Records the current state so it can be undone. [coalesce] groups rapid typing in one step. */
    fun pushUndo(coalesce: Boolean = false) {
        val now = SystemClock.uptimeMillis()
        val inBurst = coalesce && undoStack.isNotEmpty() && now - lastUndoPushAt < UNDO_COALESCE_MS
        lastUndoPushAt = if (coalesce) now else 0L
        redoStack.clear()
        if (inBurst) return
        undoStack.add(snapshot())
        if (undoStack.size > MAX_UNDO) undoStack.removeAt(0)
    }

    fun restore(s: EditorSnapshot) {
        title = s.title
        spans = s.spans
        lineAligns = s.aligns
        category = s.category
        images = s.images
        bodyValue = TextFieldValue(
            s.text,
            TextRange(s.selection.start.coerceIn(0, s.text.length), s.selection.end.coerceIn(0, s.text.length))
        )
        lastUndoPushAt = 0L
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(snapshot())
        restore(undoStack.removeAt(undoStack.lastIndex))
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(snapshot())
        restore(redoStack.removeAt(redoStack.lastIndex))
    }

    // ---- save / exit / view <-> edit ----

    /** Stores the note. The editor stays open and its state now mirrors exactly what was stored. */
    fun persistNote() {
        keyboard?.hide()
        val (cleanSpans, cleanImages) = compactImages(spans, images)
        val draft = NoteDraft(
            title = title.trimEnd(),
            content = bodyValue.text,
            category = category,
            formatting = encodeSpans(cleanSpans + alignSpansOf(bodyValue.text, lineAligns)),
            alignment = lineAligns.lastOrNull() ?: 0,
            images = cleanImages
        )
        // Images imported (or rotated) this session but not in the final note are never needed.
        sessionAdded.filter { it !in draft.images }.forEach { NoteImages.delete(it) }
        val saved = onSave(draft, noteId)
        noteId = saved.id

        spans = cleanSpans
        images = cleanImages
        sessionAdded = emptyList()
        // Undo history would point at image files that may be gone now - start fresh.
        undoStack.clear()
        redoStack.clear()
        lastUndoPushAt = 0L

        baseTitle = title
        baseContent = bodyValue.text
        baseCategory = category
        baseAlignKey = lineAligns.joinToString("")
        baseFormatting = encodeSpans(cleanSpans)
        baseUsedNames = usedImageNames(cleanSpans, cleanImages)
        dateText = saved.date
    }

    fun enterViewMode() {
        keyboard?.hide()
        focusManager.clearFocus()
        panel = Panel.None
        editing = false
    }

    fun enterEditMode() {
        bodyValue = bodyValue.copy(selection = TextRange(bodyValue.text.length))
        editing = true
        focusOnEdit = true
    }

    /** Tick: save and show the note in view mode. */
    fun saveAndView() {
        persistNote()
        enterViewMode()
    }

    /** "Save" in the unsaved-changes dialog: save and leave the screen. */
    fun saveAndExit() {
        persistNote()
        onBack()
    }

    /** Tick button: saves when something changed, otherwise just leaves edit mode. */
    fun finishEditing() {
        if (isDirty || noteId == null) saveAndView() else enterViewMode()
    }

    fun discardAndExit() {
        // Everything imported in this session is dropped together with the changes.
        sessionAdded.forEach { NoteImages.delete(it) }
        onBack()
    }

    fun requestExit() {
        if (isDirty) showDiscardDialog = true else discardAndExit() // also cleans files added this session
    }

    // Registered first so the handlers below (registered later) win while they are enabled.
    BackHandler(enabled = !isDirty && sessionAdded.isNotEmpty()) { discardAndExit() }
    BackHandler(enabled = isDirty) { showDiscardDialog = true }
    BackHandler(enabled = panel != Panel.None) { panel = Panel.None }
    BackHandler(enabled = findOpen) { findOpen = false }

    // Keyboard coming back closes any open panel
    LaunchedEffect(imeVisible) { if (imeVisible) panel = Panel.None }

    // The image panel closes when its image is gone (removed, or undone away)
    LaunchedEffect(panel, spans, imagePanelIdx) {
        if (panel == Panel.Image &&
            spans.none { it.type == FmtType.Image && imgIdx(it.arg) == imagePanelIdx }
        ) panel = Panel.None
    }

    // New notes open ready to type
    LaunchedEffect(Unit) {
        if (noteId == null && editing && title.isEmpty() && bodyValue.text.isEmpty()) {
            delay(250) // let the screen transition finish before opening the keyboard
            runCatching { bodyFocus.requestFocus() }
            keyboard?.show()
        }
    }

    // After tapping Edit: wait one frame so the field is writable, then open the keyboard
    LaunchedEffect(editing, focusOnEdit) {
        if (editing && focusOnEdit) {
            focusOnEdit = false
            delay(60)
            runCatching { bodyFocus.requestFocus() }
            keyboard?.show()
        }
    }

    fun togglePanel(target: Panel) {
        if (panel == target) {
            panel = Panel.None
            runCatching { bodyFocus.requestFocus() }
            keyboard?.show()
        } else {
            panel = target
            lastPanel = target
            keyboard?.hide()
        }
    }

    /** Tapping a picture opens the docked image panel - whatever panel was open before. */
    fun selectImage(idx: Int) {
        imagePanelIdx = idx
        panel = Panel.Image
        lastPanel = Panel.Image
        keyboard?.hide()
    }

    /** Tap on empty space: focus the body, put the caret at the end and open the keyboard. */
    // Bumped whenever the caret must be scrolled into view AFTER the keyboard has resized the screen
    var revealTick by remember { mutableIntStateOf(0) }

    fun focusBodyAtEnd() {
        panel = Panel.None
        bodyValue = bodyValue.copy(selection = TextRange(bodyValue.text.length))
        runCatching { bodyFocus.requestFocus() }
        keyboard?.show()
        revealTick++
    }

    // When the cursor moves, "typing style" follows the character before it
    LaunchedEffect(bodyValue.selection) {
        val sel = bodyValue.selection
        typing.clear()
        if (sel.collapsed && sel.start > 0) {
            textFmtTypes.forEach { t -> spans.argAt(t, sel.start - 1)?.let { typing[t] = it } }
        }
    }

    // ---- body editing ----
    fun updateBody(input: TextFieldValue, byUser: Boolean) {
        val old = bodyValue.text
        var v = input
        var forceRenumber = false
        // Alignment is per paragraph: it follows every line that is added, split or joined.
        var la = shiftLineAligns(lineAligns, old, input.text, input.selection.start)
        fun setV(nv: TextFieldValue) {
            if (nv.text != v.text) la = shiftLineAligns(la, v.text, nv.text, nv.selection.start)
            v = nv
        }

        if (byUser && v.selection.collapsed) {
            val pos = v.selection.start
            val oneInserted = v.text.length == old.length + 1 && pos > 0 &&
                    v.text.removeRange(pos - 1, pos) == old

            if (oneInserted && v.text[pos - 1] == '\n') {
                // Enter on a list line continues the list; on an empty item it outdents / ends it
                val lineStart = lineStartOf(v.text, pos - 1)
                val line = v.text.substring(lineStart, pos - 1)
                val m = markerOf(line)
                if (m != null) {
                    if (line.length == m.length) {
                        forceRenumber = true
                        if (m.indent > 0) {
                            val step = minOf(INDENT_STEP, m.indent)
                            setV(
                                TextFieldValue(
                                    old.removeRange(lineStart, lineStart + step),
                                    TextRange(pos - 1 - step)
                                )
                            )
                        } else {
                            setV(
                                TextFieldValue(
                                    old.removeRange(lineStart, lineStart + m.length),
                                    TextRange(lineStart)
                                )
                            )
                        }
                    } else {
                        val ins = nextMarkerText(m)
                        setV(
                            TextFieldValue(
                                v.text.substring(0, pos) + ins + v.text.substring(pos),
                                TextRange(pos + ins.length)
                            )
                        )
                    }
                }
            } else if (oneInserted && v.text[pos - 1] == ' ') {
                // "- " / "* " -> bullet, "1) " -> "1. ", "a) " -> "a. ", "[] " -> checkbox (works when indented too)
                val ls = lineStartOf(v.text, pos - 1)
                val typed = v.text.substring(ls, pos)
                val trimmed = typed.trimStart(' ')
                val replacement = shortcutPrefix(trimmed)
                if (replacement != null) {
                    val markerStart = ls + (typed.length - trimmed.length)
                    setV(
                        TextFieldValue(
                            v.text.replaceRange(markerStart, pos, replacement),
                            TextRange(markerStart + replacement.length)
                        )
                    )
                }
            } else if (v.text.length == old.length - 1 && old.removeRange(pos, pos + 1) == v.text) {
                // Backspace on an empty list item: outdent first, then remove the marker
                val ls = lineStartOf(old, pos)
                val le = lineEndOf(old, ls)
                val line = old.substring(ls, le)
                val m = markerOf(line)
                if (m != null && line.length == m.length && pos == ls + m.length - 1) {
                    forceRenumber = true
                    if (m.indent > 0) {
                        val step = minOf(INDENT_STEP, m.indent)
                        setV(TextFieldValue(old.removeRange(ls, ls + step), TextRange(ls + m.length - step)))
                    } else {
                        setV(TextFieldValue(old.removeRange(ls, le), TextRange(ls)))
                    }
                }
            }
        }

        if (v.text != old) {
            if (byUser) pushUndo(coalesce = true)
            val (shifted, inserted) = shiftSpans(spans, old, v.text)
            var r = shifted
            if (byUser && inserted != null) {
                textFmtTypes.forEach { t ->
                    r = r.subtract(t, inserted.first, inserted.last + 1)
                    typing[t]?.let { r = r + FormatSpan(inserted.first, inserted.last + 1, t, it) }
                }
            }
            // Keep numbered / lettered lists in sequence after lines are added or removed
            val newlinesChanged = v.text.count { it == '\n' } != old.count { it == '\n' }
            if (newlinesChanged || forceRenumber) {
                val (t, r2, edits) = renumber(v.text, r, v.selection.start)
                if (edits.isNotEmpty()) {
                    r = r2
                    setV(
                        TextFieldValue(
                            t,
                            TextRange(
                                mapOffset(v.selection.start, edits),
                                mapOffset(v.selection.end, edits)
                            )
                        )
                    )
                }
            }
            spans = r
        }
        if (byUser) {
            val norm = normalizeImages(v.text, spans, old.length)
            if (norm.edits.isNotEmpty()) {
                spans = norm.spans
                setV(
                    TextFieldValue(
                        norm.text,
                        TextRange(
                            mapOffset(v.selection.start, norm.edits),
                            mapOffset(v.selection.end, norm.edits)
                        )
                    )
                )
            }
            setV(keepCaretOffImages(v, spans, bodyValue.selection.start))
        }
        lineAligns = fitAligns(la, v.text)
        bodyValue = v
    }

    // ---- inline image operations ----

    /** Puts [names] (image indexes [firstIdx]..) on their own lines at the caret. */
    fun insertImages(firstIdx: Int, names: List<String>) {
        val text = bodyValue.text
        var pos = bodyValue.selection.max.coerceIn(0, text.length)
        val ls = lineStartOf(text, pos)
        if (ls < text.length && text[ls] == OBJ) pos = (lineEndOf(text, ls) + 1).coerceAtMost(text.length)
        val before = text.substring(0, pos)
        val sb = StringBuilder()
        if (!before.endsWith("\n")) sb.append('\n') // before is empty or mid-line
        val offsets = mutableListOf<Int>()
        names.forEach {
            offsets += pos + sb.length
            sb.append(OBJ).append('\n')
        }
        val (t, shifted) = applyEdits(text, spans, listOf(TextEdit(pos, 0, sb.toString())))
        lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, pos + sb.length), t)
        spans = shifted + offsets.mapIndexed { i, off ->
            FormatSpan(off, off + 1, FmtType.Image, packImage(firstIdx + i, DEFAULT_PCT, 1, true))
        }
        bodyValue = TextFieldValue(t, TextRange(pos + sb.length))
    }

    fun updateImage(
        idx: Int,
        pct: Int? = null,
        align: Int? = null,
        rounded: Boolean? = null,
        wrap: Boolean? = null
    ) {
        val sp = spans.firstOrNull { it.type == FmtType.Image && imgIdx(it.arg) == idx } ?: return
        val arg = packImage(
            idx,
            pct ?: imgPct(sp.arg),
            align ?: imgAlign(sp.arg),
            rounded ?: imgRounded(sp.arg),
            wrap ?: imgWrap(sp.arg)
        )
        if (arg == sp.arg) return
        pushUndo(coalesce = true)
        spans = spans.map { if (it == sp) it.copy(arg = arg) else it }
    }

    fun removeImage(idx: Int) {
        val sp = spans.firstOrNull { it.type == FmtType.Image && imgIdx(it.arg) == idx } ?: return
        val text = bodyValue.text
        val end = if (sp.end < text.length && text[sp.end] == '\n') sp.end + 1 else sp.end
        pushUndo()
        val (t, r) = applyEdits(text, spans, listOf(TextEdit(sp.start, end - sp.start, "")))
        lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, sp.start), t)
        spans = r
        bodyValue = TextFieldValue(t, TextRange(sp.start.coerceAtMost(t.length)))
    }

    /** Removes the image and offers an Undo snackbar (only undoes if nothing else was edited since). */
    fun confirmRemoveImage(idx: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        removeImage(idx)
        val mark = undoStack.size
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Image removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && undoStack.size == mark) undo()
        }
    }

    fun rotateImage(idx: Int) {
        val old = images.getOrNull(idx) ?: return
        scope.launch {
            val fresh = NoteImages.rotate(old)
            if (fresh == null) {
                Toast.makeText(context, "Couldn't rotate that image", Toast.LENGTH_SHORT).show()
            } else {
                pushUndo()
                images = images.toMutableList().also { it[idx] = fresh }
                sessionAdded = sessionAdded + fresh
            }
        }
    }

    fun toggleFmt(type: FmtType) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        if (s == e) {
            if (typing.containsKey(type)) typing.remove(type) else typing[type] = 0
        } else {
            pushUndo()
            val cleared = spans.subtract(type, s, e)
            spans = if (spans.covers(type, s, e)) cleared else cleared + FormatSpan(s, e, type)
        }
    }

    fun setFmt(type: FmtType, arg: Int, isDefault: Boolean) {
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        if (s == e) {
            if (isDefault) typing.remove(type) else typing[type] = arg
        } else {
            pushUndo()
            val cleared = spans.subtract(type, s, e)
            spans = if (isDefault) cleared else cleared + FormatSpan(s, e, type, arg)
        }
    }

    fun isOn(type: FmtType): Boolean {
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        return if (s == e) typing.containsKey(type) else spans.covers(type, s, e)
    }

    fun argOf(type: FmtType): Int? {
        val s = bodyValue.selection.min
        val e = bodyValue.selection.max
        return if (s == e) typing[type]
        else if (spans.covers(type, s, e)) spans.argAt(type, s) else null
    }

    /** Start offsets of every line touched by the selection. */
    fun selectedLineStarts(): List<Int> {
        val text = bodyValue.text
        val len = text.length
        val sMin = bodyValue.selection.min.coerceIn(0, len)
        val sMax = bodyValue.selection.max.coerceIn(0, len)
        val firstStart = lineStartOf(text, sMin)
        val end = if (sMax > sMin) sMax - 1 else sMax
        val starts = mutableListOf<Int>()
        var i = firstStart
        while (i <= end) {
            starts.add(i)
            val nl = text.indexOf('\n', i)
            if (nl == -1) break
            i = nl + 1
        }
        if (starts.isEmpty()) starts.add(firstStart)
        return starts
    }

    /** Aligns the lines touched by the selection only (just the caret's line when nothing is selected). */
    fun setAlign(a: TextAlign) {
        val value = alignToInt(a)
        val text = bodyValue.text
        val cur = fitAligns(lineAligns, text)
        val idxs = selectedLineStarts().map { lineIndexAt(text, it) }
        if (idxs.all { cur.getOrElse(it) { 0 } == value }) return
        pushUndo()
        val next = cur.toMutableList()
        idxs.forEach { if (it in next.indices) next[it] = value }
        lineAligns = next
    }

    /** Ticks / unticks the checklist item that starts at [lineStart]. */
    fun toggleCheck(lineStart: Int) {
        val text = bodyValue.text
        if (lineStart < 0 || lineStart >= text.length) return
        val m = markerOf(text.substring(lineStart, lineEndOf(text, lineStart))) ?: return
        if (m.kind != ListKind.Check) return
        val at = lineStart + m.indent
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pushUndo()
        val flipped = if (text[at] == BOX_ON) BOX_OFF else BOX_ON
        bodyValue = TextFieldValue(
            text.substring(0, at) + flipped + text.substring(at + 1),
            bodyValue.selection
        )
    }

    /** Removes bold / italic / underline / strike / highlight / size / colour from the selection. */
    fun clearFormatting() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val s0 = bodyValue.selection.min
        val e0 = bodyValue.selection.max
        if (s0 == e0) {
            typing.clear()
            return
        }
        pushUndo()
        var r = spans
        textFmtTypes.forEach { r = r.subtract(it, s0, e0) }
        spans = r
    }

    /** Title + text of the note without the invisible image placeholders. */
    fun noteAsText(): String = buildString {
        if (title.isNotBlank()) append(title.trim()).append("\n\n")
        append(bodyValue.text.filter { it != OBJ }.trim())
    }

    fun shareNote() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title.trim())
            putExtra(Intent.EXTRA_TEXT, noteAsText())
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, "Share note").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun copyNote() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Note", noteAsText()))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun applyList(kind: ListKind) {
        val text = bodyValue.text
        val starts = selectedLineStarts()
        val markers = starts.map { markerOf(text.substring(it, lineEndOf(text, it))) }
        val allHave = markers.all { it?.kind == kind }
        val edits = starts.mapIndexedNotNull { i, st ->
            val m = markers[i]
            val length = m?.length ?: 0
            val replacement =
                if (allHave) "" else " ".repeat(m?.indent ?: 0) + prefixFor(kind, i + 1)
            if (length == 0 && replacement.isEmpty()) null else TextEdit(st, length, replacement)
        }
        if (edits.isEmpty()) return
        pushUndo()
        val (t1, r1) = applyEdits(text, spans, edits)
        val lastLine = mapOffset(starts.last(), edits)
        val (t2, r2, e2) = renumber(t1, r1, lastLine)
        val cursor = lineEndOf(t2, mapOffset(lastLine, e2).coerceIn(0, t2.length))
        spans = r2
        bodyValue = TextFieldValue(t2, TextRange(cursor))
    }

    /** Indent (+1) or outdent (-1) the selected list items; numbering is fixed up afterwards. */
    fun indentSelection(delta: Int) {
        val text = bodyValue.text
        val edits = selectedLineStarts().mapNotNull { st ->
            val m = markerOf(text.substring(st, lineEndOf(text, st))) ?: return@mapNotNull null
            val newIndent = (m.indent + delta * INDENT_STEP).coerceIn(0, MAX_INDENT)
            if (newIndent == m.indent) null else TextEdit(st, m.indent, " ".repeat(newIndent))
        }
        if (edits.isEmpty()) return
        pushUndo()
        val (t1, r1) = applyEdits(text, spans, edits)
        val s1 = mapOffset(bodyValue.selection.start, edits)
        val e1 = mapOffset(bodyValue.selection.end, edits)
        val (t2, r2, e2) = renumber(t1, r1, s1)
        spans = r2
        bodyValue = TextFieldValue(t2, TextRange(mapOffset(s1, e2), mapOffset(e1, e2)))
    }

    /** Inserts a scripture reference at the caret, adding spaces only where needed. */
    fun insertScripture(reference: String) {
        val sel = bodyValue.selection
        val text = bodyValue.text
        val before = text.substring(0, sel.min)
        val after = text.substring(sel.max)
        val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        val trail = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
        updateBody(
            TextFieldValue(
                before + lead + reference + trail + after,
                TextRange(before.length + lead.length + reference.length)
            ),
            byUser = true
        )
    }

    val activeList: ListKind? = run {
        val st = bodyValue.selection.min.coerceIn(0, content.length)
        val ls = lineStartOf(content, st)
        markerOf(content.substring(ls, lineEndOf(content, ls)))?.kind
    }

    // Alignment of the line the caret is on (shown as selected in the Align panel)
    val caretLineAlign: TextAlign =
        alignFromInt(lineAligns.getOrElse(lineIndexAt(content, bodyValue.selection.min)) { 0 })

    // ---- find in note ----
    val findMatches: List<IntRange> = remember(content, findQuery, findOpen) {
        if (!findOpen || findQuery.isEmpty()) emptyList() else buildList {
            var i = content.indexOf(findQuery, 0, ignoreCase = true)
            while (i >= 0 && size < 1000) {
                add(i until i + findQuery.length)
                i = content.indexOf(findQuery, i + findQuery.length, ignoreCase = true)
            }
        }
    }
    val findCurrent = if (findMatches.isEmpty()) 0 else findIndex.coerceIn(0, findMatches.size - 1)

    /** Find & replace: replaces the current match, or every match, keeping the text styling in sync. */
    fun replaceMatches(all: Boolean) {
        if (!editing) return
        val targets = if (all) findMatches else listOfNotNull(findMatches.getOrNull(findCurrent))
        if (targets.isEmpty()) return
        val text = bodyValue.text
        val edits = targets.map { TextEdit(it.first, it.last + 1 - it.first, replaceText) }
        pushUndo()
        val (t, r) = applyEdits(text, spans, edits)
        val caret = mapOffset(targets.last().last + 1, edits).coerceIn(0, t.length)
        spans = r
        bodyValue = TextFieldValue(t, TextRange(caret))
    }
    val wordCount = remember(content) { countWords(content) }
    val charCount = remember(content) { content.count { it != OBJ && it != '\n' } }
    val bodyBring = remember { BringIntoViewRequester() }

    // Detection is debounced and runs on a background thread so long notes never freeze typing.
    var detectedReferences by remember { mutableStateOf<List<ScriptureReference>>(emptyList()) }
    var scriptureSpans by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    var invalidScriptureSpans by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    // Drives the "scripture detected" animation (glow in the text, button pop, badge halo).
    val detectionFx = remember { ScriptureDetectionMotion() }
    // Texts of the wrong references from the previous pass, so the shake only plays for NEW ones.
    val lastInvalidTexts = remember { mutableListOf<String>() }
    LaunchedEffect(content, BibleData.version) {
        delay(150)
        // The caret is still inside / at the end of a chapter:verse number ("2:2" on the way to
        // "2:20"). Wait a little longer so a half-typed number is never judged (blue / red).
        // Typing another character restarts this effect, so the wait only ends when the user pauses.
        if (editing) {
            val sel = bodyValue.selection
            val caret = if (sel.collapsed) sel.end else -1
            if (caret in 1..content.length && content[caret - 1].isDigit()) {
                val typingNumber = withContext(Dispatchers.Default) {
                    ScriptureDetector.extractReferences(content)
                        .any { it.startIndex < caret && caret <= it.endIndex }
                }
                if (typingNumber) delay(700)
            }
        }
        val (refs, ranges, invalid) = withContext(Dispatchers.Default) {
            val found = ScriptureDetector.extractReferences(content).toList()
            // The detector's own references are the source of truth: each one is exactly one span, so
            // neighbouring scriptures can never merge and fuzzy / misspelled names are covered too.
            val fromDetector = found
                .map { it.startIndex.coerceAtLeast(0) until it.endIndex.coerceAtMost(content.length) }
                .filter { !it.isEmpty() }
            // The older word-by-word finder stays as a safety net for anything the detector did not claim.
            val fallback = findScriptureSpans(content).filter { r ->
                fromDetector.none { it.first <= r.last && r.first <= it.last }
            }
            val allRanges = (fromDetector + fallback).sortedBy { it.first }
            // Split ranges into valid / invalid against the active Bible.
            val valid = mutableListOf<IntRange>()
            val bad = mutableListOf<IntRange>()
            for (range in allRanges) {
                val overlapping = found.filter {
                    it.startIndex < range.last + 1 && it.endIndex > range.first
                }
                val isBad = overlapping.isNotEmpty() && overlapping.all { !BibleData.isValidPassage(it) }
                if (isBad) bad += range else valid += range
            }
            Triple(found, valid, bad)
        }
        // Play the effect only when a reference was ADDED (count went up), never while one is
        // merely being edited, so typing "John 3:1" -> "John 3:16" does not flash twice.
        val previousTexts = detectedReferences.map { it.rawText }.toMutableList()
        val grew = refs.size > detectedReferences.size
        val fresh = if (grew) {
            ranges.filter { r ->
                val t = content.substring(r.first, (r.last + 1).coerceAtMost(content.length))
                !previousTexts.remove(t)
            }
        } else emptyList()
        detectedReferences = refs
        scriptureSpans = ranges
        invalidScriptureSpans = invalid
        // Wrong reference (does not exist in this Bible): red + shake + reject vibration.
        val oldInvalid = lastInvalidTexts.toMutableList()
        val newInvalid = invalid.filter { r ->
            val t = content.substring(r.first, (r.last + 1).coerceAtMost(content.length))
            !oldInvalid.remove(t)
        }
        lastInvalidTexts.clear()
        invalid.mapTo(lastInvalidTexts) { content.substring(it.first, (it.last + 1).coerceAtMost(content.length)) }
        if (editing && newInvalid.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            detectionFx.triggerInvalid(scope, newInvalid)
        }
        if (grew) {
            // Haptics only for what the user just typed, not when an existing note is opened.
            if (editing) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            detectionFx.trigger(scope, fresh.ifEmpty { ranges.takeLast(1) })
        }
    }

    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var bodyLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var synced by remember { mutableStateOf<SyncedLayout?>(null) }

    // System photo picker: no storage permission needed, falls back to the file picker on old devices.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            importing += uris.size
            val added = mutableListOf<String>()
            var failed = 0
            for (uri in uris) {
                val name = try {
                    NoteImages.import(context, uri)
                } finally {
                    importing -= 1
                }
                if (name != null) added += name else failed++
            }
            if (added.isNotEmpty()) {
                pushUndo()
                val firstIdx = images.size
                images = images + added
                sessionAdded = sessionAdded + added
                insertImages(firstIdx, added)
                // Open the docked image panel straight away (image icon lights up like Aa / Align).
                // Clearing focus stops the returning keyboard from closing the panel again.
                focusManager.clearFocus()
                selectImage(firstIdx + added.size - 1)
            }
            if (failed > 0) {
                Toast.makeText(
                    context,
                    if (failed == 1) "Couldn't add that image" else "Couldn't add $failed images",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val colors = MaterialTheme.colorScheme

    // ---- bottom spacing: measured, so it is right no matter what the parent already pads ----
    val view = LocalView.current
    val density = LocalDensity.current

    // ---- inline image layout (sizes depend on the text width) ----
    val aspects = remember { mutableStateMapOf<String, Float>() }
    LaunchedEffect(images) {
        images.forEach { n ->
            if (!aspects.containsKey(n)) NoteImages.aspectRatio(n)?.let { aspects[n] = it }
        }
    }
    var bodyWidthPx by remember { mutableIntStateOf(0) }
    val imageMarginPx = with(density) { 6.dp.toPx() }
    val placements = spans.filter { it.type == FmtType.Image }.sortedBy { it.start }.mapNotNull { sp ->
        val idx = imgIdx(sp.arg)
        val name = images.getOrNull(idx) ?: return@mapNotNull null
        val w = bodyWidthPx * imgPct(sp.arg) / 100f
        ImagePlacement(
            start = sp.start, idx = idx, name = name,
            widthPx = w, heightPx = w / (aspects[name] ?: DEFAULT_ASPECT),
            align = imgAlign(sp.arg), rounded = imgRounded(sp.arg), pct = imgPct(sp.arg),
            wrap = imgWrap(sp.arg)
        )
    }
    val measurer = rememberTextMeasurer()
    val bodyTextStyle = TextStyle(
        fontFamily = Lexend,
        fontSize = 16.sp,
        color = colors.onSurface,
        textAlign = TextAlign.Start // real alignment is applied per paragraph (see buildParas)
    )
    // How tall a line gets per px of font size for the placeholder glyph (used to reserve image space)
    val linePerFont = remember(measurer, density) {
        val fontPx = with(density) { 100.sp.toPx() }
        (measurer.measure("\uFFFC", TextStyle(fontFamily = Lexend, fontSize = 100.sp)).size.height / fontPx)
            .coerceAtLeast(0.5f)
    }
    val sideGapPx = with(density) { 12.dp.toPx() }
    val minTextPx = with(density) { 96.dp.toPx() }
    val scriptureColor = if (darkSurface) ScriptureBlueDark else ScriptureBlue
    val invalidScriptureColor = if (darkSurface) ScriptureRedDark else ScriptureRed
    val allScriptureSpans = remember(scriptureSpans, invalidScriptureSpans) {
        scriptureSpans + invalidScriptureSpans
    }
    // Width of a list marker ("1. ", "• ", ...) in the note font: wrapped lines are indented by exactly that much
    val hangCache = remember(measurer, density) { HashMap<String, Float>() }
    val hangOf: (String, Int) -> TextUnit = remember(measurer, density) {
        { prefix: String, sizeIdx: Int ->
            val px = hangCache.getOrPut("$sizeIdx|$prefix") {
                val st = TextStyle(
                    fontFamily = Lexend,
                    fontSize = TextSize.entries.getOrNull(sizeIdx)?.size ?: 16.sp
                )
                val withMarker = measurer.measure(prefix.replace(' ', '\u00A0') + "|", st).size.width
                val barOnly = measurer.measure("|", st).size.width
                (withMarker - barOnly).toFloat().coerceAtLeast(0f)
            }
            with(density) { px.toSp() }
        }
    }
    val paras = remember(content, lineAligns, spans, hangOf) {
        buildParas(content, lineAligns, spans, hangOf)
    }
    // What is drawn = the saved styling + checked items struck through + find matches highlighted
    val displaySpans = remember(spans, content, findMatches, findCurrent) {
        val extra = ArrayList<FormatSpan>()
        if (content.indexOf(BOX_ON) >= 0) {
            var ls = 0
            while (true) {
                val le = lineEndOf(content, ls)
                val m = markerOf(content.substring(ls, le))
                if (m != null && m.kind == ListKind.Check && m.checked && le > ls + m.length) {
                    extra.add(FormatSpan(ls + m.length, le, FmtType.Strike))
                }
                if (le >= content.length) break
                ls = le + 1
            }
        }
        findMatches.forEachIndexed { i, r ->
            extra.add(FormatSpan(r.first, r.last + 1, FmtType.Highlight))
            if (i == findCurrent) {
                extra.add(FormatSpan(r.first, r.last + 1, FmtType.Bold))
                extra.add(FormatSpan(r.first, r.last + 1, FmtType.Underline))
            }
        }
        if (extra.isEmpty()) spans else spans + extra
    }
    val plan = remember(
        content, spans, scriptureSpans, invalidScriptureSpans, placements, bodyWidthPx, paras,
        scriptureColor, invalidScriptureColor, linePerFont
    ) {
        buildDisplayPlan(
            text = content,
            spans = spans,
            scripture = scriptureSpans,
            scriptureColor = scriptureColor,
            placements = placements,
            bodyWidthPx = bodyWidthPx,
            paras = paras,
            measurer = measurer,
            baseStyle = bodyTextStyle,
            linePerFont = linePerFont,
            pxToSp = { px -> with(density) { px.toSp() } },
            marginPx = imageMarginPx,
            gapPx = sideGapPx,
            minTextPx = minTextPx,
            invalidScripture = invalidScriptureSpans,
            invalidScriptureColor = invalidScriptureColor
        )
    }
    val planState = rememberUpdatedState(plan)
    val scriptureSpansState = rememberUpdatedState(allScriptureSpans)
    val detectedReferencesState = rememberUpdatedState(detectedReferences)
    val contentState = rememberUpdatedState(content)
    val placementsState = rememberUpdatedState(placements)
    val editingState = rememberUpdatedState(editing)
    val toggleCheckState = rememberUpdatedState<(Int) -> Unit>({ ls -> toggleCheck(ls) })
    val styleTransformation = remember(
        plan, displaySpans, paras, lineAligns, hangOf, scriptureSpans, invalidScriptureSpans,
        scriptureColor, invalidScriptureColor, placements, linePerFont, darkSurface
    ) {
        VisualTransformation { text ->
            if (text.text != plan.original) {
                // Field is one frame ahead of the plan: draw plain text BUT keep the image lines tall,
                // otherwise the whole text below the image jumps up and back down.
                val keep = placements.mapNotNull { p ->
                    if (p.start < text.text.length && text.text[p.start] == OBJ) {
                        val h = p.heightPx + 2 * imageMarginPx
                        SizeOverride(
                            p.start, p.start + 1,
                            with(density) { (h / linePerFont).toSp() }
                        )
                    } else null
                }
                TransformedText(
                    buildStyled(
                        text.text, displaySpans, scriptureSpans, scriptureColor, keep, darkSurface,
                        invalidScriptureSpans, invalidScriptureColor,
                        paras = buildParas(text.text, lineAligns, spans, hangOf)
                    ),
                    OffsetMapping.Identity
                )
            } else {
                TransformedText(
                    buildStyled(
                        plan.shown,
                        displaySpans.map { it.copy(start = plan.spanStart(it.start), end = plan.spanEnd(it.end)) },
                        scriptureSpans.map { plan.spanStart(it.first) until plan.spanEnd(it.last + 1) },
                        scriptureColor,
                        plan.overrides,
                        darkSurface,
                        invalidScriptureSpans.map { plan.spanStart(it.first) until plan.spanEnd(it.last + 1) },
                        invalidScriptureColor,
                        paras = paras.toShown(plan)
                    ),
                    plan.mapping
                )
            }
        }
    }
    LaunchedEffect(findOpen, findCurrent, findMatches) {
        if (!findOpen) return@LaunchedEffect
        val m = findMatches.getOrNull(findCurrent) ?: return@LaunchedEffect
        val sl = synced ?: return@LaunchedEffect
        val len = sl.layout.layoutInput.text.length
        val line = sl.layout.getLineForOffset(sl.plan.toShown(m.first).coerceIn(0, len))
        runCatching {
            bodyBring.bringIntoView(
                Rect(0f, sl.layout.getLineTop(line), sl.layout.size.width.toFloat(), sl.layout.getLineBottom(line))
            )
        }
    }

    // Tap in the empty space below the text: the keyboard opens and shrinks the editor, so wait for it
    // to settle and then scroll the caret into view (otherwise the cursor stays hidden behind the keyboard).
    LaunchedEffect(revealTick) {
        if (revealTick == 0) return@LaunchedEffect
        // follow the keyboard animation: re-check a few times until the layout has settled
        repeat(4) {
            delay(150)
            val layout = bodyLayout ?: return@repeat
            val shown = planState.value.toShown(bodyValue.selection.end.coerceAtLeast(0))
                .coerceIn(0, layout.layoutInput.text.length)
            val r = layout.getCursorRect(shown)
            val pad = with(density) { 32.dp.toPx() }
            runCatching {
                bodyBring.bringIntoView(Rect(r.left, r.top - pad, r.right, r.bottom + pad))
            }
        }
    }

    // "More" menu in the top bar: find, share, copy, delete
    val moreMenu: @Composable () -> Unit = {
        Box {
            TipIconButton(label = "More", onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.widthIn(min = 216.dp),
                offset = DpOffset(0.dp, 8.dp),
                shape = MaterialTheme.shapes.largeIncreased,
            ) {
                MoreMenuItem(
                    label = "Find in note",
                    icon = Icons.Default.Search,
                    onClick = {
                        menuOpen = false
                        panel = Panel.None
                        findOpen = true
                    }
                )
                MoreMenuItem(
                    label = "Share",
                    icon = Icons.Default.Share,
                    enabled = canSave,
                    onClick = {
                        menuOpen = false
                        shareNote()
                    }
                )
                MoreMenuItem(
                    label = "Copy text",
                    icon = Icons.Default.ContentCopy,
                    enabled = canSave,
                    onClick = {
                        menuOpen = false
                        copyNote()
                    }
                )
                if (onDelete != null && noteId != null) {
                    MoreMenuItem(
                        label = "Delete note",
                        icon = Icons.Default.Delete,
                        color = colors.error,
                        onClick = {
                            menuOpen = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    var distanceToWindowBottomPx by remember { mutableStateOf(0) }
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars
    val keyboardGapTrimPx = with(density) { KeyboardGapTrim.roundToPx() }
    // Same value as before, but only READ while laying out (Modifier.bottomInset). Reading the keyboard
    // inset in composition recomposed this whole screen on every frame of the keyboard animation.
    val barBottomPx: () -> Int = {
        val ime = imeInsets.getBottom(density)
        val nav = navInsets.getBottom(density)
        val trim = if (ime > nav) keyboardGapTrimPx else 0
        (maxOf(ime, nav) - distanceToWindowBottomPx - trim).coerceAtLeast(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { c ->
                val bottom = c.positionInWindow().y + c.size.height
                distanceToWindowBottomPx = (view.rootView.height - bottom).toInt().coerceAtLeast(0)
            }
    ) {
        Scaffold(
            topBar = {
                StudyTopBar(
                    title = when {
                        !editing -> "Note"
                        noteId == null -> "New Note"
                        else -> "Edit Note"
                    },
                    navigationIcon = {
                        TipIconButton(label = "Back", onClick = { requestExit() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (editing) {
                            TipIconButton(
                                label = "Undo",
                                onClick = { undo() },
                                enabled = undoStack.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }
                            TipIconButton(
                                label = "Redo",
                                onClick = { redo() },
                                enabled = redoStack.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                            }
                            moreMenu()
                            // Primary action: filled tonal icon button (M3), so it stands out from Undo / Redo
                            TipTonalIconButton(
                                label = "Done",
                                onClick = { finishEditing() },
                                enabled = canSave
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Done")
                            }
                        } else {
                            TipTonalIconButton(label = "Edit", onClick = { enterEditMode() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit note")
                            }
                            moreMenu()
                        }
                        Spacer(Modifier.width(8.dp))
                    },
                )
            },
            containerColor = colors.surface,
            // We handle keyboard / nav-bar insets ourselves in the controls bar below.
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = findOpen,
                    enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()) +
                            fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()) +
                            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>())
                ) {
                    FindBar(
                        query = findQuery,
                        onQuery = { findQuery = it; findIndex = 0 },
                        count = findMatches.size,
                        index = findCurrent,
                        onPrev = {
                            if (findMatches.isNotEmpty()) {
                                findIndex = (findCurrent - 1 + findMatches.size) % findMatches.size
                            }
                        },
                        onNext = {
                            if (findMatches.isNotEmpty()) findIndex = (findCurrent + 1) % findMatches.size
                        },
                        onClose = { findOpen = false },
                        replaceText = if (editing) replaceText else null,
                        onReplaceText = { replaceText = it.replace("\n", " ") },
                        onReplace = { replaceMatches(false) },
                        onReplaceAll = { replaceMatches(true) }
                    )
                }

                // Scrollable editor area. The clickable sits BEFORE verticalScroll so it covers the
                // whole viewport: tapping any empty space focuses the note and opens the keyboard.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 640.dp) // tablets / foldables: keep a comfortable line length
                        .fillMaxWidth()
                        .clickable(
                            enabled = editing, // view mode: tapping empty space does nothing
                            interactionSource = emptyTapSource,
                            indication = null
                        ) { focusBodyAtEnd() }
                        .verticalScroll(rememberScrollState())
                ) {
                    val fieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.primary
                    )
                    val titleStyle = MaterialTheme.typography.headlineMediumEmphasized.copy(fontFamily = Lexend)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title - Enter / Next jumps into the body
                    TextField(
                        value = title,
                        onValueChange = {
                            if (it != title) pushUndo(coalesce = true)
                            title = it
                        },
                        placeholder = {
                            Text(
                                "Untitled Note",
                                style = titleStyle,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = titleStyle.copy(color = colors.onSurface),
                        singleLine = true,
                        readOnly = !editing,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusBodyAtEnd() }),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().revealOnEnter(index = 0)
                    )

                    // Date | category
                    Row(
                        modifier = Modifier.revealOnEnter(index = 1).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant
                        )
                        MetaDivider()
                        CategorySelector(
                            selected = category,
                            folderColor = folderColorOf(category),
                            enabled = editing,
                            onClick = { showFolderSheet = true }
                        )
                    }

                    AnimatedVisibility(
                        visible = wordCount > 0,
                        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()) +
                                fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
                        exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()) +
                                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>())
                    ) {
                        Text(
                            text = (if (wordCount == 1) "1 word" else "$wordCount words") +
                                    " \u00B7 " + (if (charCount == 1) "1 character" else "$charCount characters"),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Body - BasicTextField so tap positions map exactly onto the text layout.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        val selectionToolbar = remember { SelectionToolbarState() }
                        // Scripture inside the selection (e.g. "John 3:16") -> offer a verse lookup button
                        val selectedRef: ScriptureReference? = run {
                            val sel = bodyValue.selection
                            if (!selectionToolbar.isShown || sel.collapsed || sel.length > 120) null
                            else {
                                val t = bodyValue.text
                                val a = sel.min.coerceIn(0, t.length)
                                val b = sel.max.coerceIn(a, t.length)
                                ScriptureDetector.extractReferences(t.substring(a, b)).firstOrNull()
                                    ?.let { it.copy(startIndex = a + it.startIndex, endIndex = a + it.endIndex) }
                            }
                        }
                        CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides selectionToolbar) {
                            BasicTextField(
                                value = bodyValue,
                                onValueChange = { updateBody(it, byUser = true) },
                                readOnly = !editing,
                                textStyle = bodyTextStyle,
                                cursorBrush = SolidColor(colors.primary),
                                visualTransformation = styleTransformation,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                decorationBox = { inner ->
                                    Box(propagateMinConstraints = true) {
                                        if (editing && bodyValue.text.isEmpty()) {
                                            Text(
                                                "Start writing\u2026",
                                                style = bodyTextStyle,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        inner()
                                    }
                                },
                                onTextLayout = { l ->
                                    bodyLayout = l
                                    val p = planState.value
                                    // Only trust a layout that was made from the CURRENT plan's text.
                                    if (l.layoutInput.text.text == p.shown) {
                                        synced = SyncedLayout(
                                            l, p, placementsState.value.associate { it.idx to it.start }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 208.dp)
                                    .detectionGlow(detectionFx, scriptureColor) { ranges ->
                                        val sl = synced ?: return@detectionGlow null
                                        val len = sl.layout.layoutInput.text.length
                                        val path = Path()
                                        for (r in ranges) {
                                            val a = sl.plan.spanStart(r.first).coerceIn(0, len)
                                            val b = sl.plan.spanEnd(r.last + 1).coerceIn(0, len)
                                            if (b > a) path.addPath(sl.layout.getPathForRange(a, b))
                                        }
                                        path
                                    }
                                    .detectionShake(detectionFx, invalidScriptureColor) { ranges ->
                                        val sl = synced ?: return@detectionShake null
                                        val len = sl.layout.layoutInput.text.length
                                        val path = Path()
                                        for (r in ranges) {
                                            val a = sl.plan.spanStart(r.first).coerceIn(0, len)
                                            val b = sl.plan.spanEnd(r.last + 1).coerceIn(0, len)
                                            if (b > a) path.addPath(sl.layout.getPathForRange(a, b))
                                        }
                                        path
                                    }
                                    .onSizeChanged { bodyWidthPx = it.width }
                                    .onGloballyPositioned { selectionToolbar.anchor = it }
                                    .focusRequester(bodyFocus)
                                    .bringIntoViewRequester(bodyBring)
                                    // Hardware keyboard: Ctrl+B / I / U / Z / Y / F / S, Ctrl+Shift+L / E / R / J, Tab / Shift+Tab on lists
                                    .onPreviewKeyEvent { e ->
                                        if (!editing || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                        when {
                                            e.isCtrlPressed -> when (e.key) {
                                                Key.B -> { toggleFmt(FmtType.Bold); true }
                                                Key.I -> { toggleFmt(FmtType.Italic); true }
                                                Key.U -> { toggleFmt(FmtType.Underline); true }
                                                Key.Z -> { if (e.isShiftPressed) redo() else undo(); true }
                                                Key.Y -> { redo(); true }
                                                Key.F -> { panel = Panel.None; findOpen = true; true }
                                                Key.S -> { if (canSave) finishEditing(); true }
                                                // Ctrl+Shift+L / E / R / J = align left / centre / right / justify
                                                Key.L -> if (e.isShiftPressed) { setAlign(TextAlign.Start); true } else false
                                                Key.E -> if (e.isShiftPressed) { setAlign(TextAlign.Center); true } else false
                                                Key.R -> if (e.isShiftPressed) { setAlign(TextAlign.End); true } else false
                                                Key.J -> if (e.isShiftPressed) { setAlign(TextAlign.Justify); true } else false
                                                else -> false
                                            }
                                            e.key == Key.Tab && activeList != null -> {
                                                indentSelection(if (e.isShiftPressed) -1 else 1)
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                    // Keyed on Unit and reading the latest values through rememberUpdatedState:
                                    // the gesture coroutine is no longer cancelled + restarted on every keystroke.
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(
                                                requireUnconsumed = false,
                                                pass = PointerEventPass.Initial
                                            )
                                            val up = waitForUpOrCancellation(PointerEventPass.Initial)
                                                ?: return@awaitEachGesture
                                            if ((up.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                                return@awaitEachGesture
                                            }
                                            val layout = bodyLayout ?: return@awaitEachGesture
                                            val pos = up.position
                                            val shownOffset = layout.getOffsetForPosition(pos)
                                            val line = layout.getLineForOffset(shownOffset)
                                            val offset = planState.value.toOriginal(shownOffset)
                                            val onText = pos.y >= layout.getLineTop(line) &&
                                                    pos.y <= layout.getLineBottom(line) &&
                                                    pos.x >= layout.getLineLeft(line) &&
                                                    pos.x <= layout.getLineRight(line)
                                            if (!onText) return@awaitEachGesture

                                            // Tapping the box of a checklist item ticks / unticks it
                                            if (editingState.value) {
                                                val liveText = contentState.value
                                                val ols = lineStartOf(liveText, offset.coerceIn(0, liveText.length))
                                                val mk = markerOf(liveText.substring(ols, lineEndOf(liveText, ols)))
                                                if (mk != null && mk.kind == ListKind.Check) {
                                                    val pl = planState.value
                                                    val shownLen = layout.layoutInput.text.length
                                                    val firstLine = layout.getLineForOffset(
                                                        pl.toShown(ols).coerceAtMost(shownLen)
                                                    )
                                                    val textX = layout.getHorizontalPosition(
                                                        pl.toShown(ols + mk.length).coerceAtMost(shownLen), true
                                                    )
                                                    if (line == firstLine && pos.x <= textX) {
                                                        toggleCheckState.value(ols)
                                                        return@awaitEachGesture
                                                    }
                                                }
                                            }

                                            val span = scriptureSpansState.value.firstOrNull {
                                                offset >= it.first && offset <= it.last + 1
                                            } ?: return@awaitEachGesture
                                            // ranges are computed a moment after typing; ignore stale ones
                                            val liveContent = contentState.value
                                            if (span.last + 1 > liveContent.length) return@awaitEachGesture
                                            // Prefer the already-detected ref (correct absolute indices + confidence).
                                            // closest to the tapped character when several overlap the span
                                            val fromDetected = detectedReferencesState.value
                                                .filter { it.startIndex < span.last + 1 && it.endIndex > span.first }
                                                .minByOrNull {
                                                    when {
                                                        offset < it.startIndex -> it.startIndex - offset
                                                        offset > it.endIndex -> offset - it.endIndex
                                                        else -> 0
                                                    }
                                                }
                                            val ref = fromDetected ?: run {
                                                val raw = liveContent.substring(span.first, span.last + 1)
                                                ScriptureDetector.extractReferences(raw).firstOrNull()?.let {
                                                    // Re-extraction is relative to the span; map indices back.
                                                    it.copy(
                                                        startIndex = span.first + it.startIndex,
                                                        endIndex = span.first + it.endIndex
                                                    )
                                                }
                                            }
                                            if (ref != null) {
                                                focusManager.clearFocus()
                                                selectedReference = ref
                                            }
                                        }
                                    }
                            )
                        }
                        SelectionToolbar(
                            state = selectionToolbar,
                            editing = editing,
                            boldOn = isOn(FmtType.Bold),
                            italicOn = isOn(FmtType.Italic),
                            underlineOn = isOn(FmtType.Underline),
                            highlightArg = argOf(FmtType.Highlight)?.let { highlightColorOf(it).toArgb() },
                            onBold = { toggleFmt(FmtType.Bold) },
                            onItalic = { toggleFmt(FmtType.Italic) },
                            onUnderline = { toggleFmt(FmtType.Underline) },
                            onHighlight = { setFmt(FmtType.Highlight, it.toArgb(), false) },
                            onHighlightClear = { setFmt(FmtType.Highlight, 0, true) },
                            onClearFormatting = { clearFormatting() },
                            onScripture = selectedRef?.let { r ->
                                {
                                    focusManager.clearFocus()
                                    selectedReference = r
                                }
                            },
                            selection = bodyValue.selection
                        )

                        // Inline images are drawn over the space reserved for them.
                        // Positions come from `synced`, so layout and plan always belong together
                        // (no one-frame jump when the keyboard suggestion / deletion changes the text).
                        val s = synced
                        if (s != null) {
                            placements.forEach { p ->
                                val start = s.starts[p.idx] ?: return@forEach
                                val shownStart = s.plan.spanStart(start)
                                val textLen = s.layout.layoutInput.text.length
                                if (shownStart >= textLen) return@forEach
                                key(p.idx) {
                                    val line = s.layout.getLineForOffset(shownStart)
                                    val top = s.layout.getLineTop(line)
                                    val lineH = s.layout.getLineBottom(line) - top
                                    val anchor = s.plan.anchors[start]
                                    val y = if (anchor != null) {
                                        // text flows beside the picture: it starts level with the first text line
                                        val shownAnchor = s.plan.toShown(anchor).coerceAtMost(textLen)
                                        s.layout.getLineTop(s.layout.getLineForOffset(shownAnchor))
                                    } else {
                                        top + (lineH - p.heightPx) / 2f
                                    }
                                    val x = when (p.align) {
                                        0 -> 0f
                                        1 -> (bodyWidthPx - p.widthPx) / 2f
                                        else -> bodyWidthPx - p.widthPx
                                    }
                                    PlacedImage(
                                        name = p.name,
                                        description = if (editing) {
                                            "Image ${p.idx + 1}. Tap for options"
                                        } else {
                                            "Image ${p.idx + 1}. Tap to view full screen"
                                        },
                                        x = x,
                                        y = y,
                                        widthPx = p.widthPx,
                                        heightPx = p.heightPx,
                                        rounded = p.rounded,
                                        selected = editing && panel == Panel.Image && imagePanelIdx == p.idx,
                                        clickLabel = if (editing) "Image options" else "View full screen",
                                        onClick = {
                                            if (editing) selectImage(p.idx)
                                            else viewerImage = images.getOrNull(p.idx)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Images still being copied in
                    if (importing > 0) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(importing) { ImagePlaceholder() }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                }

                // Detected scriptures: one button pinned at the bottom (not at the end of the text).
                // Hidden while the keyboard is open so it never sits on top of the typing area.
                AnimatedVisibility(
                    visible = detectedReferences.isNotEmpty() && !imeVisible,
                    enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()) +
                            scaleIn(MaterialTheme.motionScheme.fastSpatialSpec<Float>(), initialScale = 0.8f) +
                            fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec<Float>()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()) +
                            scaleOut(MaterialTheme.motionScheme.fastSpatialSpec<Float>(), targetScale = 0.9f) +
                            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Same button language as the Add-hours sheet: tonal fill, labelLarge text,
                        // plus a small primary badge that carries the count.
                        FilledTonalButton(
                            onClick = { focusManager.clearFocus(); showScriptureList = true },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .height(56.dp)
                                .detectionPop(detectionFx)
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(ButtonDefaults.IconSize)
                                    .detectionIconTilt(detectionFx)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Detected scriptures", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            // Outer box is NOT clipped so the halo ring can expand past the badge.
                            Box(
                                modifier = Modifier.detectionHalo(detectionFx, colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 22.dp)
                                        .widthIn(min = 22.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                        .padding(horizontal = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RollingCount(count = detectedReferences.size) { n ->
                                        Text(
                                            "$n",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ------------------------- Controls bar (+ format panel) -------------------------
                if (editing) Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            MaterialTheme.shapes.largeIncreased.copy(
                                bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
                                bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
                            )
                        )
                        .background(colors.surfaceContainer)
                        .bottomInset(barBottomPx)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filled with the theme's primary (the app's dark green), like Save / Close.
                        Button(
                            onClick = { showAddScripture = true },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            contentPadding = PaddingValues(start = 12.dp, end = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Add Scripture")
                        }
                        Tip("Text format") {
                            BarToggle(
                                checked = panel == Panel.Format,
                                onClick = { togglePanel(Panel.Format) }
                            ) {
                                Text(
                                    "Aa",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = if (panel == Panel.Format) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        Tip("Align and lists") {
                            BarToggle(
                                checked = panel == Panel.Align,
                                onClick = { togglePanel(Panel.Align) }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Align and lists")
                            }
                        }
                        Tip("Add image") {
                            BarToggle(
                                checked = panel == Panel.Image,
                                onClick = {
                                    if (panel == Panel.Image) {
                                        togglePanel(Panel.Image) // close the image panel
                                    } else {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Add image")
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = panel != Panel.None,
                        enter = expandVertically(Motion.spatialDefault<androidx.compose.ui.unit.IntSize>()) +
                                fadeIn(Motion.effectsFast),
                        exit = shrinkVertically(Motion.spatialFast<androidx.compose.ui.unit.IntSize>()) +
                                fadeOut(Motion.effectsFast)
                    ) {
                        AnimatedContent(
                            targetState = lastPanel,
                            transitionSpec = { MotionTransitions.fadeThrough() },
                            label = "panelSwap"
                        ) { shownPanel ->
                            when (shownPanel) {
                                Panel.Align -> AlignPanel(
                                    align = caretLineAlign,
                                    onAlign = { setAlign(it) },
                                    activeList = activeList,
                                    onList = { applyList(it) },
                                    onIndent = { indentSelection(it) }
                                )
                                Panel.Image -> {
                                    val idx = imagePanelIdx
                                    val sp = spans.firstOrNull {
                                        it.type == FmtType.Image && imgIdx(it.arg) == idx
                                    }
                                    if (idx == null || sp == null) {
                                        Spacer(Modifier.fillMaxWidth().height(PanelHeight))
                                    } else {
                                        ImagePanel(
                                            pct = imgPct(sp.arg),
                                            align = imgAlign(sp.arg),
                                            rounded = imgRounded(sp.arg),
                                            wrap = imgWrap(sp.arg),
                                            wrapEnabled = lineAligns.getOrElse(
                                                lineIndexAt(content, sp.end + 1)
                                            ) { 0 } == 0,
                                            onPct = { updateImage(idx, pct = it) },
                                            onAlign = { a ->
                                                updateImage(idx, align = a, wrap = if (a == 1) false else null)
                                            },
                                            onRounded = { updateImage(idx, rounded = it) },
                                            onWrap = { on ->
                                                updateImage(
                                                    idx,
                                                    wrap = on,
                                                    pct = if (on && imgPct(sp.arg) > 55) 45 else null,
                                                    align = if (on && imgAlign(sp.arg) == 1) 0 else null
                                                )
                                            },
                                            onRotate = { rotateImage(idx) },
                                            onView = { viewerImage = images.getOrNull(idx) },
                                            onRemove = { pendingRemoveIdx = idx }
                                        )
                                    }
                                }
                                else -> FormatPanel(
                                    isOn = ::isOn,
                                    onToggle = ::toggleFmt,
                                    sizeSelected = TextSize.entries
                                        .getOrNull(argOf(FmtType.Size) ?: 0) ?: TextSize.Normal,
                                    onSize = { setFmt(FmtType.Size, it.ordinal, it == TextSize.Normal) },
                                    colorSelected = argOf(FmtType.Color) ?: InkBlack.toArgb(),
                                    onColor = { setFmt(FmtType.Color, it.toArgb(), it == InkBlack) },
                                    highlightSelected = argOf(FmtType.Highlight)?.let { highlightColorOf(it).toArgb() },
                                    onHighlight = { setFmt(FmtType.Highlight, it.toArgb(), false) },
                                    onHighlightClear = { setFmt(FmtType.Highlight, 0, true) },
                                    onClear = { clearFormatting() }
                                )
                            }
                        }
                    }
                } else {
                    // View mode has no controls bar: keep the text clear of the nav bar / keyboard
                    Spacer(Modifier.bottomInset(barBottomPx))
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .bottomInset(barBottomPx, extraPx = with(density) { (if (editing) 64.dp else 16.dp).roundToPx() })
        )
    } // end root Box

    if (showAddScripture) {
        // Full-screen book -> chapter -> verse picker. The chosen verse is typed into the note
        // as plain text, e.g. "Proverbs 1:2" (or "సామెతలు 1:2" with a Telugu Bible).
        BookBrowserDialog(
            onClose = { showAddScripture = false },
            onVersePicked = { reference ->
                showAddScripture = false
                insertScripture(reference)
                runCatching { bodyFocus.requestFocus() }
                keyboard?.show()
            }
        )
    }

    if (showDiscardDialog) {
        // Two actions (Material 3 dialog rule). Tapping outside / Back = keep editing.
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Save changes?", fontFamily = Lexend) },
            text = {
                Text(
                    "You have unsaved changes in this note. Save them, or discard them and leave. " +
                            "Tap outside this box to keep editing.",
                    fontFamily = Lexend
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        discardAndExit()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("Discard", fontFamily = Lexend) }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        saveAndExit()
                    },
                    enabled = canSave
                ) { Text("Save", fontFamily = Lexend) }
            }
        )
    }

    if (showFolderSheet) {
        FolderSheet(
            folders = categoryOptions,
            selected = category,
            colorOf = folderColorOf,
            onSelect = { if (category != it) pushUndo(); category = it },
            onNewFolder = { showNewCategory = true },
            onDismiss = { showFolderSheet = false }
        )
    }

    if (showNewCategory) {
        NewFolderDialog(
            existing = categoryOptions,
            onDismiss = { showNewCategory = false },
            onCreate = { name, color ->
                pushUndo()
                customCategories = customCategories + name
                customFolderColors = customFolderColors + "$name\u001F${color.toArgb()}"
                category = name
                showNewCategory = false
                FolderRepository.add(name, color.toArgb())      // saved to disk
                onCreateFolder(name, color.toArgb())
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete this note?", fontFamily = Lexend) },
            text = {
                Text(
                    "The note and its images will be deleted permanently.",
                    fontFamily = Lexend
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontFamily = Lexend)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val id = noteId
                        if (id != null) {
                            // images imported in this session but never saved
                            sessionAdded.forEach { NoteImages.delete(it) }
                            onDelete?.invoke(id)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("Delete", fontFamily = Lexend) }
            }
        )
    }

    pendingRemoveIdx?.let { idx ->
        AlertDialog(
            onDismissRequest = { pendingRemoveIdx = null },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Remove image?", fontFamily = Lexend) },
            text = {
                Text(
                    "The image will be removed from this note. You can bring it back with Undo.",
                    fontFamily = Lexend
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveIdx = null }) {
                    Text("Cancel", fontFamily = Lexend)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoveIdx = null
                        confirmRemoveImage(idx)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) { Text("Remove", fontFamily = Lexend) }
            }
        )
    }

    viewerImage?.let { name ->
        ImageViewer(name = name, onDismiss = { viewerImage = null })
    }

    if (showScriptureList && detectedReferences.isNotEmpty()) {
        DetectedScripturesScreen(
            references = detectedReferences,
            onClose = { showScriptureList = false },
            onOpen = { selectedReference = it }
        )
    }

    /**
     * The quote block that belongs to [ref], or null when its verse text is not in the note.
     * Quotes sit on their own line(s) right under the line that holds the reference. Tapping the
     * reference *inside* a quote also finds that quote.
     */
    fun quoteSpanFor(ref: ScriptureReference): FormatSpan? {
        val text = bodyValue.text
        val key = ref.localizedKey.hashCode()
        val at = ref.startIndex.coerceIn(0, text.length)
        spans.firstOrNull { it.type == FmtType.Quote && it.arg == key && at >= it.start && at < it.end }
            ?.let { return it }
        var pos = lineEndOf(text, ref.endIndex.coerceIn(0, text.length))
        while (pos < text.length && text[pos] == '\n') {
            val sp = spans.firstOrNull { it.type == FmtType.Quote && it.start == pos + 1 } ?: return null
            if (sp.arg == key) return sp
            pos = sp.end
        }
        return null
    }

    selectedReference?.let { ref ->
        ScriptureBottomSheet(
            reference = ref,
            onDismiss = { selectedReference = null },
            isInNote = quoteSpanFor(ref) != null,
            onInsertQuote = { r, quote ->
                val text = bodyValue.text
                // after the reference's line, below any quotes already added for it
                var pos = lineEndOf(text, r.endIndex.coerceIn(0, text.length))
                while (pos < text.length && text[pos] == '\n') {
                    val sp = spans.firstOrNull { it.type == FmtType.Quote && it.start == pos + 1 } ?: break
                    pos = sp.end
                }
                val insert = "\n" + quote
                pushUndo()
                val (t, shifted) = applyEdits(text, spans, listOf(TextEdit(pos, 0, insert)))
                var next = shifted
                // the quote starts plain: no bold / colour / highlight leaking in from the line above
                textFmtTypes.forEach { next = next.subtract(it, pos, pos + insert.length) }
                spans = next + FormatSpan(pos + 1, pos + insert.length, FmtType.Quote, r.localizedKey.hashCode())
                lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, pos), t)
                bodyValue = TextFieldValue(t, TextRange(pos + insert.length))
                selectedReference = null
            },
            onRemoveQuote = { r ->
                quoteSpanFor(r)?.let { sp ->
                    val text = bodyValue.text
                    val from = (sp.start - 1).coerceAtLeast(0) // also drop the line break before it
                    val to = sp.end.coerceAtMost(text.length)
                    pushUndo()
                    val (t, shifted) = applyEdits(text, spans, listOf(TextEdit(from, to - from, "")))
                    spans = shifted
                    lineAligns = fitAligns(shiftLineAligns(lineAligns, text, t, from), t)
                    bodyValue = TextFieldValue(t, TextRange(from.coerceAtMost(t.length)))
                }
                selectedReference = null
            },
            onCorrectName = { start, end, corrected ->
                val text = bodyValue.text
                val s = start.coerceIn(0, text.length)
                val e = end.coerceIn(s, text.length)
                if (s < e && text.substring(s, e) != corrected) {
                    pushUndo()
                    val next = text.replaceRange(s, e, corrected)
                    // updateBody shifts format spans for the text change.
                    updateBody(
                        TextFieldValue(next, TextRange(s + corrected.length)),
                        byUser = false
                    )
                }
                selectedReference = null
            }
        )
    }
}

/** One row of the top-bar "More" menu: same look as the Sort menu (Lexend, 14dp rounded row, 8dp inset). */
@Composable
private fun MoreMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
) {
    val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    DropdownMenuItem(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.medium),
        text = {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = Lexend,
                color = if (enabled) tint else tint.copy(alpha = 0.38f),
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else tint.copy(alpha = 0.38f))
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun MetaDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outline)
    )
}
