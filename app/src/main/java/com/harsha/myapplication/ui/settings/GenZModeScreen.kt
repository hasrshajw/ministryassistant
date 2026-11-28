@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.data.Gender
import com.harsha.myapplication.data.GenZPrefsRepository
import com.harsha.myapplication.data.GenZTypeFlags
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.theme.Motion
import com.harsha.myapplication.ui.theme.pressScale as themePressScale
import com.harsha.myapplication.ui.theme.revealOnEnter

private val ScreenMargin = 16.dp
private val MasterCorner = 32.dp
private const val MAX_NAME_LENGTH = 30

private data class GenZTypeRow(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val checked: Boolean,
    val onChecked: (Boolean) -> Unit,
)

/**
 * Gen-Z mode: master toggle + name/gender, then per-notification-type switches
 * so the user can choose which alerts use Gen-Z tone (male/female) and which stay plain.
 */
@Composable
fun GenZModeScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val context = LocalContext.current
    remember(context) { GenZPrefsRepository.init(context); true }
    val prefs = GenZPrefsRepository.prefs

    var showSetup by rememberSaveable { mutableStateOf(false) }

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
                .padding(horizontal = ScreenMargin, vertical = 8.dp)
        ) {
            GenZToggleCard(
                enabled = prefs.enabled,
                subtitle = when {
                    !prefs.enabled -> "Turn on for fun, casual notification tones"
                    prefs.gender != null -> "Hey ${prefs.name} · ${prefs.gender.label}"
                    else -> "Hey ${prefs.name}"
                },
                onToggle = { turnOn ->
                    if (turnOn) showSetup = true
                    else GenZPrefsRepository.disable()
                },
                modifier = Modifier.revealOnEnter(index = 0, playOnceKey = "genz-toggle")
            )

            // Edit name / gender when already on
            if (prefs.enabled) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showSetup = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Edit name & gender", style = typography.labelLarge)
                }
            }

            AnimatedVisibility(
                visible = prefs.enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Which notifications use Gen-Z?",
                        style = typography.titleMediumEmphasized,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Turn off any type to keep the normal, formal wording for that alert. Tone follows your gender (Male / Female).",
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))

                    val t = prefs.types
                    val rows = listOf(
                        GenZTypeRow(
                            "Daily summary",
                            "Morning recap of today’s visits",
                            Icons.Outlined.Today,
                            t.dailySummary,
                        ) { on -> GenZPrefsRepository.updateTypes { it.copy(dailySummary = on) } },
                        GenZTypeRow(
                            "Daily text",
                            "Reminder to read today’s text",
                            Icons.Outlined.Book,
                            t.dailyText,
                        ) { on -> GenZPrefsRepository.updateTypes { it.copy(dailyText = on) } },
                        GenZTypeRow(
                            "Visit reminders",
                            "Before and at visit time",
                            Icons.Outlined.Schedule,
                            t.visitReminder,
                        ) { on -> GenZPrefsRepository.updateTypes { it.copy(visitReminder = on) } },
                        GenZTypeRow(
                            "After a visit",
                            "Nudge to log details",
                            Icons.Outlined.EventAvailable,
                            t.afterVisit,
                        ) { on -> GenZPrefsRepository.updateTypes { it.copy(afterVisit = on) } },
                        GenZTypeRow(
                            "Overdue follow-up",
                            "When a visit still needs an update",
                            Icons.Outlined.WarningAmber,
                            t.overdue,
                        ) { on -> GenZPrefsRepository.updateTypes { it.copy(overdue = on) } },
                        GenZTypeRow(
                            "Weekly summary",
                            "Hours, return visits and studies",
                            Icons.Outlined.DateRange,
                            t.weeklySummary,
                        ) { on -> GenZPrefsRepository.updateTypes { it.copy(weeklySummary = on) } },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        rows.forEachIndexed { index, row ->
                            GenZTypeToggleRow(
                                row = row,
                                first = index == 0,
                                last = index == rows.lastIndex,
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showSetup) {
        GenZSetupDialog(
            initialName = prefs.name,
            initialGender = prefs.gender,
            onDismiss = { showSetup = false },
            onConfirm = { name, gender ->
                if (prefs.enabled) GenZPrefsRepository.updateProfile(name, gender)
                else GenZPrefsRepository.enable(name, gender)
                showSetup = false
            }
        )
    }
}

@Composable
private fun GenZTypeToggleRow(
    row: GenZTypeRow,
    first: Boolean,
    last: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val top = if (first) 20.dp else 4.dp
    val bottom = if (last) 20.dp else 4.dp
    Surface(
        shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(row.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(row.title, style = typography.titleSmall)
                Text(row.subtitle, style = typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Switch(
                checked = row.checked,
                onCheckedChange = row.onChecked,
                thumbContent = {
                    Icon(
                        if (row.checked) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun GenZToggleCard(
    enabled: Boolean,
    subtitle: String,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val container by animateColorAsState(
        if (enabled) colors.primaryContainer else colors.surfaceContainerHigh,
        Motion.effectsDefault(), label = "genZContainer"
    )
    val content by animateColorAsState(
        if (enabled) colors.onPrimaryContainer else colors.onSurface,
        Motion.effectsDefault(), label = "genZContent"
    )
    val tile by animateColorAsState(
        if (enabled) colors.primary else colors.surfaceContainerHighest,
        Motion.effectsDefault(), label = "genZTile"
    )
    val tileContent by animateColorAsState(
        if (enabled) colors.onPrimary else colors.onSurfaceVariant,
        Motion.effectsDefault(), label = "genZTileContent"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        if (pressed) 24.dp else MasterCorner,
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "genZCorner"
    )

    Surface(
        onClick = { onToggle(!enabled) },
        interactionSource = interaction,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        modifier = modifier
            .fillMaxWidth()
            .themePressScale(interaction, pressedScale = 0.98f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = tile,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Crossfade(targetState = enabled, label = "genZIcon") { on ->
                        Icon(
                            if (on) Icons.Outlined.ElectricBolt else Icons.Outlined.PowerSettingsNew,
                            contentDescription = null,
                            tint = tileContent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Enable Gen-Z mode", style = typography.titleLargeEmphasized)
                Text(
                    subtitle,
                    style = typography.bodyMedium,
                    color = content.copy(alpha = 0.8f),
                    maxLines = 2,
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
    }
}

@Composable
private fun GenZSetupDialog(
    initialName: String,
    initialGender: Gender?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, gender: Gender) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var name by rememberSaveable { mutableStateOf(initialName) }
    var gender by rememberSaveable { mutableStateOf(initialGender) }
    val valid = name.isNotBlank() && gender != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Let's set you up", style = typography.headlineSmallEmphasized) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                    label = { Text("Your name / nickname") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Gender (sets Male / Female notification tone)", style = typography.labelLarge, color = colors.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = Gender.values()
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = gender == option,
                            onClick = { gender = option },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            label = { Text(option.label, maxLines = 1) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, gender!!) },
                enabled = valid
            ) { Text("Save", style = typography.labelLargeEmphasized) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = typography.labelLarge) }
        },
        shape = MaterialTheme.shapes.extraLargeIncreased,
        containerColor = colors.surfaceContainerHigh
    )
}
