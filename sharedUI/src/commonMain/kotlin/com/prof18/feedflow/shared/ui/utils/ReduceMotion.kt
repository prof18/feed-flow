package com.prof18.feedflow.shared.ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun ConditionalAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val reduceMotionEnabled = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotionEnabled) EnterTransition.None else enter,
        exit = if (reduceMotionEnabled) ExitTransition.None else exit,
        label = label,
        content = content,
    )
}

@Composable
fun <T> conditionalAnimateAsState(
    targetValue: T,
    animationSpec: AnimationSpec<T>,
    label: String = "",
    finishedListener: ((T) -> Unit)? = null,
    animateFunction: @Composable (
        targetValue: T,
        animationSpec: AnimationSpec<T>,
        label: String,
        finishedListener: ((T) -> Unit)?,
    ) -> State<T>,
): State<T> {
    val reduceMotionEnabled = LocalReduceMotion.current
    val actualAnimationSpec = if (reduceMotionEnabled) {
        snap()
    } else {
        animationSpec
    }
    return animateFunction(targetValue, actualAnimationSpec, label, finishedListener)
}

@Composable
fun conditionalAnimateFloatAsState(
    targetValue: Float,
    animationSpec: FiniteAnimationSpec<Float> = spring(),
    label: String = "",
    finishedListener: ((Float) -> Unit)? = null,
): State<Float> {
    val reduceMotionEnabled = LocalReduceMotion.current
    val actualAnimationSpec = if (reduceMotionEnabled) {
        snap()
    } else {
        animationSpec
    }
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = actualAnimationSpec,
        label = label,
        finishedListener = finishedListener,
    )
}

suspend fun LazyListState.scrollToItemConditionally(
    index: Int,
    scrollOffset: Int = 0,
    reduceMotionEnabled: Boolean,
) {
    if (reduceMotionEnabled) {
        scrollToItem(index, scrollOffset)
    } else {
        animateScrollToItem(index, scrollOffset)
    }
}
