package com.prof18.feedflow.android.settings.feedlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.presentation.FeedListSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedListSettingsScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<FeedListSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fontSizesState by viewModel.feedFontSizeState.collectAsStateWithLifecycle()

    FeedListSettingsScreenContent(
        navigateBack = navigateBack,
        fontSizes = fontSizesState,
        state = state,
        updateFontScale = viewModel::updateFontScale,
        setFeedLayout = viewModel::updateFeedLayout,
        setHideDescription = viewModel::updateHideDescription,
        setHideImages = viewModel::updateHideImages,
        setHideDate = viewModel::updateHideDate,
        onDateFormatSelected = viewModel::updateDateFormat,
        onTimeFormatSelected = viewModel::updateTimeFormat,
        onSwipeActionSelected = viewModel::updateSwipeAction,
        setRemoveTitleFromDescription = viewModel::updateRemoveTitleFromDescription,
        onFeedOrderSelected = viewModel::updateFeedOrder,
        setHideUnreadCount = viewModel::updateHideUnreadCount,
    )
}
