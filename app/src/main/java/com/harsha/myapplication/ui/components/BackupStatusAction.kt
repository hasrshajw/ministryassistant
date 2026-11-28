@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.harsha.myapplication.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.harsha.myapplication.data.BackupRepository
import com.harsha.myapplication.data.BackupStatus
import kotlinx.coroutines.launch

private val SheetRadius = 28.dp

// ─────────────────────────────────────────────────────────────────────────────
// Top-bar backup indicator, shown on every screen (StudyTopBar adds it automatically).
//
// It reads the real state from BackupRepository, so it animates for automatic syncs after
// every change as well as manual backups, and shows the real reason when one fails.
//
//  - Synced:  quiet cloud-with-tick. Tap = back up now.
//  - Syncing: tonal container + Expressive LoadingIndicator.
//  - Success: primary fill, tick, springy pop + ripple ring + Confirm haptic.
//  - Failed:  error container, cloud-off, damped shake + Reject haptic. Tap = reason sheet.
//
// Hidden while "Back up automatically" is off and nothing is running or failed.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BackupStatusAction() {
    val status = BackupRepository.syncStatus
    val visible = BackupRepository.prefs.autoBackup || status != BackupStatus.Synced
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f),
    ) {
        BackupStatusButton(
            status = status,
            onClick = {
                if (status == BackupStatus.Failed) showSheet = true else BackupRepository.retry()
            },
        )
    }

    // Close the sheet automatically if the status leaves Failed (e.g. a retry started).
    if (showSheet && status == BackupStatus.Failed) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        fun dismiss(then: () -> Unit = {}) {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                showSheet = false
                then()
            }
        }
        BackupFailureSheet(
            sheetState = sheetState,
            reason = BackupRepository.failureReason ?: "Something went wrong.",
            onDismissRequest = { showSheet = false },
            onRetry = { dismiss { BackupRepository.retry() } },
            onClose = { dismiss { BackupRepository.dismissFailure() } },
        )
    }
}

// Motion follows the M3 Expressive rules:
//  • SPATIAL properties (scale, position, size)  -> spatial springs, may overshoot
//  • EFFECTS properties (color, alpha)           -> effects springs, never overshoot
//  • State swap of the icon                      -> "fade through" transition pattern
@Composable
fun BackupStatusButton(
    status: BackupStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playEffects: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val haptic = LocalHapticFeedback.current

    val pop = remember { Animatable(1f) }      // scale, spatial
    val shake = remember { Animatable(0f) }    // dp offset, spatial
    val ring = remember { Animatable(0f) }     // 0..1 ripple progress, effects

    // Effects only play when the status CHANGES while this screen is open, so opening another
    // screen while a backup is failed does not shake / buzz again.
    var previous by remember { mutableStateOf(status) }
    LaunchedEffect(status) {
        val changed = previous != status
        previous = status
        if (!playEffects || !changed) return@LaunchedEffect
        when (status) {
            BackupStatus.Success -> {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                launch {
                    pop.snapTo(0.72f)
                    pop.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMedium))
                }
                launch {
                    ring.snapTo(0f)
                    ring.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessVeryLow))
                    ring.snapTo(0f)
                }
            }
            BackupStatus.Failed -> {
                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                shake.snapTo(12f)
                shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 900f))
            }
            else -> Unit
        }
    }

    val containerColor by animateColorAsState(
        targetValue = when (status) {
            BackupStatus.Synced -> Color.Transparent
            BackupStatus.Syncing -> colors.secondaryContainer
            BackupStatus.Success -> colors.primary
            BackupStatus.Failed -> colors.errorContainer
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "backupContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            BackupStatus.Synced -> colors.onSurfaceVariant
            BackupStatus.Syncing -> colors.onSecondaryContainer
            BackupStatus.Success -> colors.onPrimary
            BackupStatus.Failed -> colors.onErrorContainer
        },
        animationSpec = motion.defaultEffectsSpec(),
        label = "backupContent",
    )

    val ringColor = colors.primary
    val stateText = when (status) {
        BackupStatus.Synced -> "Backed up"
        BackupStatus.Syncing -> "Backing up"
        BackupStatus.Success -> "Backup complete"
        BackupStatus.Failed -> "Backup failed, tap for details"
    }

    // Capture specs here: transitionSpec below is not a @Composable scope.
    val iconFade = motion.fastEffectsSpec<Float>()
    val iconScale = motion.fastSpatialSpec<Float>()

    FilledTonalIconButton(
        onClick = onClick,
        enabled = status == BackupStatus.Synced || status == BackupStatus.Failed,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .drawBehind {
                val p = ring.value
                if (p > 0f) {
                    drawCircle(
                        color = ringColor.copy(alpha = (1f - p) * 0.5f),
                        radius = size.minDimension / 2f * lerp(1f, 1.9f, p),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                translationX = shake.value.dp.toPx()
            }
            .semantics {
                stateDescription = stateText
                liveRegion = LiveRegionMode.Polite
            },
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
    ) {
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (fadeIn(iconFade) + scaleIn(iconScale, initialScale = 0.6f)) togetherWith
                    (fadeOut(tween(90)) + scaleOut(iconScale, targetScale = 0.6f)) using
                    SizeTransform(clip = false)
            },
            label = "backupStatus",
        ) { s ->
            when (s) {
                BackupStatus.Synced -> Icon(Icons.Default.CloudDone, contentDescription = "Backed up")
                BackupStatus.Syncing -> LoadingIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .semantics { contentDescription = "Backing up" },
                    color = colors.primary,
                )
                BackupStatus.Success -> Icon(Icons.Default.Check, contentDescription = "Backup complete")
                BackupStatus.Failed -> Icon(Icons.Default.CloudOff, contentDescription = "Backup failed, tap for details")
            }
        }
    }
}

// Failure bottom sheet: one icon, one headline, the real reason, one primary action.
@Composable
private fun BackupFailureSheet(
    sheetState: SheetState,
    reason: String,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(colors.errorContainer, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = colors.onErrorContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Backup failed",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
            )
            Text(
                reason,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.MediumContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Try again", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Not now")
            }
        }
    }
}
