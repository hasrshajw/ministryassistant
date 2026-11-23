package com.harsha.myapplication.ui.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.harsha.myapplication.ui.theme.Motion

/**
 * ============================================================================
 *  Shared Element Transitions — app-wide utilities
 * ============================================================================
 *
 * 三个核心能力：
 *
 *  1. 通过 CompositionLocal 分发 SharedTransitionScope 和
 *     AnimatedVisibilityScope，避免逐层传递参数。
 *  2. 所有 morph 统一使用 Motion.spatialFast<Rect>() 作为 bounds 动画，
 *     与 App 其余部分的 motion 人格一致。
 *  3. 同时提供 sharedElement（同内容 hero）和 sharedBounds（容器变换）
 *     两种 modifier。
 *
 * 关于 OverlayClip：
 *  `ParentClip` 在部分 Compose 版本中并非 public API，直接 import 会导致
 *  "Unresolved reference"。sharedBounds 的 `clipInOverlayDuringTransition`
 *  参数默认值就是 ParentClip，因此本文件不显式指定该参数 —— 行为完全一致，
 *  但不再依赖可能缺失的符号。
 *
 * ---------------------------------------------------------------------------
 *  用法
 * ---------------------------------------------------------------------------
 *
 *  1. 在导航宿主外层包裹 AppSharedTransitionHost：
 *
 *        AppSharedTransitionHost {
 *            AppNavHost(...)
 *        }
 *
 *  2. 在 AnimatedContent / NavHost 的 destination 中发布
 *     AnimatedVisibilityScope：
 *
 *        AnimatedContent(targetState = screen) { target ->
 *            ProvideAnimatedVisibilityScope(this) {
 *                when (target) { ... }
 *            }
 *        }
 *
 *  3. 在列表端和详情端使用相同的 key：
 *
 *        Modifier.appSharedBounds(key = "card-${item.id}")
 *        Text(item.title, Modifier.appSharedElement(key = "title-${item.id}"))
 */

// ============================================================================
//  Scope plumbing
// ============================================================================

/** 由 AppSharedTransitionHost 安装的 SharedTransitionScope。 */
val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * 当前 AnimatedContent / AnimatedVisibility / NavHost destination 的
 * AnimatedVisibilityScope。
 */
val LocalAnimatedVisibilityScope =
    staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * 安装 SharedTransitionLayout 并发布 SharedTransitionScope。
 *
 * 包裹在导航宿主的最外层（或 Dialog 的 Scaffold 外层）。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppSharedTransitionHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
            content = content,
        )
    }
}

/**
 * 发布 AnimatedVisibilityScope 到 CompositionLocal。
 */
@Composable
fun ProvideAnimatedVisibilityScope(
    scope: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAnimatedVisibilityScope provides scope,
        content = content,
    )
}

/** AnimatedContentScope 的重载，方便直接传入 this@AnimatedContent。 */
@Composable
fun ProvideAnimatedVisibilityScope(
    scope: AnimatedContentScope,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAnimatedVisibilityScope provides scope,
        content = content,
    )
}

// ============================================================================
//  默认值
// ============================================================================

/**
 * 所有 sharedBounds / sharedElement 的默认 bounds 动画。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val DefaultSharedBoundsTransform: BoundsTransform = BoundsTransform { _, _ ->
    Motion.spatialFast<Rect>()
}

/**
 * container transform 的默认 ResizeMode。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val DefaultContainerResizeMode: ResizeMode = ResizeMode.RemeasureToBounds

// ============================================================================
//  Modifier 扩展
// ============================================================================

/**
 * 标记 hero 内容（标题、图片、图标等两端完全相同的视觉元素）。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.appSharedElement(
    key: Any,
    boundsTransform: BoundsTransform? = null,
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
        ?: error(
            "appSharedElement() requires AppSharedTransitionHost { ... } " +
                    "somewhere above this composable."
        )
    val visibilityScope = LocalAnimatedVisibilityScope.current
        ?: error(
            "appSharedElement() requires ProvideAnimatedVisibilityScope(this) " +
                    "inside the current AnimatedContent / NavHost destination."
        )

    return with(sharedScope) {
        this@appSharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = boundsTransform ?: DefaultSharedBoundsTransform,
        )
    }
}

/**
 * 标记共享容器（card → detail、列表项 → 全屏页）。
 *
 * 移除了 clipInOverlayDuringTransition 参数，使用 sharedBounds 的默认值
 * （即 ParentClip 的行为），避免依赖非 public 符号。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.appSharedBounds(
    key: Any,
    resizeMode: ResizeMode = DefaultContainerResizeMode,
    boundsTransform: BoundsTransform? = null,
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
        ?: error(
            "appSharedBounds() requires AppSharedTransitionHost { ... } " +
                    "somewhere above this composable."
        )
    val visibilityScope = LocalAnimatedVisibilityScope.current
        ?: error(
            "appSharedBounds() requires ProvideAnimatedVisibilityScope(this) " +
                    "inside the current AnimatedContent / NavHost destination."
        )

    return with(sharedScope) {
        this@appSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(),
            exit = fadeOut(),
            boundsTransform = boundsTransform ?: DefaultSharedBoundsTransform,
            resizeMode = resizeMode,
            // clipInOverlayDuringTransition 使用默认值（ParentClip），
            // 不需要显式传入，因此不再引用 ParentClip 符号。
        )
    }
}

// ============================================================================
//  Convenience: paired keys
// ============================================================================

/**
 * 为同一个逻辑 item 生成 bounds 和 hero 两个 key。
 */
data class SharedItemKeys(
    val bounds: Any,
    val hero: Any?,
) {
    companion object {
        fun forItem(id: Any, useHero: Boolean = false): SharedItemKeys =
            SharedItemKeys(
                bounds = "bounds:$id",
                hero = if (useHero) "hero:$id" else null,
            )
    }
}

// ============================================================================
//  Container transform (card → full screen) — safe variant
// ============================================================================

/**
 * Softer spring for large surfaces. `spatialFast` (stiffness 800) is right for pills and
 * small chips, but a full-screen container morphing at that speed looks abrupt.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val ContainerBoundsTransform: BoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.86f, stiffness = 320f)
}

/** Clips the shared surface to a rounded rectangle while it is drawn in the overlay. */
@OptIn(ExperimentalSharedTransitionApi::class)
private fun roundedOverlayClip(radius: Dp): OverlayClip = object : OverlayClip {
    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path? = Path().apply {
        addRoundRect(RoundRect(bounds, CornerRadius(with(density) { radius.toPx() })))
    }
}

/**
 * Like [appSharedBounds], but:
 *  • returns the modifier unchanged when no SharedTransitionLayout / AnimatedVisibilityScope is
 *    available (previews, tests, dialogs) instead of crashing;
 *  • uses the softer [ContainerBoundsTransform];
 *  • rounds the overlay clip so the card keeps its corners while it grows.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.appSharedBoundsOrNone(
    key: Any,
    cornerRadius: Dp = 28.dp,
    boundsTransform: BoundsTransform = ContainerBoundsTransform,
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@appSharedBoundsOrNone.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(),
            exit = fadeOut(),
            boundsTransform = boundsTransform,
            resizeMode = DefaultContainerResizeMode,
            clipInOverlayDuringTransition = roundedOverlayClip(cornerRadius),
        )
    }
}