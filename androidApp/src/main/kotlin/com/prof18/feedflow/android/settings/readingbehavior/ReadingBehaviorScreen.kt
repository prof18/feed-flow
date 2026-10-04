package com.prof18.feedflow.android.settings.readingbehavior

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.core.utils.BrowserIds
import com.prof18.feedflow.shared.presentation.ReadingBehaviorSettingsViewModel
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList
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
    val strings = LocalFeedFlowStrings.current

    ReadingBehaviorScreenContent(
        navigateBack = navigateBack,
        state = state,
        browsers = browserListState.map { browser ->
            if (browser.id == BrowserIds.IN_APP_BROWSER) {
                browser.copy(name = strings.inAppBrowser)
            } else {
                browser
            }
        }.toImmutableList(),
        onBrowserSelected = { browser ->
            browserManager.setFavouriteBrowser(browser)
        },
        setArticleOpenMode = viewModel::updateArticleOpenMode,
        setSaveReaderModeContent = viewModel::updateSaveReaderModeContent,
        setPrefetchArticleContent = viewModel::updatePrefetchArticleContent,
        setMarkReadWhenScrolling = viewModel::updateMarkReadWhenScrolling,
        setShowReadItem = viewModel::updateShowReadItemsOnTimeline,
        setHideReadItems = viewModel::updateHideReadItems,
    )
}
