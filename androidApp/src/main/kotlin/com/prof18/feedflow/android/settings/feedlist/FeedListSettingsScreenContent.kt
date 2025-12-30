package com.prof18.feedflow.android.settings.feedlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.android.settings.components.FeedOrderSelectionDialog
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.presentation.model.FeedListSettingsState
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.settings.DateFormatSelector
import com.prof18.feedflow.shared.ui.settings.FeedItemPreview
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.settings.SwipeActionSelector
import com.prof18.feedflow.shared.ui.settings.TimeFormatSelector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsFeedListTitle) },
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
                    FeedLayoutSelector(
                        feedLayout = state.feedLayout,
                        onFeedLayoutSelected = setFeedLayout,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideDescription,
                        icon = Icons.Outlined.SubtitlesOff,
                        isChecked = state.isHideDescriptionEnabled,
                        onCheckedChange = setHideDescription,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideImages,
                        icon = Icons.Outlined.HideImage,
                        isChecked = state.isHideImagesEnabled,
                        onCheckedChange = setHideImages,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideDate,
                        icon = Icons.Outlined.EventBusy,
                        isChecked = state.isHideDateEnabled,
                        onCheckedChange = setHideDate,
                    )
                }

                item {
                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideDuplicatedTitleFromDesc,
                        icon = Icons.Outlined.HideSource,
                        isChecked = state.isRemoveTitleFromDescriptionEnabled,
                        onCheckedChange = setRemoveTitleFromDescription,
                    )
                }

                item {
                    DateFormatSelector(
                        currentFormat = state.dateFormat,
                        onFormatSelected = onDateFormatSelected,
                    )
                }

                item {
                    TimeFormatSelector(
                        currentFormat = state.timeFormat,
                        onFormatSelected = onTimeFormatSelected,
                    )
                }

                item {
                    val feedOrderLabel = when (state.feedOrder) {
                        FeedOrder.NEWEST_FIRST -> LocalFeedFlowStrings.current.settingsFeedOrderNewestFirst
                        FeedOrder.OLDEST_FIRST -> LocalFeedFlowStrings.current.settingsFeedOrderOldestFirst
                    }
                    var showDialog by remember { mutableStateOf(false) }

                    SettingSelectorItem(
                        title = LocalFeedFlowStrings.current.settingsFeedOrderTitle,
                        currentValueLabel = feedOrderLabel,
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        onClick = { showDialog = true },
                    )

                    if (showDialog) {
                        FeedOrderSelectionDialog(
                            currentFeedOrder = state.feedOrder,
                            onFeedOrderSelected = onFeedOrderSelected,
                            dismissDialog = { showDialog = false },
                        )
                    }
                }

                item {
                    SwipeActionSelector(
                        direction = SwipeDirection.LEFT,
                        currentAction = state.leftSwipeActionType,
                        onActionSelected = { action ->
                            onSwipeActionSelected(SwipeDirection.LEFT, action)
                        },
                    )
                }

                item {
                    SwipeActionSelector(
                        direction = SwipeDirection.RIGHT,
                        currentAction = state.rightSwipeActionType,
                        onActionSelected = { action ->
                            onSwipeActionSelected(SwipeDirection.RIGHT, action)
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                }
            }
        }
    }
}

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
        )
    }
}
