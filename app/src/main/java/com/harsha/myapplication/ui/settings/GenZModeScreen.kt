@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.harsha.myapplication.data.Gender
import com.harsha.myapplication.data.GenZPrefsRepository
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale
import com.harsha.myapplication.ui.theme.rememberAppFeedback
import com.harsha.myapplication.ui.theme.revealOnEnter

// ── Design tokens (same as About / Backup) ─────────────────────────────────
private val ScreenMargin = 16.dp
private val SectionGap = 28.dp
private val CardRadius = 28.dp
private val GroupInner = 4.dp
private val GroupGap = 2.dp
private const val MAX_NAME_LENGTH = 30

private val ColorEffects = spring<Color>(dampingRatio = 1f, stiffness = 1600f)

private data class GenZTypeRow(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val shape: Shape,
    val checked: Boolean,
    val onChecked: (Boolean) -> Unit,
)

// ── Remembers name + gender even after Gen-Z mode is switched off ──────────
// The repository may clear the profile on disable(), so we keep our own copy.
// Turning the mode back on re-uses it and skips the setup dialog.
private object GenZProfileMemory {
    private const val FILE = "genz_profile_memory"
    private const val K_NAME = "name"
    private const val K_GENDER = "gender"

    fun save(context: Context, name: String, gender: Gender) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(K_NAME, name.trim())
            .putString(K_GENDER, gender.name)
            .apply()
    }

    fun name(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_NAME, null).orEmpty()

    fun gender(context: Context): Gender? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_GENDER, null)
            ?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
}

// ───────────────────────────── Screen ─────────────────────────────
@Composable
fun GenZModeScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val feedback = rememberAppFeedback()
    remember(context) { GenZPrefsRepository.init(context); true }
    val prefs = GenZPrefsRepository.prefs

    var showSetup by rememberSaveable { mutableStateOf(false) }

    // Saved profile: repository first, our own memory as fallback.
    fun savedName(): String = prefs.name.ifBlank { GenZProfileMemory.name(context) }
    fun savedGender(): Gender? = prefs.gender ?: GenZProfileMemory.gender(context)

    fun turnOn() {
        val name = savedName()
        val gender = savedGender()
        if (name.isNotBlank() && gender != null) {
            // Already set up once → just switch on, no questions asked.
            GenZPrefsRepository.enable(name, gender)
        } else {
            showSetup = true
        }
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Gen-Z Mode",
                navigationIcon = {
                    StudyBarIconButton(
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                },
            )
        },
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            // ── Master card (toggle + profile) ──
            Box(Modifier.revealOnEnter(index = 0, playOnceKey = "genz-0")) {
                GenZHeroCard(
                    enabled = prefs.enabled,
                    name = prefs.name,
                    genderLabel = prefs.gender?.label,
                    onToggle = { on ->
                        feedback.select()
                        if (on) turnOn() else GenZPrefsRepository.disable()
                    },
                    onEdit = { feedback.click(); showSetup = true },
                )
            }

            AnimatedVisibility(
                visible = prefs.enabled,
                enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>()),
            ) {
                Column {
                    // ── Notification types ──
                    Spacer(Modifier.height(SectionGap))
                    Column(Modifier.revealOnEnter(index = 2, playOnceKey = "genz-2")) {
                        SectionHeader("Notifications")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Switch off a type to keep its normal, formal wording. " +
                                "Tone follows your gender.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(Modifier.height(12.dp))

                        val t = prefs.types
                        val rows = listOf(
                            GenZTypeRow(
                                "Daily summary", "Morning recap of today’s visits",
                                Icons.Outlined.Today, MaterialShapes.Cookie9Sided.toShape(), t.dailySummary
                            ) { on -> GenZPrefsRepository.updateTypes { it.copy(dailySummary = on) } },
                            GenZTypeRow(
                                "Daily text", "Reminder to read today’s text",
                                Icons.Outlined.Book, MaterialShapes.Clover4Leaf.toShape(), t.dailyText
                            ) { on -> GenZPrefsRepository.updateTypes { it.copy(dailyText = on) } },
                            GenZTypeRow(
                                "Visit reminders", "Before and at visit time",
                                Icons.Outlined.Schedule, MaterialShapes.Cookie6Sided.toShape(), t.visitReminder
                            ) { on -> GenZPrefsRepository.updateTypes { it.copy(visitReminder = on) } },
                            GenZTypeRow(
                                "After a visit", "Nudge to log details",
                                Icons.Outlined.EventAvailable, MaterialShapes.Flower.toShape(), t.afterVisit
                            ) { on -> GenZPrefsRepository.updateTypes { it.copy(afterVisit = on) } },
                            GenZTypeRow(
                                "Overdue follow-up", "When a visit still needs an update",
                                Icons.Outlined.WarningAmber, MaterialShapes.SoftBurst.toShape(), t.overdue
                            ) { on -> GenZPrefsRepository.updateTypes { it.copy(overdue = on) } },
                            GenZTypeRow(
                                "Weekly summary", "Hours, return visits and studies",
                                Icons.Outlined.DateRange, MaterialShapes.Cookie7Sided.toShape(), t.weeklySummary
                            ) { on -> GenZPrefsRepository.updateTypes { it.copy(weeklySummary = on) } },
                        )

                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(GroupGap)
                        ) {
                            rows.forEachIndexed { index, row ->
                                GenZTypeToggleRow(
                                    row = row,
                                    index = index,
                                    count = rows.size,
                                    onToggle = { on -> feedback.select(); row.onChecked(on) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSetup) {
        GenZSetupSheet(
            isEditing = prefs.enabled,
            initialName = savedName(),
            initialGender = savedGender(),
            onDismiss = { showSetup = false },
            onConfirm = { name, gender ->
                val clean = name.trim()
                GenZProfileMemory.save(context, clean, gender)
                if (prefs.enabled) GenZPrefsRepository.updateProfile(clean, gender)
                else GenZPrefsRepository.enable(clean, gender)
                feedback.confirm()
            }
        )
    }
}

// ───────────────────────────── Building blocks ─────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun rememberGroupShape(index: Int, count: Int, innerTarget: Dp): Shape {
    val inner by animateDpAsState(innerTarget, Motion.spatialFastDp, label = "groupInner")
    val safeInner = inner.coerceAtLeast(0.dp)
    val top = if (index == 0) CardRadius else safeInner
    val bottom = if (index == count - 1) CardRadius else safeInner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun ShapeTile(
    shape: Shape,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Master card, same layout as the Notifications page (cookie tile · title · switch).
 * New: when on, a profile pill slides open inside the card ("Hey Harsha · Male" + edit),
 * so the name/gender live with the switch and the title stays on one line.
 */
@Composable
private fun GenZHeroCard(
    enabled: Boolean,
    name: String,
    genderLabel: String?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val container by animateColorAsState(
        if (enabled) colors.primaryContainer else colors.surfaceContainerHigh,
        Motion.effectsDefault(), label = "heroContainer"
    )
    val content by animateColorAsState(
        if (enabled) colors.onPrimaryContainer else colors.onSurface,
        Motion.effectsDefault(), label = "heroContent"
    )
    val tile by animateColorAsState(
        if (enabled) colors.primary else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "heroTile"
    )
    val tileContent by animateColorAsState(
        if (enabled) colors.onPrimary else colors.onSurfaceVariant,
        Motion.effectsDefault(), label = "heroTileContent"
    )

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 24.dp else 32.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "heroCorner"
    )

    Surface(
        onClick = { onToggle(!enabled) },
        interactionSource = source,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.98f)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShapeTile(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    container = tile,
                    size = 56.dp
                ) {
                    Crossfade(targetState = enabled, label = "heroIcon") { on ->
                        Icon(
                            if (on) Icons.Outlined.ElectricBolt else Icons.Outlined.PowerSettingsNew,
                            contentDescription = null,
                            tint = tileContent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Short title + single line: can never wrap.
                    Text(
                        "Gen-Z mode",
                        style = typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (enabled) "Casual, fun tones are on" else "Casual, fun tones are off",
                        style = typography.bodyMedium,
                        color = content.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    thumbContent = {
                        Icon(
                            if (enabled) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(Motion.effectsDefault) + expandVertically(Motion.spatialDefault<IntSize>()),
                exit = fadeOut(Motion.effectsFast) + shrinkVertically(Motion.spatialFast<IntSize>()),
            ) {
                // Profile pill
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colors.surfaceContainerLowest.copy(alpha = 0.6f),
                    contentColor = colors.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Row(
                        Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShapeTile(
                            shape = MaterialShapes.Clover4Leaf.toShape(),
                            container = colors.primary,
                            size = 36.dp
                        ) {
                            Text(
                                name.trim().firstOrNull()?.uppercase() ?: "?",
                                style = typography.labelLargeEmphasized,
                                color = colors.onPrimary
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Hey ${name.ifBlank { "there" }}",
                                style = typography.titleSmallEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                genderLabel ?: "Gender not set",
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        FilledTonalIconButton(onClick = onEdit, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit name and gender")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenZTypeToggleRow(
    row: GenZTypeRow,
    index: Int,
    count: Int,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = rememberGroupShape(index, count, if (pressed) 20.dp else GroupInner)

    val tile by animateColorAsState(
        if (row.checked) colors.secondaryContainer else colors.surfaceContainerHighest,
        ColorEffects, label = "typeTile"
    )
    val tileContent by animateColorAsState(
        if (row.checked) colors.onSecondaryContainer else colors.onSurfaceVariant,
        ColorEffects, label = "typeTileContent"
    )

    Surface(
        onClick = { onToggle(!row.checked) },
        interactionSource = source,
        shape = shape,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source, pressedScale = 0.98f)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    row.title,
                    style = typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    row.subtitle,
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                ShapeTile(shape = row.shape, container = tile, size = 44.dp) {
                    Icon(
                        row.icon,
                        contentDescription = null,
                        tint = tileContent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            trailingContent = {
                Switch(
                    checked = row.checked,
                    onCheckedChange = onToggle,
                    thumbContent = {
                        Icon(
                            if (row.checked) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    },
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ───────────────────────────── Setup bottom sheet ─────────────────────────────

@Composable
private fun fieldColors(): TextFieldColors {
    val c = MaterialTheme.colorScheme
    return TextFieldDefaults.colors(
        unfocusedContainerColor = c.surfaceContainerHighest,
        focusedContainerColor = c.surfaceContainerLowest,
        errorContainerColor = c.surfaceContainerHighest,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = c.primary,
        focusedLeadingIconColor = c.primary,
    )
}

@Composable
private fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp),
    )
}

/**
 * Same filled field as the Person edit screen (animated focus ring, leading icon, clear button),
 * but with a soft, smaller placeholder instead of a floating label.
 */
@Composable
private fun ExpressiveField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ring by animateColorAsState(
        targetValue = if (focused) colors.primary else colors.outlineVariant,
        animationSpec = ColorEffects,
        label = "fieldRing",
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, ring, shape),
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = colors.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(
                visible = focused && value.isNotEmpty(),
                enter = scaleIn(Motion.spatialFast) + fadeIn(Motion.effectsDefault),
                exit = scaleOut(Motion.spatialFast) + fadeOut(Motion.effectsDefault),
            ) {
                IconButton(onClick = { onValueChange("") }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Default.Close, contentDescription = "Clear name")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        interactionSource = interaction,
        shape = shape,
        colors = fieldColors(),
    )
}

/** Connected expressive button group (shared edges, corners morph when checked). */
@Composable
private fun ConnectedToggleGroup(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        labels.forEachIndexed { index, label ->
            val shapes = when {
                labels.size == 1 -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = index == selectedIndex,
                onCheckedChange = { focus.clearFocus(); keyboard?.hide(); onSelect(index) },
                modifier = Modifier.weight(1f),
                shapes = shapes,
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Name + gender setup as a modal bottom sheet (replaces the old popup).
 * Buttons follow the Person edit screen: medium expressive Button with a tick that slides in
 * once the form is valid.
 */
@Composable
private fun GenZSetupSheet(
    isEditing: Boolean,
    initialName: String,
    initialGender: Gender?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, gender: Gender) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf(initialName) }
    var gender by rememberSaveable { mutableStateOf(initialGender) }
    val valid = name.isNotBlank() && gender != null
    val options = Gender.values()
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    fun submit() {
        if (valid) {
            onConfirm(name, gender!!)
            close()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Header ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShapeTile(
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    container = colors.primary,
                    size = 56.dp
                ) {
                    Icon(
                        Icons.Outlined.ElectricBolt,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (isEditing) "Edit your profile" else "Let’s set you up",
                        style = typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "One time only. We’ll remember it.",
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            // ── Name ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupLabel("Name")
                ExpressiveField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                    placeholder = "What should we call you?",
                    icon = Icons.Outlined.Person,
                    onDone = { submit() },
                )
            }

            // ── Tone ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupLabel("Notification tone")
                ConnectedToggleGroup(
                    labels = options.map { it.label },
                    selectedIndex = options.indexOf(gender),
                    onSelect = { gender = options[it] },
                )
            }

            // ── Actions ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = { close() },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
                ) {
                    Text("Cancel", style = ButtonDefaults.textStyleFor(buttonHeight), maxLines = 1)
                }
                Button(
                    onClick = { submit() },
                    enabled = valid,
                    modifier = Modifier
                        .weight(2f)
                        .heightIn(min = buttonHeight),
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
                ) {
                    AnimatedVisibility(
                        visible = valid,
                        enter = expandHorizontally(Motion.spatialFast()) + fadeIn(Motion.effectsDefault),
                        exit = shrinkHorizontally(Motion.spatialFast()) + fadeOut(Motion.effectsDefault),
                    ) {
                        Row {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
                            )
                            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
                        }
                    }
                    Text(
                        if (isEditing) "Save" else "Turn on",
                        style = ButtonDefaults.textStyleFor(buttonHeight),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
