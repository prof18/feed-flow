package com.prof18.feedflow.desktop.home.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun NewArticlesPill(
    pendingNewArticlesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConditionalAnimatedVisibility(
        visible = pendingNewArticlesCount > 0,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        label = "NewArticlesPill",
    ) {
        val colors = MaterialTheme.colorScheme
        val border = if (colors.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD) {
            BorderStroke(Dp.Hairline, colors.outlineVariant)
        } else {
            null
        }
        val displayCount = if (pendingNewArticlesCount >= MAX_DISPLAY_COUNT) {
            "$MAX_DISPLAY_COUNT+"
        } else {
            pendingNewArticlesCount.toString()
        }

        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            color = colors.surfaceContainerHighest,
            shadowElevation = 6.dp,
            border = border,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.regular, vertical = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                )
                Text(
                    text = LocalFeedFlowStrings.current.newArticlesButton(displayCount),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private const val MAX_DISPLAY_COUNT = 40
private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f
