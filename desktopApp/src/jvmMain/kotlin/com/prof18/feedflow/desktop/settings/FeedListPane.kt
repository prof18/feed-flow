package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedListSettingsState
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ArticleRow
import com.prof18.feedflow.desktop.ui.components.articleCardContainerColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Slider
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeedListPane(
    fontSizesState: FeedFontSizes,
    settingsState: FeedListSettingsState,
    feedOrder: FeedOrder,
    onFontScaleUpdate: (Int) -> Unit,
    onFeedLayoutUpdate: (FeedLayout) -> Unit,
    onGridLayoutEnabledUpdate: (Boolean) -> Unit,
    onHideDescriptionUpdate: (Boolean) -> Unit,
    onHideImagesUpdate: (Boolean) -> Unit,
    onHideDateUpdate: (Boolean) -> Unit,
    onRemoveTitleFromDescUpdate: (Boolean) -> Unit,
    onDateFormatUpdate: (DateFormat) -> Unit,
    onTimeFormatUpdate: (TimeFormat) -> Unit,
    onSwipeActionUpdate: (SwipeDirection, SwipeActionType) -> Unit,
    onFeedOrderSelected: (FeedOrder) -> Unit,
    onHideUnreadDotUpdate: (Boolean) -> Unit,
    onHideFeedSourceUpdate: (Boolean) -> Unit,
    onDescriptionLineLimitUpdate: (DescriptionLineLimit) -> Unit,
    isShowReadItemsEnabled: Boolean = false,
    isHideReadItemsEnabled: Boolean = false,
    isMarkReadWhenScrollingEnabled: Boolean = false,
    onShowReadItemsToggled: (Boolean) -> Unit = {},
    onHideReadItemsToggled: (Boolean) -> Unit = {},
) {
    val strings = LocalFeedFlowStrings.current
    val scale = fontSizesState.scaleFactor
    val latestScale by rememberUpdatedState(scale)
    val latestFontScaleUpdate by rememberUpdatedState(onFontScaleUpdate)
    SettingsPage(
        title = strings.settingsFeedListTitle,
        preview = {
            FeedItemPreview(
                fontSizes = fontSizesState,
                layout = settingsState.feedLayout,
                showDescription = !settingsState.isHideDescriptionEnabled,
                showImage = !settingsState.isHideImagesEnabled,
                showDate = !settingsState.isHideDateEnabled,
                dateFormat = settingsState.dateFormat,
                timeFormat = settingsState.timeFormat,
                displaySettings = FeedItemDisplaySettings(
                    isHideUnreadDotEnabled = settingsState.isHideUnreadDotEnabled,
                    isHideFeedSourceEnabled = settingsState.isHideFeedSourceEnabled,
                    descriptionLineLimit = settingsState.descriptionLineLimit,
                ),
            )
        },
    ) {
        SettingsSection(title = strings.settingsAppearance) {
            SettingsChoice(
                title = strings.feedLayoutTitle,
                selected = settingsState.feedLayout,
                choices = persistentListOf(
                    FeedLayout.LIST to strings.settingsFeedLayoutList,
                    FeedLayout.CARD to strings.settingsFeedLayoutCard,
                    FeedLayout.BIG_IMAGE to strings.settingsFeedLayoutBigImage,
                ),
                onSelected = onFeedLayoutUpdate,
            )
            if (settingsState.feedLayout.supportsGridToggle()) {
                SettingsSwitch(
                    title = strings.settingsFeedLayoutGridToggle,
                    checked = settingsState.isGridLayoutEnabled,
                    onCheckedChange = onGridLayoutEnabledUpdate,
                )
            }
            SettingsBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.settingsFeedListScaleTitle, modifier = Modifier.weight(1f))
                    Text(scale.toString(), color = JewelTheme.globalColors.text.info)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Tooltip(tooltip = { Text(strings.decreaseFontSizeButtonContentDescription) }) {
                        OutlinedButton(
                            onClick = {
                                if (latestScale > FONT_SCALE_MIN) latestFontScaleUpdate(latestScale - 1)
                            },
                            enabled = scale > FONT_SCALE_MIN,
                            modifier = Modifier.width(FONT_SCALE_BUTTON_WIDTH).semantics {
                                contentDescription = strings.decreaseFontSizeButtonContentDescription
                            },
                        ) { Text("A", fontSize = 11.sp) }
                    }
                    Slider(
                        value = scale.toFloat(),
                        onValueChange = { onFontScaleUpdate(it.toInt()) },
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = strings.settingsFeedListScaleTitle
                        },
                        valueRange = FONT_SCALE_MIN.toFloat()..FONT_SCALE_MAX.toFloat(),
                        steps = FONT_SCALE_STEPS,
                    )
                    Tooltip(tooltip = { Text(strings.increaseFontSizeButtonContentDescription) }) {
                        OutlinedButton(
                            onClick = {
                                if (latestScale < FONT_SCALE_MAX) latestFontScaleUpdate(latestScale + 1)
                            },
                            enabled = scale < FONT_SCALE_MAX,
                            modifier = Modifier.width(FONT_SCALE_BUTTON_WIDTH).semantics {
                                contentDescription = strings.increaseFontSizeButtonContentDescription
                            },
                        ) { Text("A", fontSize = 17.sp) }
                    }
                }
            }
            SettingsChoice(
                title = strings.settingsDescriptionMaxLines,
                selected = settingsState.descriptionLineLimit,
                choices = persistentListOf(
                    DescriptionLineLimit.THREE to strings.settingsDescriptionLinesThree,
                    DescriptionLineLimit.FIVE to strings.settingsDescriptionLinesFive,
                    DescriptionLineLimit.NO_LIMIT to strings.settingsDescriptionLinesNoLimit,
                ),
                onSelected = onDescriptionLineLimitUpdate,
            )
        }

        SettingsSection {
            SettingsSwitch(
                title = strings.settingsHideDescription,
                checked = settingsState.isHideDescriptionEnabled,
                onCheckedChange = onHideDescriptionUpdate,
            )
            SettingsSwitch(
                title = strings.settingsHideImages,
                checked = settingsState.isHideImagesEnabled,
                onCheckedChange = onHideImagesUpdate,
            )
            SettingsSwitch(
                title = strings.settingsHideDate,
                checked = settingsState.isHideDateEnabled,
                onCheckedChange = onHideDateUpdate,
            )
            SettingsSwitch(
                title = strings.settingsHideUnreadDot,
                checked = settingsState.isHideUnreadDotEnabled,
                onCheckedChange = onHideUnreadDotUpdate,
            )
            SettingsSwitch(
                title = strings.settingsHideFeedSource,
                checked = settingsState.isHideFeedSourceEnabled,
                onCheckedChange = onHideFeedSourceUpdate,
            )
            SettingsSwitch(
                title = strings.settingsHideDuplicatedTitleFromDesc,
                checked = settingsState.isRemoveTitleFromDescriptionEnabled,
                onCheckedChange = onRemoveTitleFromDescUpdate,
            )
            SettingsChoice(
                title = strings.dateFormatTitle,
                selected = settingsState.dateFormat,
                choices = persistentListOf(
                    DateFormat.NORMAL to strings.dateFormatNormal,
                    DateFormat.DAY_MONTH_DOT to strings.dateFormatDayMonthDot,
                    DateFormat.AMERICAN to strings.dateFormatAmerican,
                    DateFormat.ISO to strings.dateFormatIso,
                ),
                onSelected = onDateFormatUpdate,
            )
            SettingsChoice(
                title = strings.timeFormatTitle,
                selected = settingsState.timeFormat,
                choices = persistentListOf(
                    TimeFormat.HOURS_24 to strings.timeFormatHours24,
                    TimeFormat.HOURS_12 to strings.timeFormatHours12,
                ),
                onSelected = onTimeFormatUpdate,
            )
        }

        SettingsSection(title = strings.settingsReadingBehavior) {
            FeedVisibilitySettings(
                isShowReadItemsEnabled = isShowReadItemsEnabled,
                isHideReadItemsEnabled = isHideReadItemsEnabled,
                isMarkReadWhenScrollingEnabled = isMarkReadWhenScrollingEnabled,
                onShowReadItemsToggled = onShowReadItemsToggled,
                onHideReadItemsToggled = onHideReadItemsToggled,
            )
            SettingsChoice(
                title = strings.settingsFeedOrderTitle,
                selected = feedOrder,
                choices = persistentListOf(
                    FeedOrder.NEWEST_FIRST to strings.settingsFeedOrderNewestFirst,
                    FeedOrder.OLDEST_FIRST to strings.settingsFeedOrderOldestFirst,
                ),
                onSelected = onFeedOrderSelected,
            )
            SettingsChoice(
                title = strings.settingsLeftSwipeAction,
                selected = settingsState.leftSwipeActionType,
                choices = swipeActionOptions(strings),
                onSelected = { onSwipeActionUpdate(SwipeDirection.LEFT, it) },
            )
            SettingsChoice(
                title = strings.settingsRightSwipeAction,
                selected = settingsState.rightSwipeActionType,
                choices = swipeActionOptions(strings),
                onSelected = { onSwipeActionUpdate(SwipeDirection.RIGHT, it) },
            )
        }
    }
}

@Composable
internal fun FeedVisibilitySettings(
    isShowReadItemsEnabled: Boolean,
    isHideReadItemsEnabled: Boolean,
    isMarkReadWhenScrollingEnabled: Boolean,
    onShowReadItemsToggled: (Boolean) -> Unit,
    onHideReadItemsToggled: (Boolean) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    SettingsSwitch(
        title = strings.settingsToggleShowReadArticles,
        checked = isShowReadItemsEnabled,
        onCheckedChange = onShowReadItemsToggled,
    )
    SettingsSwitch(
        title = strings.settingsHideReadItems,
        checked = isHideReadItemsEnabled,
        onCheckedChange = onHideReadItemsToggled,
        enabled = !isMarkReadWhenScrollingEnabled,
        supportingText = if (isMarkReadWhenScrollingEnabled) {
            strings.settingsHideReadItemsDisabledWhileScrolling
        } else {
            null
        },
    )
}

// Renders the same row the article list uses, so every option below previews exactly as it will look.
@Composable
private fun FeedItemPreview(
    fontSizes: FeedFontSizes,
    layout: FeedLayout,
    showDescription: Boolean,
    showImage: Boolean,
    showDate: Boolean,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    displaySettings: FeedItemDisplaySettings,
) {
    val strings = LocalFeedFlowStrings.current
    val rowLayout = if (layout == FeedLayout.GRID) FeedLayout.BIG_IMAGE else layout
    val item = FeedItem(
        id = "settings-preview",
        url = "https://www.example.com",
        title = strings.settingsFontScaleTitleExample,
        subtitle = strings.settingsFontScaleSubtitleExample.takeIf { showDescription },
        content = null,
        imageUrl = PREVIEW_IMAGE_URL.takeIf { showImage },
        feedSource = FeedSource(
            id = "settings-preview-source",
            url = "https://www.example.com",
            title = strings.settingsFontScaleFeedSourceExample,
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        ),
        pubDateMillis = null,
        isRead = false,
        dateString = feedPreviewDate(dateFormat, timeFormat).takeIf { showDate },
        commentsUrl = null,
        isBookmarked = false,
    )
    Box(
        modifier = Modifier.fillMaxWidth()
            .then(if (rowLayout != FeedLayout.LIST) Modifier.background(articleCardContainerColor()) else Modifier)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        ArticleRow(
            feedItem = item,
            feedFontSize = fontSizes,
            shareMenuLabel = strings.menuShare,
            shareCommentsMenuLabel = strings.menuShareComments,
            feedLayout = rowLayout,
            currentFeedFilter = FeedFilter.Timeline,
            feedItemDisplaySettings = displaySettings,
            onFeedItemClick = {},
            onOpenInBrowser = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            onOpenFeedSettings = {},
            onOpenFeedWebsite = {},
            onShareClick = {},
            onMarkAllAboveAsRead = {},
            onMarkAllBelowAsRead = {},
            modifier = Modifier.widthIn(
                max = if (rowLayout == FeedLayout.BIG_IMAGE) 300.dp else 560.dp,
            ),
        )
    }
}

private const val PREVIEW_IMAGE_URL = "https://picsum.photos/seed/feedflow/800/450"
private const val FONT_SCALE_MIN = -4
private val FONT_SCALE_BUTTON_WIDTH = 40.dp
private const val FONT_SCALE_MAX = 16
private const val FONT_SCALE_STEPS = 19

private fun feedPreviewDate(dateFormat: DateFormat, timeFormat: TimeFormat): String {
    val date = when (dateFormat) {
        DateFormat.NORMAL -> "25/12"
        DateFormat.DAY_MONTH_DOT -> "25.12"
        DateFormat.AMERICAN -> "12/25"
        DateFormat.ISO -> "2025-12-25"
    }
    val time = when (timeFormat) {
        TimeFormat.HOURS_24 -> "14:30"
        TimeFormat.HOURS_12 -> "2:30 PM"
    }
    return "$date - $time"
}

private fun FeedLayout.supportsGridToggle(): Boolean = this == FeedLayout.CARD || this == FeedLayout.BIG_IMAGE

private fun swipeActionOptions(
    strings: com.prof18.feedflow.i18n.FeedFlowStrings,
): ImmutableList<Pair<SwipeActionType, String>> = persistentListOf(
    SwipeActionType.TOGGLE_READ_STATUS to strings.settingsSwipeActionToggleRead,
    SwipeActionType.TOGGLE_BOOKMARK_STATUS to strings.settingsSwipeActionToggleBookmark,
    SwipeActionType.OPEN_IN_BROWSER to strings.settingsSwipeActionOpenInBrowser,
    SwipeActionType.NONE to strings.settingsSwipeActionNone,
)
