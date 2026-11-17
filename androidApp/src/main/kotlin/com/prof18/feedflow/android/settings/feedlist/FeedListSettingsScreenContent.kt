package com.prof18.feedflow.android.settings.feedlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.android.settings.SettingsE2eIds
import com.prof18.feedflow.android.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.android.ui.readermode.wholeValueSteps
import com.prof18.feedflow.android.ui.settings.CompactSettingDropdownRow
import com.prof18.feedflow.android.ui.settings.FeedItemPreview
import com.prof18.feedflow.android.ui.settings.SettingDropdownOption
import com.prof18.feedflow.android.ui.settings.SettingRowDefaults
import com.prof18.feedflow.android.ui.settings.SettingSwitchItem
import com.prof18.feedflow.android.ui.settings.SettingsSectionHeader
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedListSettingsState
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Composable
internal fun FeedListSettingsScreenContent(
    navigateBack: () -> Unit,
    fontSizes: FeedFontSizes,
    state: FeedListSettingsState,
    updateFontScale: (Int) -> Unit,
    setFeedLayout: (FeedLayout) -> Unit,
    setGridLayoutEnabled: (Boolean) -> Unit,
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
                    IconButton(
                        modifier = Modifier.testTag(SettingsE2eIds.BACK_BUTTON),
                        onClick = navigateBack,
                    ) {
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
                    SettingsSectionHeader(title = strings.settingsFeedListSectionLayout)
                }

                item {
                    CompactSettingDropdownRow(
                        modifier = Modifier.testTag(SettingsE2eIds.FEED_LIST_LAYOUT),
                        title = strings.feedLayoutTitle,
                        currentValue = state.feedLayout,
                        options = persistentListOf(
                            SettingDropdownOption(FeedLayout.LIST, strings.settingsFeedLayoutList),
                            SettingDropdownOption(FeedLayout.CARD, strings.settingsFeedLayoutCard),
                            SettingDropdownOption(FeedLayout.BIG_IMAGE, strings.settingsFeedLayoutBigImage),
                        ),
                        onOptionSelected = setFeedLayout,
                    )
                }

                if (state.feedLayout.supportsGridToggle()) {
                    item {
                        SettingSwitchItem(
                            title = strings.settingsFeedLayoutGridToggle,
                            isChecked = state.isGridLayoutEnabled,
                            onCheckedChange = setGridLayoutEnabled,
                        )
                    }
                }

                item {
                    TextScaleHeader(
                        scaleFactor = fontSizes.scaleFactor,
                        onReset = { updateFontScale(0) },
                    )
                }

                item {
                    SliderWithPlusMinus(
                        modifier = Modifier.padding(horizontal = Spacing.regular),
                        value = fontSizes.scaleFactor.toFloat(),
                        onValueChange = { updateFontScale(it.roundToInt()) },
                        valueRange = FONT_SCALE_RANGE,
                        steps = wholeValueSteps(FONT_SCALE_RANGE),
                    )
                }

                item {
                    SettingsSectionHeader(title = strings.settingsFeedListSectionShowInList)
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsShowDescription,
                        isChecked = !state.isHideDescriptionEnabled,
                        onCheckedChange = { setHideDescription(!it) },
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
                        enabled = !state.isHideDescriptionEnabled,
                        supportingText = requiresParentText(
                            isParentOff = state.isHideDescriptionEnabled,
                            text = strings.settingsRequiresDescription,
                        ),
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsHideDuplicatedTitleFromDesc,
                        isChecked = state.isRemoveTitleFromDescriptionEnabled,
                        onCheckedChange = setRemoveTitleFromDescription,
                        enabled = !state.isHideDescriptionEnabled,
                        supportingText = strings.settingsRequiresDescription.takeIf {
                            state.isHideDescriptionEnabled
                        },
                    )
                }

                item {
                    SettingSwitchItem(
                        modifier = Modifier.testTag(SettingsE2eIds.FEED_LIST_HIDE_IMAGES),
                        title = strings.settingsShowImages,
                        isChecked = !state.isHideImagesEnabled,
                        onCheckedChange = { setHideImages(!it) },
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsShowDate,
                        isChecked = !state.isHideDateEnabled,
                        onCheckedChange = { setHideDate(!it) },
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsShowUnreadDot,
                        isChecked = !state.isHideUnreadDotEnabled,
                        onCheckedChange = { setHideUnreadDot(!it) },
                    )
                }

                item {
                    SettingSwitchItem(
                        title = strings.settingsShowFeedSource,
                        isChecked = !state.isHideFeedSourceEnabled,
                        onCheckedChange = { setHideFeedSource(!it) },
                    )
                }

                item {
                    SettingsSectionHeader(title = strings.settingsFeedListSectionDateTime)
                }

                item {
                    CompactSettingDropdownRow(
                        title = strings.dateFormatTitle,
                        currentValue = state.dateFormat,
                        options = persistentListOf(
                            SettingDropdownOption(DateFormat.NORMAL, strings.dateFormatNormal),
                            SettingDropdownOption(DateFormat.DAY_MONTH_DOT, strings.dateFormatDayMonthDot),
                            SettingDropdownOption(DateFormat.AMERICAN, strings.dateFormatAmerican),
                            SettingDropdownOption(DateFormat.ISO, strings.dateFormatIso),
                        ),
                        onOptionSelected = onDateFormatSelected,
                        enabled = !state.isHideDateEnabled,
                        supportingText = requiresParentText(
                            isParentOff = state.isHideDateEnabled,
                            text = strings.settingsRequiresDate,
                        ),
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
                        enabled = !state.isHideDateEnabled,
                        supportingText = requiresParentText(
                            isParentOff = state.isHideDateEnabled,
                            text = strings.settingsRequiresDate,
                        ),
                    )
                }

                item {
                    SettingsSectionHeader(title = strings.settingsFeedListSectionOrder)
                }

                item {
                    CompactSettingDropdownRow(
                        modifier = Modifier.testTag(SettingsE2eIds.FEED_LIST_ORDER),
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
                    SettingsSectionHeader(title = strings.settingsFeedListSectionGestures)
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

@Composable
private fun TextScaleHeader(
    scaleFactor: Int,
    onReset: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = strings.settingsFeedListScaleTitle,
            modifier = Modifier.weight(1f),
            style = SettingRowDefaults.titleStyle,
        )
        Text(
            text = scaleLabel(scaleFactor, strings.settingsFeedListScaleDefault),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (scaleFactor != 0) {
            TextButton(
                modifier = Modifier.testTag(SettingsE2eIds.FEED_LIST_SCALE_RESET),
                onClick = onReset,
            ) {
                Text(strings.resetButton)
            }
        }
    }
}

private fun scaleLabel(scaleFactor: Int, defaultLabel: String): String = when {
    scaleFactor == 0 -> defaultLabel
    scaleFactor > 0 -> "+$scaleFactor"
    else -> scaleFactor.toString()
}

private fun requiresParentText(
    isParentOff: Boolean,
    text: String,
): (@Composable () -> Unit)? = if (isParentOff) {
    { Text(text) }
} else {
    null
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

private fun FeedLayout.supportsGridToggle(): Boolean =
    this == FeedLayout.CARD || this == FeedLayout.BIG_IMAGE

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
            setGridLayoutEnabled = {},
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

private val FONT_SCALE_RANGE = -4f..16f
