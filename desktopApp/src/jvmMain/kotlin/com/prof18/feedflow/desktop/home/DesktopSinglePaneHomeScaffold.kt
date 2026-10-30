package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.FeedListActions
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.ShareBehavior
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState

@Composable
internal fun DesktopSinglePaneHomeScaffold(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    snackbarHostState: DesktopFeedbackState,
    onSearchClick: () -> Unit,
    pendingNewArticlesCount: Int,
    onShowNewArticlesClicked: () -> Unit,
    onEmptyStateClick: () -> Unit,
    onImportExportClick: () -> Unit = {},
) {
    DesktopHomeWorkspace(
        displayState = displayState,
        feedListActions = feedListActions,
        feedManagementActions = feedManagementActions,
        shareBehavior = shareBehavior,
        listState = listState,
        gridState = gridState,
        snackbarHostState = snackbarHostState,
        onSearchClick = onSearchClick,
        pendingNewArticlesCount = pendingNewArticlesCount,
        onShowNewArticlesClicked = onShowNewArticlesClicked,
        onEmptyStateClick = onEmptyStateClick,
        onImportExportClick = onImportExportClick,
        multiPane = false,
    )
}

internal fun usesDockedDrawerForWidth(width: Dp): Boolean = width >= 600.dp
