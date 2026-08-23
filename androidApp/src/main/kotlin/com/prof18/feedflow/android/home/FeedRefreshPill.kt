package com.prof18.feedflow.android.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedRefreshPill(
    loadingState: FeedUpdateStatus,
    modifier: Modifier = Modifier,
) {
    ConditionalAnimatedVisibility(
        modifier = modifier,
        visible = loadingState.isLoading(),
        enter = fadeIn(animationSpec = tween(durationMillis = ANIMATION_DURATION)) +
            slideInVertically(
                animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
            ) { -it / 2 },
        exit = fadeOut(animationSpec = tween(durationMillis = ANIMATION_DURATION)) +
            slideOutVertically(
                animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
            ) { -it / 2 },
    ) {
        val hasProgress = loadingState.refreshedFeedCount > 0 && loadingState.totalFeedCount > 0
        val feedRefreshCounter = if (hasProgress) {
            "${loadingState.refreshedFeedCount}/${loadingState.totalFeedCount}"
        } else {
            "..."
        }

        Surface(
            shape = CircleShape,
            color = homeFloatingToolbarContainerColor(),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
            border = homeFloatingToolbarBorder(),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = Spacing.regular - Spacing.xsmall,
                    end = Spacing.regular,
                    top = Spacing.small,
                    bottom = Spacing.small,
                ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(INDICATOR_SIZE),
                    strokeWidth = INDICATOR_STROKE,
                )

                Text(
                    text = LocalFeedFlowStrings.current.loadingFeedMessage(feedRefreshCounter),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val INDICATOR_SIZE = 16.dp
private val INDICATOR_STROKE = 2.dp

private const val ANIMATION_DURATION = 350
