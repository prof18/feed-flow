package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.presentation.model.FeedListSettingsState
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.shared.ui.settings.FeedItemPreview
import com.prof18.feedflow.shared.ui.settings.SettingDropdownOption
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun FeedListPane(
    fontSizesState: FeedFontSizes,
    settingsState: FeedListSettingsState,
    feedOrder: FeedOrder,
    onFontScaleUpdate: (Int) -> Unit,
    onFeedLayoutUpdate: (FeedLayout) -> Unit,
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

    Column(modifier = Modifier.fillMaxSize()) {
        FeedItemPreview(
            fontSizes = fontSizesState,
            feedLayout = settingsState.feedLayout,
            isHideDescriptionEnabled = settingsState.isHideDescriptionEnabled,
            isHideImagesEnabled = settingsState.isHideImagesEnabled,
            isHideDateEnabled = settingsState.isHideDateEnabled,
            dateFormat = settingsState.dateFormat,
            timeFormat = settingsState.timeFormat,
            feedItemDisplaySettings = FeedItemDisplaySettings(
                isHideUnreadDotEnabled = settingsState.isHideUnreadDotEnabled,
                isHideFeedSourceEnabled = settingsState.isHideFeedSourceEnabled,
                descriptionLineLimit = settingsState.descriptionLineLimit,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = LocalFeedFlowStrings.current.settingsFeedListScaleTitle,
                modifier = Modifier.padding(Spacing.regular),
                style = MaterialTheme.typography.bodyLarge,
            )

            SliderWithPlusMinus(
                modifier = Modifier.padding(horizontal = Spacing.regular),
                value = fontSizesState.scaleFactor.toFloat(),
                onValueChange = { onFontScaleUpdate(it.toInt()) },
                valueRange = -4f..16f,
                steps = 20,
            )

            CompactSettingDropdownRow(
                title = strings.feedLayoutTitle,
                currentValue = settingsState.feedLayout,
                options = persistentListOf(
                    SettingDropdownOption(FeedLayout.LIST, strings.settingsFeedLayoutList),
                    SettingDropdownOption(FeedLayout.CARD, strings.settingsFeedLayoutCard),
                ),
                onOptionSelected = onFeedLayoutUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideDescription,
                isChecked = settingsState.isHideDescriptionEnabled,
                onCheckedChange = onHideDescriptionUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideImages,
                isChecked = settingsState.isHideImagesEnabled,
                onCheckedChange = onHideImagesUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideDate,
                isChecked = settingsState.isHideDateEnabled,
                onCheckedChange = onHideDateUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideUnreadDot,
                isChecked = settingsState.isHideUnreadDotEnabled,
                onCheckedChange = onHideUnreadDotUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideFeedSource,
                isChecked = settingsState.isHideFeedSourceEnabled,
                onCheckedChange = onHideFeedSourceUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideDuplicatedTitleFromDesc,
                isChecked = settingsState.isRemoveTitleFromDescriptionEnabled,
                onCheckedChange = onRemoveTitleFromDescUpdate,
            )

            CompactSettingDropdownRow(
                title = strings.settingsDescriptionMaxLines,
                currentValue = settingsState.descriptionLineLimit,
                options = persistentListOf(
                    SettingDropdownOption(
                        DescriptionLineLimit.THREE,
                        strings.settingsDescriptionLinesThree,
                    ),
                    SettingDropdownOption(
                        DescriptionLineLimit.FIVE,
                        strings.settingsDescriptionLinesFive,
                    ),
                    SettingDropdownOption(
                        DescriptionLineLimit.NO_LIMIT,
                        strings.settingsDescriptionLinesNoLimit,
                    ),
                ),
                onOptionSelected = onDescriptionLineLimitUpdate,
            )

            CompactSettingDropdownRow(
                title = strings.settingsFeedOrderTitle,
                currentValue = feedOrder,
                options = persistentListOf(
                    SettingDropdownOption(
                        FeedOrder.NEWEST_FIRST,
                        strings.settingsFeedOrderNewestFirst,
                    ),
                    SettingDropdownOption(
                        FeedOrder.OLDEST_FIRST,
                        strings.settingsFeedOrderOldestFirst,
                    ),
                ),
                onOptionSelected = onFeedOrderSelected,
            )

            CompactSettingDropdownRow(
                title = strings.dateFormatTitle,
                currentValue = settingsState.dateFormat,
                options = persistentListOf(
                    SettingDropdownOption(DateFormat.NORMAL, strings.dateFormatNormal),
                    SettingDropdownOption(DateFormat.AMERICAN, strings.dateFormatAmerican),
                    SettingDropdownOption(DateFormat.ISO, strings.dateFormatIso),
                ),
                onOptionSelected = onDateFormatUpdate,
            )

            CompactSettingDropdownRow(
                title = strings.timeFormatTitle,
                currentValue = settingsState.timeFormat,
                options = persistentListOf(
                    SettingDropdownOption(TimeFormat.HOURS_24, strings.timeFormatHours24),
                    SettingDropdownOption(TimeFormat.HOURS_12, strings.timeFormatHours12),
                ),
                onOptionSelected = onTimeFormatUpdate,
            )

            CompactSettingDropdownRow(
                title = strings.settingsLeftSwipeAction,
                currentValue = settingsState.leftSwipeActionType,
                options = swipeActionOptions(
                    toggleReadLabel = strings.settingsSwipeActionToggleRead,
                    toggleBookmarkLabel = strings.settingsSwipeActionToggleBookmark,
                    openInBrowserLabel = strings.settingsSwipeActionOpenInBrowser,
                    noneLabel = strings.settingsSwipeActionNone,
                ),
                onOptionSelected = { action -> onSwipeActionUpdate(SwipeDirection.LEFT, action) },
            )

            CompactSettingDropdownRow(
                title = strings.settingsRightSwipeAction,
                currentValue = settingsState.rightSwipeActionType,
                options = swipeActionOptions(
                    toggleReadLabel = strings.settingsSwipeActionToggleRead,
                    toggleBookmarkLabel = strings.settingsSwipeActionToggleBookmark,
                    openInBrowserLabel = strings.settingsSwipeActionOpenInBrowser,
                    noneLabel = strings.settingsSwipeActionNone,
                ),
                onOptionSelected = { action -> onSwipeActionUpdate(SwipeDirection.RIGHT, action) },
            )
        }
    }
}

private fun swipeActionOptions(
    toggleReadLabel: String,
    toggleBookmarkLabel: String,
    openInBrowserLabel: String,
    noneLabel: String,
): ImmutableList<SettingDropdownOption<SwipeActionType>> = persistentListOf(
    SettingDropdownOption(
        SwipeActionType.TOGGLE_READ_STATUS,
        toggleReadLabel,
    ),
    SettingDropdownOption(
        SwipeActionType.TOGGLE_BOOKMARK_STATUS,
        toggleBookmarkLabel,
    ),
    SettingDropdownOption(
        SwipeActionType.OPEN_IN_BROWSER,
        openInBrowserLabel,
    ),
    SettingDropdownOption(
        SwipeActionType.NONE,
        noneLabel,
    ),
)

@Preview
@Composable
private fun FeedListPanePreview() {
    FeedFlowTheme {
        FeedListPane(
            fontSizesState = FeedFontSizes(),
            settingsState = FeedListSettingsState(),
            feedOrder = FeedOrder.NEWEST_FIRST,
            onFontScaleUpdate = {},
            onFeedLayoutUpdate = {},
            onHideDescriptionUpdate = {},
            onHideImagesUpdate = {},
            onHideDateUpdate = {},
            onRemoveTitleFromDescUpdate = {},
            onDateFormatUpdate = {},
            onTimeFormatUpdate = {},
            onSwipeActionUpdate = { _, _ -> },
            onFeedOrderSelected = {},
            onHideUnreadDotUpdate = {},
            onHideFeedSourceUpdate = {},
            onDescriptionLineLimitUpdate = {},
        )
    }
}
