package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PullToNextLayout(
    onNavigateNext: () -> Unit,
    indicator: @Composable BoxScope.(progress: Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    navigateNextThresholdDp: Dp = 164.dp,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val triggerThresholdPx = remember(navigateNextThresholdDp, density) {
        with(density) { navigateNextThresholdDp.toPx() }
    }

    val pullY = remember { Animatable(0f) }
    val popScale = remember { Animatable(1.2f) }

    val nestedScrollConnection = remember(triggerThresholdPx, onNavigateNext) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If we are currently pulled up, consume the scroll to pull back down
                if (source == NestedScrollSource.UserInput && pullY.value > 0f && available.y > 0f) {
                    val consumed = available.y.coerceAtMost(pullY.value)
                    coroutineScope.launch { pullY.snapTo(pullY.value - consumed) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // If we are at the bottom and pulling up (negative Y)
                if (source == NestedScrollSource.UserInput && available.y < 0f) {
                    val dragResistance = 0.4f
                    coroutineScope.launch {
                        pullY.snapTo(pullY.value - (available.y * dragResistance))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val isTriggered = pullY.value >= triggerThresholdPx

                if (isTriggered) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    popScale.animateTo(
                        targetValue = 1.3f,
                        animationSpec = tween(durationMillis = 100),
                    )

                    onNavigateNext()
                    popScale.snapTo(1f)
                }

                pullY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                )

                return Velocity.Zero
            }
        }
    }

    if (enabled) {
        val fallbackScrollState = rememberScrollableState { 0f }

        Box(
            modifier = modifier
                .nestedScroll(nestedScrollConnection)
                .scrollable(
                    state = fallbackScrollState,
                    orientation = Orientation.Vertical,
                )
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = -pullY.value },
            ) {
                content()
            }

            val progress = (pullY.value / triggerThresholdPx).coerceIn(0f, 1f)
            val dynamicScale = (0.5f + (progress * 0.5f)) * popScale.value

            if (pullY.value > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(bottom = 16.dp)
                        .graphicsLayer {
                            translationY = -pullY.value + 80.dp.toPx()
                            alpha = progress // Fade in
                            scaleX = dynamicScale
                            scaleY = dynamicScale
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    indicator(progress)
                }
            }
        }
    } else {
        content()
    }
}

@Composable
fun PullToNextIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val fillStartThreshold = 0.50f

    val fillProgress = if (progress > fillStartThreshold) {
        ((progress - fillStartThreshold) / (1f - fillStartThreshold)).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(Color.Transparent),
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                strokeWidth = 2.dp,
                trackColor = Color.Transparent,
            )
            CircularProgressIndicator(
                progress = { fillProgress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                trackColor = Color.Transparent,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        if (!title.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = (1f - fillProgress) * 20f
                        alpha = fillProgress
                    }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                        shape = CircleShape,
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
