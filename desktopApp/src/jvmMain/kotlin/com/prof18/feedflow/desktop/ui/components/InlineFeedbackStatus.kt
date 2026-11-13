package com.prof18.feedflow.desktop.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.DesktopFeedbackData
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.DesktopFeedbackTone
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

internal const val InlineFeedbackStatusTag = "inline-feedback-status"

/**
 * Shows the current feedback as a quiet icon and message, inline at the leading edge of an action row,
 * instead of a floating snackbar over the dialog. It dismisses itself like the snackbar does.
 */
@Composable
internal fun InlineFeedbackStatus(state: DesktopFeedbackState, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    Box(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        AnimatedContent(
            targetState = state.currentFeedback,
            transitionSpec = { feedbackTransition(reduceMotion) },
            contentAlignment = Alignment.CenterStart,
            label = "InlineFeedbackStatus",
        ) { data ->
            if (data != null) InlineFeedbackRow(data)
        }
    }
}

@Composable
private fun InlineFeedbackRow(data: DesktopFeedbackData) {
    FeedbackAutoDismiss(data, containsIcons = true, containsControls = false)
    Row(
        modifier = Modifier.testTag(InlineFeedbackStatusTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedbackToneIcon(data.tone)
        FeedbackMessageText(data)
    }
}

@Composable
internal fun FeedbackMessageText(data: DesktopFeedbackData, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Text(
        text = data.message,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun FeedbackToneIcon(tone: DesktopFeedbackTone, modifier: Modifier = Modifier) {
    val icon = tone.icon
    if (icon == null) {
        CircularProgressIndicator(modifier.size(16.dp))
    } else {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = modifier.size(16.dp),
            tint = tone.tint(),
        )
    }
}

/** A fade with a short rise in and a fade out, or no motion when the user asked to reduce it. */
internal fun feedbackTransition(reduceMotion: Boolean): ContentTransform = if (reduceMotion) {
    EnterTransition.None togetherWith ExitTransition.None
} else {
    val enter = fadeIn(tween(durationMillis = 180)) +
        slideInVertically(tween(durationMillis = 180)) { it / 4 }
    enter togetherWith fadeOut(tween(durationMillis = 150))
}

private val DesktopFeedbackTone.icon: ImageVector?
    get() = when (this) {
        DesktopFeedbackTone.Success -> Icons.Default.Check
        DesktopFeedbackTone.Info -> Icons.Outlined.Info
        DesktopFeedbackTone.Error -> Icons.Outlined.ErrorOutline
        DesktopFeedbackTone.Progress -> null
    }

@Composable
@ReadOnlyComposable
private fun DesktopFeedbackTone.tint(): Color = when (this) {
    DesktopFeedbackTone.Success -> FeedbackSuccessColor
    DesktopFeedbackTone.Info, DesktopFeedbackTone.Progress -> JewelTheme.globalColors.text.info
    DesktopFeedbackTone.Error -> JewelTheme.globalColors.text.error
}

internal val FeedbackSuccessColor = Color(color = 0xFF2F9E6B)
