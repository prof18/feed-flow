package com.prof18.feedflow.android.settings.readingbehavior

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.shared.presentation.ReadingBehaviorSettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ReadingBehaviorScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<ReadingBehaviorSettingsViewModel>()
    val browserManager = koinInject<BrowserManager>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val browserListState by browserManager.browserListState.collectAsStateWithLifecycle()

    ReadingBehaviorScreenContent(
        navigateBack = navigateBack,
        state = state,
        browsers = browserListState,
        onBrowserSelected = { browser ->
            browserManager.setFavouriteBrowser(browser)
        },
        setReaderMode = viewModel::updateReaderMode,
        setSaveReaderModeContent = viewModel::updateSaveReaderModeContent,
        setPrefetchArticleContent = viewModel::updatePrefetchArticleContent,
        setMarkReadWhenScrolling = viewModel::updateMarkReadWhenScrolling,
        setShowReadItem = viewModel::updateShowReadItemsOnTimeline,
        setHideReadItems = viewModel::updateHideReadItems,
    )
}
