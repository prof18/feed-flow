package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FEED_DESC_FONT_SIZE
import com.prof18.feedflow.core.model.FEED_META_FONT_SIZE
import com.prof18.feedflow.core.model.FEED_TITLE_FONT_SIZE
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedListSettingsState
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.jewel.foundation.theme.LocalTextStyle
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
) {
    val strings = LocalFeedFlowStrings.current
    val scale = fontSizesState.scaleFactor
    val latestScale by rememberUpdatedState(scale)
    val latestFontScaleUpdate by rememberUpdatedState(onFontScaleUpdate)
    SettingsScrollPane(modifier = Modifier.fillMaxSize()) {
        FeedItemPreview(
            fontScale = scale,
            layout = settingsState.feedLayout,
            showDescription = !settingsState.isHideDescriptionEnabled,
            showImage = !settingsState.isHideImagesEnabled,
            showDate = !settingsState.isHideDateEnabled,
            dateFormat = settingsState.dateFormat,
            timeFormat = settingsState.timeFormat,
            showFeedSource = !settingsState.isHideFeedSourceEnabled,
            showUnreadDot = !settingsState.isHideUnreadDotEnabled,
            descriptionLines = settingsState.descriptionLineLimit,
        )

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SettingsGroup(strings.settingsAppearance)
            Text(strings.settingsFeedListScaleTitle, modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Tooltip(tooltip = { Text(strings.decreaseFontSizeButtonContentDescription) }) {
                    OutlinedButton(
                        onClick = {
                            if (latestScale > FONT_SCALE_MIN) latestFontScaleUpdate(latestScale - 1)
                        },
                        enabled = scale > FONT_SCALE_MIN,
                        modifier = Modifier.semantics {
                            contentDescription = strings.decreaseFontSizeButtonContentDescription
                        },
                    ) { Text("−") }
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
                        modifier = Modifier.semantics {
                            contentDescription = strings.increaseFontSizeButtonContentDescription
                        },
                    ) { Text("+") }
                }
                Text(scale.toString())
            }

            JewelSettingsChoice(
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
                SettingsCheckbox(
                    strings.settingsFeedLayoutGridToggle,
                    settingsState.isGridLayoutEnabled,
                    onGridLayoutEnabledUpdate,
                )
            }
            SettingsCheckbox(
                strings.settingsHideDescription,
                settingsState.isHideDescriptionEnabled,
                onHideDescriptionUpdate,
            )
            SettingsCheckbox(strings.settingsHideImages, settingsState.isHideImagesEnabled, onHideImagesUpdate)
            SettingsCheckbox(strings.settingsHideDate, settingsState.isHideDateEnabled, onHideDateUpdate)
            SettingsCheckbox(
                strings.settingsHideUnreadDot,
                settingsState.isHideUnreadDotEnabled,
                onHideUnreadDotUpdate,
            )
            SettingsCheckbox(
                strings.settingsHideFeedSource,
                settingsState.isHideFeedSourceEnabled,
                onHideFeedSourceUpdate,
            )
            SettingsCheckbox(
                strings.settingsHideDuplicatedTitleFromDesc,
                settingsState.isRemoveTitleFromDescriptionEnabled,
                onRemoveTitleFromDescUpdate,
            )
            JewelSettingsChoice(
                title = strings.settingsDescriptionMaxLines,
                selected = settingsState.descriptionLineLimit,
                choices = persistentListOf(
                    DescriptionLineLimit.THREE to strings.settingsDescriptionLinesThree,
                    DescriptionLineLimit.FIVE to strings.settingsDescriptionLinesFive,
                    DescriptionLineLimit.NO_LIMIT to strings.settingsDescriptionLinesNoLimit,
                ),
                onSelected = onDescriptionLineLimitUpdate,
            )
            JewelSettingsChoice(
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
            JewelSettingsChoice(
                title = strings.timeFormatTitle,
                selected = settingsState.timeFormat,
                choices = persistentListOf(
                    TimeFormat.HOURS_24 to strings.timeFormatHours24,
                    TimeFormat.HOURS_12 to strings.timeFormatHours12,
                ),
                onSelected = onTimeFormatUpdate,
            )
            SettingsGroup(strings.settingsReadingBehavior)
            JewelSettingsChoice(
                title = strings.settingsFeedOrderTitle,
                selected = feedOrder,
                choices = persistentListOf(
                    FeedOrder.NEWEST_FIRST to strings.settingsFeedOrderNewestFirst,
                    FeedOrder.OLDEST_FIRST to strings.settingsFeedOrderOldestFirst,
                ),
                onSelected = onFeedOrderSelected,
            )
            JewelSettingsChoice(
                title = strings.settingsLeftSwipeAction,
                selected = settingsState.leftSwipeActionType,
                choices = swipeActionOptions(strings),
                onSelected = { onSwipeActionUpdate(SwipeDirection.LEFT, it) },
            )
            JewelSettingsChoice(
                title = strings.settingsRightSwipeAction,
                selected = settingsState.rightSwipeActionType,
                choices = swipeActionOptions(strings),
                onSelected = { onSwipeActionUpdate(SwipeDirection.RIGHT, it) },
            )
        }
    }
}

@Composable
private fun FeedItemPreview(
    fontScale: Int,
    layout: FeedLayout,
    showDescription: Boolean,
    showImage: Boolean,
    showDate: Boolean,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    showFeedSource: Boolean,
    showUnreadDot: Boolean,
    descriptionLines: DescriptionLineLimit,
) {
    val strings = LocalFeedFlowStrings.current
    val titleFontSize = (FEED_TITLE_FONT_SIZE + fontScale).coerceAtLeast(1).sp
    val descriptionFontSize = (FEED_DESC_FONT_SIZE + fontScale).coerceAtLeast(1).sp
    val metadataFontSize = (FEED_META_FONT_SIZE + fontScale).coerceAtLeast(1).sp
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        SettingsGroup(strings.settingsFeedListTitle)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showUnreadDot) Text("•  ")
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.settingsFontScaleTitleExample,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = LocalTextStyle.current.copy(fontSize = titleFontSize),
                )
                if (showFeedSource) {
                    Text(
                        strings.settingsFontScaleFeedSourceExample,
                        style = LocalTextStyle.current.copy(fontSize = metadataFontSize),
                    )
                }
                if (showDate) {
                    Text(
                        feedPreviewDate(dateFormat, timeFormat),
                        style = LocalTextStyle.current.copy(fontSize = metadataFontSize),
                    )
                }
                if (showDescription) {
                    Text(
                        text = strings.settingsFontScaleSubtitleExample,
                        modifier = Modifier.padding(top = 4.dp),
                        style = LocalTextStyle.current.copy(fontSize = descriptionFontSize),
                        maxLines = descriptionLines.lines,
                    )
                }
            }
            if (showImage && layout != FeedLayout.LIST) Text("▧")
        }
    }
}

private const val FONT_SCALE_MIN = -4
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
