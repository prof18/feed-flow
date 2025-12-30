package com.prof18.feedflow.desktop.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.presentation.model.FeedListSettingsState
import com.prof18.feedflow.shared.ui.readermode.SliderWithPlusMinus
import com.prof18.feedflow.shared.ui.settings.DateFormatSelector
import com.prof18.feedflow.shared.ui.settings.FeedItemPreview
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.settings.SwipeActionSelector
import com.prof18.feedflow.shared.ui.settings.TimeFormatSelector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedListAppearanceDialog(
    visible: Boolean,
    onCloseRequest: () -> Unit,
    fontSizesState: FeedFontSizes,
    settingsState: FeedListSettingsState,
    callbacks: FeedListAppearanceCallbacks,
) {
    val dialogState = rememberDialogState(
        size = DpSize(500.dp, 720.dp),
    )

    DialogWindow(
        state = dialogState,
        title = LocalFeedFlowStrings.current.feedListAppearance,
        visible = visible,
        onCloseRequest = onCloseRequest,
    ) {
        Scaffold { paddingValues ->
            val scrollableState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(paddingValues),
            ) {
                FeedItemPreview(
                    fontSizes = fontSizesState,
                    feedLayout = settingsState.feedLayout,
                    isHideDescriptionEnabled = settingsState.isHideDescriptionEnabled,
                    isHideImagesEnabled = settingsState.isHideImagesEnabled,
                    isHideDateEnabled = settingsState.isHideDateEnabled,
                    dateFormat = settingsState.dateFormat,
                    timeFormat = settingsState.timeFormat,
                )

                Column(
                    modifier = Modifier
                        .verticalScroll(scrollableState),
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.settingsFeedListScaleTitle,
                        modifier = Modifier.padding(Spacing.regular),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    SliderWithPlusMinus(
                        modifier = Modifier.padding(horizontal = Spacing.regular),
                        value = fontSizesState.scaleFactor.toFloat(),
                        onValueChange = { callbacks.onFontScaleUpdate(it.toInt()) },
                        valueRange = -4f..16f,
                        steps = 20,
                    )

                    Spacer(modifier = Modifier.padding(top = Spacing.regular))

                    FeedLayoutSelector(
                        feedLayout = settingsState.feedLayout,
                        onFeedLayoutSelected = { feedLayout ->
                            callbacks.onFeedLayoutUpdate(feedLayout)
                        },
                    )

                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideDescription,
                        icon = Icons.Outlined.SubtitlesOff,
                        isChecked = settingsState.isHideDescriptionEnabled,
                        onCheckedChange = { callbacks.onHideDescriptionUpdate(it) },
                    )

                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideImages,
                        icon = Icons.Outlined.HideImage,
                        isChecked = settingsState.isHideImagesEnabled,
                        onCheckedChange = { callbacks.onHideImagesUpdate(it) },
                    )

                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideDate,
                        icon = Icons.Outlined.EventBusy,
                        isChecked = settingsState.isHideDateEnabled,
                        onCheckedChange = { callbacks.onHideDateUpdate(it) },
                    )

                    SettingSwitchItem(
                        title = LocalFeedFlowStrings.current.settingsHideDuplicatedTitleFromDesc,
                        icon = Icons.Outlined.HideSource,
                        isChecked = settingsState.isRemoveTitleFromDescriptionEnabled,
                        onCheckedChange = { callbacks.onRemoveTitleFromDescUpdate(it) },
                    )

                    DateFormatSelector(
                        currentFormat = settingsState.dateFormat,
                        onFormatSelected = { format ->
                            callbacks.onDateFormatUpdate(format)
                        },
                    )

                    TimeFormatSelector(
                        currentFormat = settingsState.timeFormat,
                        onFormatSelected = { format ->
                            callbacks.onTimeFormatUpdate(format)
                        },
                    )

                    SwipeActionSelector(
                        direction = SwipeDirection.LEFT,
                        currentAction = settingsState.leftSwipeActionType,
                        onActionSelected = { action ->
                            callbacks.onSwipeActionUpdate(SwipeDirection.LEFT, action)
                        },
                    )

                    SwipeActionSelector(
                        direction = SwipeDirection.RIGHT,
                        currentAction = settingsState.rightSwipeActionType,
                        onActionSelected = { action ->
                            callbacks.onSwipeActionUpdate(SwipeDirection.RIGHT, action)
                        },
                    )
                }
            }
        }
    }
}

internal data class FeedListAppearanceCallbacks(
    val onFontScaleUpdate: (Int) -> Unit,
    val onFeedLayoutUpdate: (FeedLayout) -> Unit,
    val onHideDescriptionUpdate: (Boolean) -> Unit,
    val onHideImagesUpdate: (Boolean) -> Unit,
    val onHideDateUpdate: (Boolean) -> Unit,
    val onRemoveTitleFromDescUpdate: (Boolean) -> Unit,
    val onDateFormatUpdate: (DateFormat) -> Unit,
    val onTimeFormatUpdate: (TimeFormat) -> Unit,
    val onSwipeActionUpdate: (SwipeDirection, SwipeActionType) -> Unit,
)
