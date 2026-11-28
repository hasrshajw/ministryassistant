@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.security

import androidx.compose.animation.AnimatedContent
import com.harsha.myapplication.ui.theme.ShapeLargeIncreased
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.harsha.myapplication.data.LockTimeout
import com.harsha.myapplication.data.PinCheck
import com.harsha.myapplication.data.PinKind
import com.harsha.myapplication.data.SecurityRepository
import com.harsha.myapplication.ui.components.StudyBarIconButton
import com.harsha.myapplication.ui.components.StudyTopBar
import com.harsha.myapplication.ui.theme.Motion

private enum class Route { Home, Enable, VerifyDisable, VerifyChangePin, VerifyChangeRecovery, ChangePin, ChangeRecovery }

/** Settings > Security: turn the PIN lock on or off, pick the timeout, change the PIN or recovery question. */
@Composable
fun SecuritySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var route by remember { mutableStateOf(Route.Home) }

    // Back from a sub-step returns to the list first.
    BackHandler(enabled = route != Route.Home) { route = Route.Home }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            (fadeIn(Motion.effectsDefault()) + scaleIn(Motion.spatialDefault(), initialScale = 0.94f)) togetherWith
                fadeOut(Motion.effectsFast())
        },
        label = "securityRoute",
    ) { r ->
        when (r) {
            Route.Home -> SecurityHome(
                onBack = onBack,
                onEnable = { route = Route.Enable },
                onDisable = { route = Route.VerifyDisable },
                onChangePin = { route = Route.VerifyChangePin },
                onChangeRecovery = { route = Route.VerifyChangeRecovery },
            )
            Route.Enable -> PinSetupFlow(
                askQuestion = true,
                onFinished = { pin, q, a ->
                    if (SecurityRepository.enable(context, pin, q!!, a!!)) null else "Could not turn the lock on. Try again."
                },
                onDone = { route = Route.Home },
            )
            Route.VerifyDisable -> VerifyCurrentPin("Turn off app lock") {
                SecurityRepository.disable(context); route = Route.Home
            }
            Route.VerifyChangePin -> VerifyCurrentPin("Change PIN") { route = Route.ChangePin }
            Route.VerifyChangeRecovery -> VerifyCurrentPin("Change recovery question") { route = Route.ChangeRecovery }
            Route.ChangePin -> PinSetupFlow(
                askQuestion = false,
                secretName = "PIN",
                onFinished = { pin, _, _ ->
                    if (SecurityRepository.changePin(context, pin)) null else "Could not save the new PIN. Try again."
                },
                onDone = { route = Route.Home },
            )
            Route.ChangeRecovery -> {
                var error by remember { mutableStateOf<String?>(null) }
                QuestionPanel(
                    title = "Recovery question",
                    subtitle = "Pick a question only you can answer. It is asked if you forget your PIN.",
                    errorText = error,
                    onSubmit = { q, a ->
                        if (SecurityRepository.changeQuestion(context, q, a)) route = Route.Home
                        else error = "Could not save. Try again."
                    },
                )
            }
        }
    }
}

/** Asks for the current PIN on the custom pad, then runs [onVerified]. */
@Composable
private fun VerifyCurrentPin(title: String, onVerified: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }
    PinEntryPanel(
        title = title,
        maxLength = SecurityRepository.pinLength,
        subtitle = "Enter your current PIN to continue.",
        errorText = error,
        errorKey = errorKey,
        onSubmit = { entered ->
            when (val r = SecurityRepository.check(context, PinKind.Pin, entered)) {
                PinCheck.Ok -> onVerified()
                is PinCheck.Wrong -> { error = "Wrong PIN."; errorKey++ }
                is PinCheck.LockedOut -> { error = "Too many tries. Wait ${(r.remainingMillis + 999) / 1000} s."; errorKey++ }
            }
        },
    )
}

@Composable
private fun SecurityHome(
    onBack: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onChangePin: () -> Unit,
    onChangeRecovery: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val enabled = SecurityRepository.isEnabled

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Security",
                navigationIcon = {
                    StudyBarIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                },
            )
        },
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .padding(bottom = 32.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SecRow(
                icon = Icons.Outlined.Lock, title = "Lock app",
                subtitle = if (enabled) "On. Asks for your PIN" else "Ask for a PIN when the app opens",
                first = true, last = !enabled,
                onClick = { if (enabled) onDisable() else onEnable() },
            ) {
                Switch(
                    checked = enabled,
                    onCheckedChange = { if (it) onEnable() else onDisable() },
                    thumbContent = {
                        Icon(if (enabled) Icons.Filled.Check else Icons.Filled.Close, null, Modifier.size(SwitchIconSize))
                    },
                )
            }
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.spatialDefault()),
                exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.spatialDefault()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TimeoutRow(selected = SecurityRepository.timeout) { SecurityRepository.setTimeout(context, it) }
                    SecRow(Icons.Outlined.Pin, "Change PIN", "Needs your current PIN", onClick = onChangePin)
                    SecRow(
                        Icons.Outlined.Key,
                        if (SecurityRepository.question == null) "Set recovery question" else "Change recovery question",
                        SecurityRepository.question?.text ?: "Needed if you forget your PIN. Needs your current PIN",
                        last = true, onClick = onChangeRecovery,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Surface(shape = ShapeLargeIncreased, color = colors.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = colors.onSecondaryContainer)
                    Text(
                        "The lock only decides who can open the app. Screenshots and screen recording still work. " +
                            "The PIN and your recovery answer are stored only as salted hashes and are not included in backups.",
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

private val SwitchIconSize = 16.dp

@Composable
private fun TimeoutRow(selected: LockTimeout, onSelect: (LockTimeout) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(4.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Timer, null, tint = colors.onSurfaceVariant)
                Column {
                    Text("Lock after", style = MaterialTheme.typography.bodyLarge)
                    Text("How long the app can stay in the background", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LockTimeout.entries.forEach { t ->
                    FilterChip(selected = t == selected, onClick = { onSelect(t) }, label = { Text(t.label) })
                }
            }
        }
    }
}

@Composable
private fun SecRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    first: Boolean = false,
    last: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(
        topStart = if (first) 28.dp else 4.dp, topEnd = if (first) 28.dp else 4.dp,
        bottomStart = if (last) 28.dp else 4.dp, bottomEnd = if (last) 28.dp else 4.dp,
    )
    Surface(onClick = onClick, shape = shape, color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
            supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium) },
            leadingContent = {
                Surface(shape = MaterialShapes.Cookie6Sided.toShape(), color = colors.secondaryContainer, modifier = Modifier.size(44.dp)) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, null, tint = colors.onSecondaryContainer, modifier = Modifier.size(24.dp))
                    }
                }
            },
            trailingContent = trailing,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
