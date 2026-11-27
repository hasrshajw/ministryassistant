@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.harsha.myapplication.ui.security

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.data.PinCheck
import com.harsha.myapplication.data.PinKind
import com.harsha.myapplication.data.SecurityRepository
import com.harsha.myapplication.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────
//  Shared pieces
// ─────────────────────────────────────────────────────────────────────────

/** Cookie-shaped badge that turns slowly while idle; swaps to an open lock when [open]. */
@Composable
private fun LockBadge(open: Boolean, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val colors = MaterialTheme.colorScheme
    val spin = rememberInfiniteTransition(label = "badgeSpin")
    val angle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart), label = "angle",
    )
    val scale by animateFloatAsState(if (open) 1.15f else 1f, Motion.spatialFast, label = "badgeScale")
    val container by animateColorAsState(
        if (open) colors.tertiaryContainer else colors.primaryContainer, Motion.effectsDefault(), label = "badgeColor",
    )
    Box(modifier.size(88.dp).graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = container,
            modifier = Modifier.size(88.dp).rotate(angle),
        ) {}
        AnimatedContent(
            targetState = icon ?: if (open) Icons.Filled.LockOpen else Icons.Filled.Lock,
            transitionSpec = { (scaleIn(Motion.spatialFast) + fadeIn(Motion.effectsFast())) togetherWith fadeOut(Motion.effectsFast()) },
            label = "badgeIcon",
        ) { vec ->
            Icon(
                vec, contentDescription = null, modifier = Modifier.size(36.dp),
                tint = if (open) colors.onTertiaryContainer else colors.onPrimaryContainer,
            )
        }
    }
}

/**
 * One PIN entry screen: badge, title, dots, message line and the custom pad. Holds the typed digits.
 * [onSubmit] gets the PIN; [errorKey] changing (with [errorText] set) shakes the dots and clears them.
 */
@Composable
internal fun PinEntryPanel(
    title: String,
    subtitle: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    errorKey: Int = 0,
    padEnabled: Boolean = true,
    autoSubmitAtMax: Boolean = true,
    badgeOpen: Boolean = false,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    footer: @Composable () -> Unit = {},
) {
    var pin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var submitting by remember { mutableStateOf(false) }

    fun submit() {
        val entered = pin
        if (submitting || entered.length < SecurityRepository.MIN_LENGTH) return
        submitting = true
        scope.launch {
            delay(110) // let the last dot finish its spring before the screen reacts
            onSubmit(entered)
            pin = ""
            submitting = false
        }
    }

    LaunchedEffect(errorKey) { if (errorKey != 0) pin = "" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LockBadge(open = badgeOpen, icon = badgeIcon)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
        PinDots(length = pin.length, shakeKey = errorKey, isError = errorText != null)
        Box(Modifier.height(40.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = errorText,
                transitionSpec = { fadeIn(Motion.effectsDefault()) togetherWith fadeOut(Motion.effectsFast()) },
                label = "pinMessage",
            ) { msg ->
                if (msg != null) {
                    Text(
                        msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        PinPad(
            onDigit = { d ->
                if (pin.length < SecurityRepository.MAX_LENGTH) {
                    pin += d
                    if (autoSubmitAtMax && pin.length == SecurityRepository.MAX_LENGTH) submit()
                }
            },
            onBackspace = { pin = pin.dropLast(1) },
            onSubmit = ::submit,
            submitEnabled = pin.length >= SecurityRepository.MIN_LENGTH,
            enabled = padEnabled,
        )
        Spacer(Modifier.height(12.dp))
        footer()
    }
}

private fun formatWait(ms: Long): String {
    val s = (ms + 999) / 1000
    return if (s >= 60) "${s / 60} min ${s % 60} s" else "$s s"
}

// ─────────────────────────────────────────────────────────────────────────
//  Lock screen
// ─────────────────────────────────────────────────────────────────────────

private enum class LockMode { Pin, Recovery, Reset }

/**
 * Full-screen overlay shown while [SecurityRepository.isLocked]. PIN entry, a "Forgot PIN?" route through the
 * recovery PIN, and (after a correct recovery PIN) a forced reset of both PINs.
 */
@Composable
fun LockScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(LockMode.Pin) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }
    var unlocking by remember { mutableStateOf(false) }
    var waitMs by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    val kind = if (mode == LockMode.Recovery) PinKind.Recovery else PinKind.Pin

    // Live countdown while a lockout is active.
    LaunchedEffect(mode, errorKey) {
        waitMs = SecurityRepository.lockoutRemaining(context, kind)
        while (waitMs > 0) {
            delay(500)
            waitMs = SecurityRepository.lockoutRemaining(context, kind)
        }
    }
    val lockedOut by remember { derivedStateOf { waitMs > 0 } }

    fun fail(msg: String) { errorText = msg; errorKey++ }

    // Fingerprint first: the system prompt opens as soon as the lock screen appears. Cancelling it
    // leaves this PIN screen as the fallback. A match unlocks exactly like a correct PIN.
    val activity = remember(context) { BiometricUnlock.findActivity(context) }
    val canUseBiometric = remember(context) { BiometricUnlock.isAvailable(context) }
    fun promptBiometric() {
        if (activity == null || mode != LockMode.Pin || unlocking) return
        BiometricUnlock.prompt(activity) {
            unlocking = true
            scope.launch { delay(260); SecurityRepository.unlock() }
        }
    }
    LaunchedEffect(Unit) { if (canUseBiometric) { delay(350); promptBiometric() } }

    fun handle(entered: String) {
        when (val r = SecurityRepository.check(context, kind, entered)) {
            PinCheck.Ok -> {
                errorText = null
                if (mode == LockMode.Recovery) {
                    mode = LockMode.Reset
                } else {
                    unlocking = true
                    scope.launch { delay(260); SecurityRepository.unlock() }
                }
            }
            is PinCheck.LockedOut -> { waitMs = r.remainingMillis; fail("Too many tries. Wait ${formatWait(r.remainingMillis)}.") }
            is PinCheck.Wrong -> fail(
                if (r.attemptsLeftBeforeLockout == 1) "Wrong PIN. 1 try left before a wait."
                else "Wrong PIN. ${r.attemptsLeftBeforeLockout} tries left before a wait."
            )
        }
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(Motion.spatialDefault()) { if (forward) it / 4 else -it / 4 } + fadeIn(Motion.effectsDefault())) togetherWith
                    (slideOutHorizontally(Motion.spatialDefault()) { if (forward) -it / 4 else it / 4 } + fadeOut(Motion.effectsFast()))
            },
            label = "lockMode",
        ) { m ->
            when (m) {
                LockMode.Pin, LockMode.Recovery -> {
                    val recovery = m == LockMode.Recovery
                    PinEntryPanel(
                        title = if (recovery) "Enter recovery PIN" else "Enter PIN",
                        subtitle = when {
                            lockedOut -> "Try again in ${formatWait(waitMs)}"
                            recovery -> "Use the recovery PIN you created when you turned on the lock."
                            else -> "Your ministry notes and contacts are locked."
                        },
                        onSubmit = ::handle,
                        errorText = errorText,
                        errorKey = errorKey,
                        padEnabled = !lockedOut && !unlocking,
                        autoSubmitAtMax = true,
                        badgeOpen = unlocking,
                        footer = {
                            if (!recovery && canUseBiometric) {
                                TextButton(onClick = { promptBiometric() }) {
                                    Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Use fingerprint")
                                }
                            }
                            TextButton(onClick = { errorText = null; mode = if (recovery) LockMode.Pin else LockMode.Recovery }) {
                                Text(if (recovery) "Back to PIN" else "Forgot PIN?")
                            }
                        },
                    )
                }
                LockMode.Reset -> PinSetupFlow(
                    askRecovery = true,
                    secretName = "PIN",
                    showDone = false,
                    intro = "Recovery PIN accepted. Choose a new PIN and a new recovery PIN.",
                    onFinished = { pin, rec ->
                        if (SecurityRepository.enable(context, pin, rec!!)) null else "Could not save the new PINs. Try again."
                    },
                    onDone = {},
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Setup / change flow
// ─────────────────────────────────────────────────────────────────────────

private enum class SetupStep { Create, Confirm, CreateRecovery, ConfirmRecovery, Done }

/**
 * Create -> confirm -> (create recovery -> confirm recovery) -> done.
 *
 * @param askRecovery true when turning the lock on (or resetting both PINs).
 * @param secretName "PIN" or "recovery PIN" for the single-secret changes.
 * @param onFinished saves the result; return null on success or an error message to restart the flow.
 * @param showDone shows the closing "You're protected" page before calling [onDone].
 */
@Composable
fun PinSetupFlow(
    askRecovery: Boolean,
    onFinished: (pin: String, recovery: String?) -> String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    secretName: String = "PIN",
    showDone: Boolean = true,
    intro: String? = null,
) {
    var step by remember { mutableStateOf(SetupStep.Create) }
    var first by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var recoveryFirst by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }

    fun fail(msg: String, restart: SetupStep? = null) {
        error = msg; errorKey++
        if (restart != null) { step = restart; first = ""; recoveryFirst = "" }
    }

    fun finish(rec: String?) {
        val result = onFinished(pin, rec)
        if (result != null) fail(result, SetupStep.Create)
        else if (showDone) step = SetupStep.Done else onDone()
    }

    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val t: ContentTransform =
                    (slideInHorizontally(Motion.spatialDefault()) { if (forward) it / 3 else -it / 3 } + fadeIn(Motion.effectsDefault())) togetherWith
                        (slideOutHorizontally(Motion.spatialDefault()) { if (forward) -it / 3 else it / 3 } + fadeOut(Motion.effectsFast()))
                t
            },
            label = "setupStep",
        ) { s ->
            when (s) {
                SetupStep.Create -> PinEntryPanel(
                    title = "Create $secretName",
                    subtitle = intro ?: "Choose ${SecurityRepository.MIN_LENGTH} to ${SecurityRepository.MAX_LENGTH} digits.",
                    errorText = error, errorKey = errorKey, autoSubmitAtMax = false,
                    onSubmit = { v ->
                        if (SecurityRepository.isTooSimple(v)) fail("Too easy to guess. Avoid 1234 or 0000.")
                        else { error = null; first = v; step = SetupStep.Confirm }
                    },
                )
                SetupStep.Confirm -> PinEntryPanel(
                    title = "Confirm $secretName",
                    subtitle = "Enter the same digits again.",
                    errorText = error, errorKey = errorKey, autoSubmitAtMax = false,
                    onSubmit = { v ->
                        if (v != first) fail("Those did not match. Try again.", SetupStep.Create)
                        else {
                            error = null; pin = v
                            if (askRecovery) step = SetupStep.CreateRecovery else finish(null)
                        }
                    },
                )
                SetupStep.CreateRecovery -> PinEntryPanel(
                    title = "Create recovery PIN",
                    subtitle = "A second PIN, different from the first. It is the only way back in if you forget your PIN.",
                    errorText = error, errorKey = errorKey, autoSubmitAtMax = false,
                    onSubmit = { v ->
                        when {
                            v == pin -> fail("The recovery PIN must be different from your PIN.")
                            SecurityRepository.isTooSimple(v) -> fail("Too easy to guess. Avoid 1234 or 0000.")
                            else -> { error = null; recoveryFirst = v; step = SetupStep.ConfirmRecovery }
                        }
                    },
                )
                SetupStep.ConfirmRecovery -> PinEntryPanel(
                    title = "Confirm recovery PIN",
                    subtitle = "Enter the recovery PIN once more.",
                    errorText = error, errorKey = errorKey, autoSubmitAtMax = false,
                    onSubmit = { v ->
                        if (v != recoveryFirst) fail("Those did not match. Try again.", SetupStep.CreateRecovery)
                        else { error = null; finish(v) }
                    },
                )
                SetupStep.Done -> DonePage(askRecovery = askRecovery, onDone = onDone)
            }
        }
    }
}

@Composable
private fun DonePage(askRecovery: Boolean, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LockBadge(open = true, icon = Icons.Filled.Check)
        Spacer(Modifier.height(24.dp))
        Text(
            if (askRecovery) "App lock is on" else "PIN updated",
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (askRecovery) {
                "Nobody can read your PIN or recovery PIN, not even us. If you forget both, the only way back in is to clear the app's data, which erases everything not backed up."
            } else {
                "Use the new one next time the app asks."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onDone) { Text("Got it") }
    }
}
