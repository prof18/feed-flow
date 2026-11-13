package com.prof18.feedflow.desktop.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.DesktopFeedbackData
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.DesktopFeedbackTone
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.linkStyle
import kotlin.math.abs

@Composable
@NonRestartableComposable
internal fun DesktopSnackbarHost(state: DesktopFeedbackState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier)
}

@Composable
internal fun SnackbarHost(state: DesktopFeedbackState, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = state.currentFeedback,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        transitionSpec = { feedbackTransition(reduceMotion) },
        contentAlignment = Alignment.BottomCenter,
        label = "FeedbackToast",
    ) { data ->
        if (data != null) FeedbackToast(data)
    }
}

/**
 * A slim toast: the tone icon and the message, an action only when it does something, and a small close
 * button only when the message has an action or stays up longer than usual. Swiping it sideways dismisses it.
 */
@Composable
private fun FeedbackToast(data: DesktopFeedbackData) {
    val showClose = data.tone != DesktopFeedbackTone.Progress &&
        (data.actionLabel != null || data.duration != DesktopFeedbackDuration.Short)
    FeedbackAutoDismiss(data, containsIcons = true, containsControls = data.actionLabel != null || showClose)
    val shape = RoundedCornerShape(8.dp)
    val isDark = JewelTheme.isDark
    var dragOffset by remember(data) { mutableFloatStateOf(0f) }
    var width by remember(data) { mutableFloatStateOf(0f) }
    Row(
        modifier = Modifier
            .testTag(FeedbackToastTag)
            .padding(16.dp)
            .widthIn(max = 460.dp)
            .onSizeChanged { width = it.width.toFloat() }
            .graphicsLayer { translationX = dragOffset }
            .pointerInput(data) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f },
                    onDragEnd = {
                        if (width > 0f && abs(dragOffset) > width * DismissFraction) data.dismiss()
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                    },
                )
            }
            .shadow(elevation = 6.dp, shape = shape)
            .background(
                if (isDark) JewelTheme.globalColors.toolwindowBackground else JewelTheme.globalColors.panelBackground,
                shape,
            )
            .then(
                // A hairline separates the toast from dark content; in light mode the shadow is enough.
                if (isDark) Modifier.border(1.dp, JewelTheme.globalColors.borders.normal, shape) else Modifier,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FeedbackToneIcon(data.tone)
        FeedbackMessageText(data, Modifier.weight(1f, fill = false), maxLines = Int.MAX_VALUE)
        data.actionLabel?.let { label ->
            Text(
                text = label,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClick = data::performAction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                color = JewelTheme.linkStyle.colors.content,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        if (showClose) {
            val closeLabel = LocalFeedFlowStrings.current.closeButtonContentDescription
            IconButton(
                onClick = data::dismiss,
                modifier = Modifier.size(20.dp).semantics { contentDescription = closeLabel },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = JewelTheme.globalColors.text.info,
                )
            }
        }
    }
}

/** Dismisses [data] after its duration, extended to the timeout accessibility settings recommend. */
@Composable
internal fun FeedbackAutoDismiss(data: DesktopFeedbackData, containsIcons: Boolean, containsControls: Boolean) {
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(data, accessibilityManager) {
        val baseDuration = when (data.duration) {
            DesktopFeedbackDuration.Short -> ShortFeedbackDuration
            DesktopFeedbackDuration.Long -> LongFeedbackDuration
            DesktopFeedbackDuration.Indefinite -> return@LaunchedEffect
        }
        val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = baseDuration,
            containsIcons = containsIcons,
            containsText = true,
            containsControls = containsControls,
        ) ?: baseDuration
        delay(timeout)
        data.dismiss()
    }
}

private const val ShortFeedbackDuration = 4_000L
private const val LongFeedbackDuration = 10_000L
private const val DismissFraction = 0.25f

internal const val FeedbackToastTag = "desktop-feedback-toast"
