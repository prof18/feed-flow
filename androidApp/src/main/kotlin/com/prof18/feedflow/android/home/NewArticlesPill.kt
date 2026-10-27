package com.prof18.feedflow.android.home

import androidx.compose.animation.EnterExitState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.FeedUpdateStatus

@Composable
internal fun NewArticlesPill(
    count: Int,
    loadingState: FeedUpdateStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConditionalAnimatedVisibility(
        modifier = modifier,
        visible = count > 0 && !loadingState.isLoading(),
        enter = fadeIn(animationSpec = tween(durationMillis = ANIMATION_DURATION)) +
            slideInVertically(
                animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
            ) { -it / 2 },
        exit = fadeOut(animationSpec = tween(durationMillis = ANIMATION_DURATION)) +
            slideOutVertically(
                animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
            ) { -it / 2 },
    ) {
        // Same as FeedRefreshPill: only cast the shadow once the enter transition has settled.
        val isTransitionSettled = transition.currentState == transition.targetState &&
            transition.currentState == EnterExitState.Visible

        val label = if (count >= MaxNewArticlesCount) {
            "$MaxNewArticlesCount+"
        } else {
            count.toString()
        }

        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = homeFloatingToolbarContainerColor(),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = if (isTransitionSettled) PILL_ELEVATION else 0.dp,
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
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(INDICATOR_SIZE),
                )

                Text(
                    text = LocalFeedFlowStrings.current.newArticlesButton(label),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val PILL_ELEVATION = 6.dp
private val INDICATOR_SIZE = 16.dp

private const val ANIMATION_DURATION = 350
private const val MaxNewArticlesCount = 40
