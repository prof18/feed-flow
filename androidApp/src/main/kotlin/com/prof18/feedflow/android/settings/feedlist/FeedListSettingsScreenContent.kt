package com.prof18.feedflow.android.settings.feedlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
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
internal fun FeedListSettingsScreenContent(
    navigateBack: () -> Unit,
    fontSizes: FeedFontSizes,
    state: FeedListSettingsState,
    updateFontScale: (Int) -> Unit,
    setFeedLayout: (FeedLayout) -> Unit,
    setHideDescription: (Boolean) -> Unit,
    setHideImages: (Boolean) -> Unit,
    setHideDate: (Boolean) -> Unit,
    onDateFormatSelected: (DateFormat) -> Unit,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    onSwipeActionSelected: (SwipeDirection, SwipeActionType) -> Unit,
    setRemoveTitleFromDescription: (Boolean) -> Unit,
    onFeedOrderSelected: (FeedOrder) -> Unit,
    setHideUnreadDot: (Boolean) -> Unit,
    setHideFeedSource: (Boolean) -> Unit,
    onDescriptionLineLimitSelected: (DescriptionLineLimit) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsFeedListTitle) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            FeedItemPreview(
                fontSizes = fontSizes,
                feedLayout = state.feedLayout,
                isHideDescriptionEnabled = state.isHideDescriptionEnabled,
                isHideImagesEnabled = state.isHideImagesEnabled,
                isHideDateEnabled = state.isHideDateEnabled,
                dateFormat = state.dateFormat,
                timeFormat = state.timeFormat,
                feedItemDisplaySettings = FeedItemDisplaySettings(
                    isHideUnreadDotEnabled = state.isHideUnreadDotEnabled,
                    isHideFeedSourceEnabled = state.isHideFeedSourceEnabled,
                    descriptionLineLimit = state.descriptionLineLimit,
                ),
            )

            LazyColumn {
                item {
                    Text(
                        text = LocalFeedFlowStrings.current.settingsFeedListScaleTitle,
                        modifier = Modifier.padding(Spacing.regular),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                item {
                    SliderWithPlusMinus(
                        modifier = Modifier.padding(horizontal = Spacing.regular),
                        value = fontSizes.scaleFactor.toFloat(),
                        onValueChange = { updateFontScale(it.toInt()) },
                        valueRange = -4f..16f,
                        steps = 20,
                    )
                }

                item {
                    Spacer(modifier = Modifier.padding(top = Spacing.regular))
                    CompactSettingDropdownRow(
                        title = strings.feedLayoutTitle,
                        currentValue = state.feedLayout,
                        options = persistentListOf(
                            SettingDropdownOption(FeedLayout.LIST, strings.settingsFeedLayoutList),
                            SettingDropdownOption(FeedLayout.CARD, strings.settingsFeedLayoutCard),
                        ),
                        onOptionSelected = setFeedLayout,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsHideDescription,
                        isChecked = state.isHideDescriptionEnabled,
                        onCheckedChange = setHideDescription,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsHideImages,
                        isChecked = state.isHideImagesEnabled,
                        onCheckedChange = setHideImages,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsHideDate,
                        isChecked = state.isHideDateEnabled,
                        onCheckedChange = setHideDate,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsHideUnreadDot,
                        isChecked = state.isHideUnreadDotEnabled,
                        onCheckedChange = setHideUnreadDot,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsHideFeedSource,
                        isChecked = state.isHideFeedSourceEnabled,
                        onCheckedChange = setHideFeedSource,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsHideDuplicatedTitleFromDesc,
                        isChecked = state.isRemoveTitleFromDescriptionEnabled,
                        onCheckedChange = setRemoveTitleFromDescription,
                    )
                }

                item {
                    CompactSettingDropdownRow(
                        title = strings.settingsDescriptionMaxLines,
                        currentValue = state.descriptionLineLimit,
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
                        onOptionSelected = onDescriptionLineLimitSelected,
                    )
                }

                item {
                    CompactSettingDropdownRow(
                        title = strings.dateFormatTitle,
                        currentValue = state.dateFormat,
                        options = persistentListOf(
                            SettingDropdownOption(DateFormat.NORMAL, strings.dateFormatNormal),
                            SettingDropdownOption(DateFormat.AMERICAN, strings.dateFormatAmerican),
                            SettingDropdownOption(DateFormat.ISO, strings.dateFormatIso),
                        ),
                        onOptionSelected = onDateFormatSelected,
                    )
                }

                item {
                    CompactSettingDropdownRow(
                        title = strings.timeFormatTitle,
                        currentValue = state.timeFormat,
                        options = persistentListOf(
                            SettingDropdownOption(TimeFormat.HOURS_24, strings.timeFormatHours24),
                            SettingDropdownOption(TimeFormat.HOURS_12, strings.timeFormatHours12),
                        ),
                        onOptionSelected = onTimeFormatSelected,
                    )
                }

                item {
                    CompactSettingDropdownRow(
                        title = strings.settingsFeedOrderTitle,
                        currentValue = state.feedOrder,
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
                }

                item {
                    CompactSettingDropdownRow(
                        title = strings.settingsLeftSwipeAction,
                        currentValue = state.leftSwipeActionType,
                        options = swipeActionOptions(
                            toggleReadLabel = strings.settingsSwipeActionToggleRead,
                            toggleBookmarkLabel = strings.settingsSwipeActionToggleBookmark,
                            openInBrowserLabel = strings.settingsSwipeActionOpenInBrowser,
                            noneLabel = strings.settingsSwipeActionNone,
                        ),
                        onOptionSelected = { action -> onSwipeActionSelected(SwipeDirection.LEFT, action) },
                    )
                }

                item {
                    CompactSettingDropdownRow(
                        title = strings.settingsRightSwipeAction,
                        currentValue = state.rightSwipeActionType,
                        options = swipeActionOptions(
                            toggleReadLabel = strings.settingsSwipeActionToggleRead,
                            toggleBookmarkLabel = strings.settingsSwipeActionToggleBookmark,
                            openInBrowserLabel = strings.settingsSwipeActionOpenInBrowser,
                            noneLabel = strings.settingsSwipeActionNone,
                        ),
                        onOptionSelected = { action -> onSwipeActionSelected(SwipeDirection.RIGHT, action) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                }
            }
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
private fun FeedListSettingsScreenContentPreview() {
    FeedFlowTheme {
        FeedListSettingsScreenContent(
            navigateBack = {},
            fontSizes = FeedFontSizes(),
            state = FeedListSettingsState(
                isHideDescriptionEnabled = false,
                isHideImagesEnabled = true,
                isHideDateEnabled = false,
                dateFormat = DateFormat.NORMAL,
                timeFormat = TimeFormat.HOURS_24,
                feedLayout = FeedLayout.LIST,
                feedOrder = FeedOrder.NEWEST_FIRST,
            ),
            updateFontScale = {},
            setFeedLayout = {},
            setHideDescription = {},
            setHideImages = {},
            setHideDate = {},
            onDateFormatSelected = {},
            onTimeFormatSelected = {},
            onSwipeActionSelected = { _, _ -> },
            setRemoveTitleFromDescription = {},
            onFeedOrderSelected = {},
            setHideUnreadDot = {},
            setHideFeedSource = {},
            onDescriptionLineLimitSelected = {},
        )
    }
}
