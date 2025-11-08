package com.prof18.feedflow.shared.ui.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun ConditionalAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    reduceMotion: Boolean = LocalReduceMotion.current,
    content: @Composable () -> Unit,
) {
    if (reduceMotion) {
        if (visible) {
            content()
        }
    } else {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = enter,
            exit = exit,
            content = { content() },
        )
    }
}
