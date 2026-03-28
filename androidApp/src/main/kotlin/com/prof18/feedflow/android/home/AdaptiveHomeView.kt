package com.prof18.feedflow.android.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.prof18.feedflow.android.home.drawer.AndroidDrawer
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.data.AndroidHomeSettingsRepository
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Suppress("MultipleEmitters", "ModifierMissing")
@Composable
fun AdaptiveHomeView(
    snackbarHostState: SnackbarHostState,
    onSettingsButtonClicked: () -> Unit,
    onSearchClick: () -> Unit,
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState = rememberLazyListState(),
    feedContentWrapper: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
    onBackupClick: () -> Unit = {},
    onFeedSuggestionsClick: () -> Unit = {},
    onEmptyStateClick: (() -> Unit)? = null,
    onNavigateToNextFeed: (() -> Unit) = {},
    currentReaderArticle: FeedItemUrlInfo? = null,
    readerModeState: ReaderModeState = ReaderModeState.Loading,
    readerFontSize: Int = 16,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    canNavigatePrevious: Boolean = false,
    canNavigateNext: Boolean = false,
    onReaderClosed: () -> Unit = {},
    onUpdateReaderFontSize: (Int) -> Unit = {},
    onReaderBookmarkClick: (FeedItemId, Boolean) -> Unit = { _, _ -> },
    onNavigateToPreviousArticle: () -> Unit = {},
    onNavigateToNextArticle: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current
    val homeSettingsRepository = koinInject<AndroidHomeSettingsRepository>()

    @Composable
    fun HomeContentInternal(
        showDrawerMenu: Boolean,
        modifier: Modifier = Modifier,
        isDrawerMenuOpen: Boolean = false,
        onDrawerMenuClick: () -> Unit,
    ) {
        AndroidHomeScreenContent(
            modifier = modifier,
            displayState = displayState,
            feedListActions = feedListActions,
            feedManagementActions = feedManagementActions,
            listState = listState,
            snackbarHostState = snackbarHostState,
            onSearchClick = onSearchClick,
            onSettingsButtonClicked = onSettingsButtonClicked,
            showDrawerMenu = showDrawerMenu,
            isDrawerOpen = isDrawerMenuOpen,
            onDrawerMenuClick = onDrawerMenuClick,
            onRefresh = feedListActions.refreshData,
            feedContentWrapper = feedContentWrapper,
            shareBehavior = shareBehavior,
            onBackupClick = onBackupClick,
            onEmptyStateClick = onEmptyStateClick,
            onNavigateToNextFeed = onNavigateToNextFeed,
        )
    }

    @Composable
    fun DrawerInternal(
        onFeedFilterSelectedLambda: (FeedFilter) -> Unit,
        modifier: Modifier = Modifier,
        onFeedSuggestionsClickLambda: () -> Unit = onFeedSuggestionsClick,
    ) {
        AndroidDrawer(
            modifier = modifier,
            displayState = displayState,
            feedManagementActions = feedManagementActions,
            onFeedFilterSelected = onFeedFilterSelectedLambda,
            onFeedSuggestionsClick = onFeedSuggestionsClickLambda,
        )
    }

    AndroidThreePaneHomeScaffold(
        currentReaderArticle = currentReaderArticle,
        readerModeState = readerModeState,
        readerFontSize = readerFontSize,
        themeMode = themeMode,
        canNavigatePrevious = canNavigatePrevious,
        canNavigateNext = canNavigateNext,
        initialPaneExpansionIndex = homeSettingsRepository.getPaneExpansionIndex(),
        onReaderClosed = onReaderClosed,
        onUpdateReaderFontSize = onUpdateReaderFontSize,
        onReaderBookmarkClick = onReaderBookmarkClick,
        onNavigateToPreviousArticle = onNavigateToPreviousArticle,
        onNavigateToNextArticle = onNavigateToNextArticle,
        onPaneExpansionIndexChanged = homeSettingsRepository::setPaneExpansionIndex,
        drawerPane = { modifier, _, onCloseDrawer ->
            DrawerInternal(
                modifier = modifier,
                onFeedFilterSelectedLambda = { feedFilter ->
                    feedManagementActions.onFeedFilterSelected(feedFilter)
                    onCloseDrawer()
                    scope.launch {
                        listState.scrollToItemConditionally(
                            0,
                            reduceMotionEnabled = reduceMotionEnabled,
                        )
                    }
                },
                onFeedSuggestionsClickLambda = {
                    onCloseDrawer()
                    onFeedSuggestionsClick()
                },
            )
        },
        listPane = { modifier, isDrawerOpen, onDrawerClick ->
            HomeContentInternal(
                modifier = modifier,
                showDrawerMenu = true,
                isDrawerMenuOpen = isDrawerOpen,
                onDrawerMenuClick = onDrawerClick,
            )
        },
    )
}
