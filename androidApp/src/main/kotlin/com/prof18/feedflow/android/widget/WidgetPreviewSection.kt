package com.prof18.feedflow.android.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun WidgetPreviewSection(
    settingsState: WidgetSettingsState,
    modifier: Modifier = Modifier,
) {
    val baseBackgroundColor = settingsState.backgroundColor?.let { Color(it) }
        ?: MaterialTheme.colorScheme.surface

    @Suppress("MagicNumber")
    val backgroundAlpha = settingsState.backgroundOpacityPercent.coerceIn(minimumValue = 0, maximumValue = 100) / 100f
    val previewBackgroundColor = baseBackgroundColor.copy(alpha = backgroundAlpha)
    val fontSizes = widgetFontSizes(settingsState.fontScale)

    Column(
        modifier = modifier,
    ) {
        WidgetPreviewWallpaper(
            feedLayout = settingsState.feedLayout,
            showWidgetHeader = settingsState.showHeader,
            fontSizes = fontSizes,
            backgroundColor = previewBackgroundColor,
            modifier = Modifier
                .padding(vertical = Spacing.small),
        )
    }
}

@Composable
private fun WidgetPreviewWallpaper(
    feedLayout: FeedLayout,
    showWidgetHeader: Boolean,
    fontSizes: WidgetFontSizes,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val wallpaperColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainer,
    )
    val wallpaperShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular)
            .height(230.dp)
            .background(
                brush = Brush.linearGradient(wallpaperColors),
                shape = wallpaperShape,
            )
            .padding(Spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        WidgetPreview(
            feedLayout = feedLayout,
            showWidgetHeader = showWidgetHeader,
            fontSizes = fontSizes,
            backgroundColor = backgroundColor,
            modifier = Modifier.fillMaxWidth(fraction = 0.9f),
        )
    }
}

@Composable
private fun WidgetPreview(
    feedLayout: FeedLayout,
    showWidgetHeader: Boolean,
    fontSizes: WidgetFontSizes,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        shape = shape,
        color = backgroundColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
        ) {
            if (showWidgetHeader) {
                Text(
                    text = strings.widgetLatestItems,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = fontSizes.header.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(Spacing.small))
            }

            val items = listOf(
                WidgetPreviewItem(
                    feedSource = strings.settingsFontScaleFeedSourceExample,
                    title = strings.settingsFontScaleTitleExample,
                    date = "25/12 - 14:30",
                ),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                items.forEach { item ->
                    when (feedLayout) {
                        FeedLayout.LIST -> WidgetPreviewListItem(item, fontSizes)
                        FeedLayout.CARD -> WidgetPreviewCardItem(item, fontSizes)
                    }
                }
            }
        }
    }
}

private data class WidgetPreviewItem(
    val feedSource: String,
    val title: String,
    val date: String,
)

@Composable
private fun WidgetPreviewListItem(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        WidgetPreviewTextContent(
            item = item,
            fontSizes = fontSizes,
            modifier = Modifier.weight(1f),
        )
        WidgetPreviewImage()
    }
}

@Composable
private fun WidgetPreviewCardItem(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.regular),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
        ) {
            WidgetPreviewTextContent(
                item = item,
                fontSizes = fontSizes,
                modifier = Modifier.weight(1f),
            )
            WidgetPreviewImage()
        }
    }
}

@Composable
private fun WidgetPreviewTextContent(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xsmall),
    ) {
        Text(
            text = item.feedSource,
            style = MaterialTheme.typography.bodySmall,
            fontSize = fontSizes.meta.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = fontSizes.title.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.date,
            style = MaterialTheme.typography.bodySmall,
            fontSize = fontSizes.meta.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WidgetPreviewImage(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(50.dp)
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            ),
    )
}
