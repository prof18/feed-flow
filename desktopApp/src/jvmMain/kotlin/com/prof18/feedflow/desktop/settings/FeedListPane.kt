package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.LabelOff
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.prof18.feedflow.shared.ui.settings.DateFormatSelector
import com.prof18.feedflow.shared.ui.settings.DescriptionLineLimitSelector
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
    var showFeedOrderDialog by remember { mutableStateOf(false) }

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

            FeedLayoutSelector(
                feedLayout = settingsState.feedLayout,
                onFeedLayoutSelected = onFeedLayoutUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideDescription,
                icon = Icons.Outlined.SubtitlesOff,
                isChecked = settingsState.isHideDescriptionEnabled,
                onCheckedChange = onHideDescriptionUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideImages,
                icon = Icons.Outlined.HideImage,
                isChecked = settingsState.isHideImagesEnabled,
                onCheckedChange = onHideImagesUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideDate,
                icon = Icons.Outlined.EventBusy,
                isChecked = settingsState.isHideDateEnabled,
                onCheckedChange = onHideDateUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideUnreadDot,
                icon = Icons.Outlined.FiberManualRecord,
                isChecked = settingsState.isHideUnreadDotEnabled,
                onCheckedChange = onHideUnreadDotUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideFeedSource,
                icon = Icons.Outlined.LabelOff,
                isChecked = settingsState.isHideFeedSourceEnabled,
                onCheckedChange = onHideFeedSourceUpdate,
            )

            SettingSwitchItem(
                title = LocalFeedFlowStrings.current.settingsHideDuplicatedTitleFromDesc,
                icon = Icons.Outlined.HideSource,
                isChecked = settingsState.isRemoveTitleFromDescriptionEnabled,
                onCheckedChange = onRemoveTitleFromDescUpdate,
            )

            DescriptionLineLimitSelector(
                currentLimit = settingsState.descriptionLineLimit,
                onLimitSelected = onDescriptionLineLimitUpdate,
            )

            val feedOrderLabel = when (feedOrder) {
                FeedOrder.NEWEST_FIRST -> strings.settingsFeedOrderNewestFirst
                FeedOrder.OLDEST_FIRST -> strings.settingsFeedOrderOldestFirst
            }

            SettingSelectorItem(
                title = strings.settingsFeedOrderTitle,
                currentValueLabel = feedOrderLabel,
                icon = Icons.AutoMirrored.Outlined.Sort,
                onClick = { showFeedOrderDialog = true },
            )

            if (showFeedOrderDialog) {
                FeedOrderDialog(
                    currentFeedOrder = feedOrder,
                    onFeedOrderSelected = { selected ->
                        onFeedOrderSelected(selected)
                        showFeedOrderDialog = false
                    },
                    onDismiss = { showFeedOrderDialog = false },
                )
            }

            DateFormatSelector(
                currentFormat = settingsState.dateFormat,
                onFormatSelected = onDateFormatUpdate,
            )

            TimeFormatSelector(
                currentFormat = settingsState.timeFormat,
                onFormatSelected = onTimeFormatUpdate,
            )

            SwipeActionSelector(
                direction = SwipeDirection.LEFT,
                currentAction = settingsState.leftSwipeActionType,
                onActionSelected = { action ->
                    onSwipeActionUpdate(SwipeDirection.LEFT, action)
                },
            )

            SwipeActionSelector(
                direction = SwipeDirection.RIGHT,
                currentAction = settingsState.rightSwipeActionType,
                onActionSelected = { action ->
                    onSwipeActionUpdate(SwipeDirection.RIGHT, action)
                },
            )
        }
    }
}

@Composable
private fun FeedOrderDialog(
    currentFeedOrder: FeedOrder,
    onFeedOrderSelected: (FeedOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val orders = listOf(
        FeedOrder.NEWEST_FIRST to strings.settingsFeedOrderNewestFirst,
        FeedOrder.OLDEST_FIRST to strings.settingsFeedOrderOldestFirst,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingsFeedOrderTitle) },
        text = {
            Column {
                orders.forEach { (order, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentFeedOrder == order,
                                onClick = { onFeedOrderSelected(order) },
                            )
                            .padding(vertical = Spacing.small),
                    ) {
                        RadioButton(
                            selected = currentFeedOrder == order,
                            onClick = { onFeedOrderSelected(order) },
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = Spacing.small),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelButton)
            }
        },
    )
}

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
