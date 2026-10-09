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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun WidgetPreviewSection(
    settingsState: WidgetSettingsState,
    modifier: Modifier = Modifier,
) {
    var previewBackdropMode by rememberSaveable { mutableStateOf(WidgetPreviewBackdropMode.LIGHT) }
    var showPreviewNote by rememberSaveable { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current
    val baseBackgroundColor = settingsState.backgroundColor?.let(::widgetColorFromArgb)
        ?: MaterialTheme.colorScheme.surface

    @Suppress("MagicNumber")
    val backgroundAlpha = settingsState.backgroundOpacityPercent.coerceIn(minimumValue = 0, maximumValue = 100) / 100f
    val previewWallpaperBaseColor = previewBackdropMode.underlayColor
    val effectiveBackgroundColor = widgetEffectiveBackgroundColor(
        backgroundColor = baseBackgroundColor,
        backgroundOpacity = backgroundAlpha,
        underlayColor = previewWallpaperBaseColor,
    )
    val textColors = when {
        settingsState.backgroundColor != null -> {
            widgetTextColorsForMode(settingsState.textColorMode, effectiveBackgroundColor)
        }
        settingsState.textColorMode != WidgetTextColorMode.AUTOMATIC -> {
            widgetTextColorsForMode(settingsState.textColorMode, effectiveBackgroundColor)
        }
        else -> null
    }
    val primaryTextColor = textColors?.primary ?: MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = textColors?.secondary ?: MaterialTheme.colorScheme.onSurfaceVariant
    val previewBackgroundColor = baseBackgroundColor.copy(alpha = backgroundAlpha)
    val fontSizes = widgetFontSizes(settingsState.fontScale)

    Column(
        modifier = modifier.testTag("widget_preview"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.widgetBackgroundColorPreview,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showPreviewNote = true },
                modifier = Modifier.testTag("widget_preview_info"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = strings.widgetPreviewInfo,
                )
            }
            PreviewBackdropToggleButton(
                backdropMode = previewBackdropMode,
                onClick = { previewBackdropMode = previewBackdropMode.next() },
                modifier = Modifier.testTag("widget_preview_toggle_background"),
            )
        }
        WidgetPreviewWallpaper(
            headerTitle = settingsState.widgetContentTitle(),
            feedLayout = settingsState.feedLayout,
            showWidgetHeader = settingsState.showHeader,
            showRefreshButton = settingsState.showRefreshButton,
            hideImages = settingsState.hideImages,
            fontSizes = fontSizes,
            backgroundColor = previewBackgroundColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            backdropMode = previewBackdropMode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular)
                .padding(bottom = Spacing.xsmall)
                .heightIn(min = 148.dp),
        )
    }

    if (showPreviewNote) {
        AlertDialog(
            onDismissRequest = { showPreviewNote = false },
            modifier = Modifier
                .testTag("widget_preview_note_dialog")
                .semantics { testTagsAsResourceId = true },
            title = { Text(text = strings.widgetPreviewInfo) },
            text = { Text(text = strings.widgetPreviewNote) },
            confirmButton = {
                TextButton(
                    onClick = { showPreviewNote = false },
                ) {
                    Text(text = strings.closeButtonContentDescription)
                }
            },
        )
    }
}

@Composable
private fun WidgetPreviewWallpaper(
    headerTitle: String,
    feedLayout: WidgetFeedLayout,
    showWidgetHeader: Boolean,
    showRefreshButton: Boolean,
    hideImages: Boolean,
    fontSizes: WidgetFontSizes,
    backgroundColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    backdropMode: WidgetPreviewBackdropMode,
    modifier: Modifier = Modifier,
) {
    val wallpaperShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(backdropMode.wallpaperColors),
                shape = wallpaperShape,
            )
            .padding(Spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        WidgetPreview(
            headerTitle = headerTitle,
            feedLayout = feedLayout,
            showWidgetHeader = showWidgetHeader,
            showRefreshButton = showRefreshButton,
            hideImages = hideImages,
            fontSizes = fontSizes,
            backgroundColor = backgroundColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PreviewBackdropToggleButton(
    backdropMode: WidgetPreviewBackdropMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current
    val backgroundColor = if (backdropMode == WidgetPreviewBackdropMode.LIGHT) {
        Color.White.copy(alpha = 0.88f)
    } else {
        PreviewToggleDarkChromeColor.copy(alpha = 0.96f)
    }
    val contentColor = if (backdropMode == WidgetPreviewBackdropMode.LIGHT) {
        PreviewToggleDarkChromeColor
    } else {
        Color.White
    }
    val icon = if (backdropMode == WidgetPreviewBackdropMode.LIGHT) {
        Icons.Outlined.LightMode
    } else {
        Icons.Outlined.DarkMode
    }

    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color = backgroundColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = strings.widgetPreviewToggleBackground,
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun WidgetPreview(
    headerTitle: String,
    feedLayout: WidgetFeedLayout,
    showWidgetHeader: Boolean,
    showRefreshButton: Boolean,
    hideImages: Boolean,
    fontSizes: WidgetFontSizes,
    backgroundColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
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
                .padding(Spacing.small),
        ) {
            if (showWidgetHeader || showRefreshButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showWidgetHeader) {
                        Text(
                            text = headerTitle,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = fontSizes.header.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (showRefreshButton) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = strings.refreshFeeds,
                            tint = primaryTextColor,
                            modifier = Modifier.size(width = 24.dp, height = 24.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xsmall))
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
                        WidgetFeedLayout.LIST -> WidgetPreviewListItem(
                            item = item,
                            fontSizes = fontSizes,
                            hideImages = hideImages,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                        )
                        WidgetFeedLayout.CARD -> WidgetPreviewCardItem(item, fontSizes, hideImages)
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
    hideImages: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
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
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            modifier = Modifier.weight(1f),
        )
        if (!hideImages) {
            WidgetPreviewImage()
        }
    }
}

@Composable
private fun WidgetPreviewCardItem(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
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
                primaryTextColor = MaterialTheme.colorScheme.onSurface,
                secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!hideImages) {
                WidgetPreviewImage()
            }
        }
    }
}

@Composable
private fun WidgetPreviewTextContent(
    item: WidgetPreviewItem,
    fontSizes: WidgetFontSizes,
    primaryTextColor: Color,
    secondaryTextColor: Color,
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
            color = secondaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = fontSizes.title.sp,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.date,
            style = MaterialTheme.typography.bodySmall,
            fontSize = fontSizes.meta.sp,
            color = secondaryTextColor,
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

private enum class WidgetPreviewBackdropMode(
    val underlayColor: Color,
    val wallpaperColors: List<Color>,
) {
    LIGHT(
        underlayColor = PreviewLightUnderlayColor,
        wallpaperColors = PreviewLightWallpaperColors,
    ),
    DARK(
        underlayColor = PreviewDarkUnderlayColor,
        wallpaperColors = PreviewDarkWallpaperColors,
    ),
}

private fun WidgetPreviewBackdropMode.next(): WidgetPreviewBackdropMode =
    when (this) {
        WidgetPreviewBackdropMode.LIGHT -> WidgetPreviewBackdropMode.DARK
        WidgetPreviewBackdropMode.DARK -> WidgetPreviewBackdropMode.LIGHT
    }

private val PreviewToggleDarkChromeColor = Color(0xFF20283A)
private val PreviewLightUnderlayColor = Color(0xFFF3F5FA)
private val PreviewLightWallpaperColors = listOf(
    Color(0xFFE9EEF8),
    Color(0xFFF4F6FB),
    Color(0xFFE1E7F2),
)
private val PreviewDarkUnderlayColor = Color(0xFF151B28)
private val PreviewDarkWallpaperColors = listOf(
    Color(0xFF0E1420),
    Color(0xFF1A2232),
    Color(0xFF111827),
)

@Preview
@Composable
private fun WidgetPreviewSectionPreview() {
    FeedFlowTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            WidgetPreviewSection(
                settingsState = WidgetSettingsState(
                    syncPeriod = SyncPeriod.ONE_HOUR,
                    feedLayout = WidgetFeedLayout.LIST,
                    showHeader = true,
                    fontScale = 0,
                    backgroundColor = 0xFFC7B2E7.toInt(),
                    backgroundOpacityPercent = 60,
                    textColorMode = WidgetTextColorMode.AUTOMATIC,
                    hideImages = false,
                ),
            )
        }
    }
}
