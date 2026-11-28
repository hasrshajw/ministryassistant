package com.harsha.myapplication.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harsha.myapplication.ui.theme.Motion

/**
 * The single top app bar used by Home, People, Notes and Settings (Material 3 Expressive).
 *
 *  - Compact (default): same height and left-aligned title on every screen, so existing callers
 *    look the same, now with the Expressive emphasized title and an animated title / subtitle.
 *  - [collapsing] = true: an Expressive *flexible* bar that is tall at the top of the page and
 *    collapses on scroll. Pass `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` for it.
 *
 * Use [StudyBarIconButton] for navigation / action icons to get the Expressive tonal button
 * that morphs its shape on press.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapsing: Boolean = false,
    // true: the title stays exactly where it is without a subtitle; the subtitle hangs just below it
    // instead of pushing the title up (compact bar only).
    keepTitleInPlace: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    // Screens are already padded for the status bar by their host, so no extra inset by default.
    windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
) {
    val colors = MaterialTheme.colorScheme
    // Every screen's bar ends with the backup status button (animates on each automatic sync).
    val barActions: @Composable RowScope.() -> Unit = {
        actions()
        BackupStatusAction()
    }
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = containerColor,
        scrolledContainerColor = if (containerColor == Color.Transparent) Color.Transparent else colors.surfaceContainer,
        titleContentColor = colors.onSurface,
        subtitleContentColor = colors.onSurfaceVariant,
        navigationIconContentColor = colors.onSurfaceVariant,
        actionIconContentColor = colors.onSurfaceVariant,
    )

    if (collapsing) {
        MediumFlexibleTopAppBar(
            title = { BarTitle(title) },
            subtitle = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = barActions,
            windowInsets = windowInsets,
            colors = barColors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        TopAppBar(
            title = {
                if (keepTitleInPlace) {
                    Box {
                        BarTitle(title)
                        AnimatedVisibility(
                            visible = subtitle != null,
                            enter = fadeIn(spring(1f, 1600f)),
                            exit = fadeOut(spring(1f, 1600f)),
                            // Not counted in the title's height, so the title keeps its position.
                            modifier = Modifier
                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                .offset(y = 26.dp),
                        ) {
                            BarSubtitle(subtitle.orEmpty())
                        }
                    }
                } else {
                    Column {
                        BarTitle(title)
                        AnimatedVisibility(
                            visible = subtitle != null,
                            enter = expandVertically(Motion.spatialDefault()) + fadeIn(spring(1f, 1600f)),
                            exit = shrinkVertically(Motion.spatialDefault()) + fadeOut(spring(1f, 1600f)),
                        ) {
                            BarSubtitle(subtitle.orEmpty())
                        }
                    }
                }
            },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = barActions,
            windowInsets = windowInsets,
            colors = barColors,
            scrollBehavior = scrollBehavior,
        )
    }
}

/** Small subtitle line under the title; cross-fades when the text changes. */
@Composable
private fun BarSubtitle(subtitle: String) {
    AnimatedContent(
        targetState = subtitle,
        transitionSpec = { fadeIn(spring(1f, 1600f)) togetherWith fadeOut(spring(1f, 1600f)) },
        label = "barSubtitle",
    ) { text ->
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Title text: Expressive emphasized style, cross-fades when the text changes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BarTitle(title: String) {
    AnimatedContent(
        targetState = title,
        transitionSpec = { fadeIn(spring(1f, 1600f)) togetherWith fadeOut(spring(1f, 1600f)) },
        label = "barTitle",
    ) { text ->
        Text(
            text,
            style = MaterialTheme.typography.titleLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Expressive tonal icon button for top bars: round at rest, its shape morphs when pressed.
 * Use inside `navigationIcon = { ... }` or `actions = { ... }`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyBarIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 4.dp),
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
