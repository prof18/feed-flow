package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.main.LocalDesktopMenuCommands
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.FeedListActions
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.ShareBehavior
import com.prof18.feedflow.desktop.ui.components.DesktopFeedSidebar
import com.prof18.feedflow.desktop.ui.components.ReaderEmptyState
import com.prof18.feedflow.desktop.ui.scrollToItemConditionally
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme

@Composable
internal fun DesktopHomeScaffold(
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
    currentReaderArticle: FeedItemUrlInfo?,
    onReaderClosed: () -> Unit,
    onEmptyStateClick: () -> Unit,
    onImportExportClick: () -> Unit = {},
    searchContent: (@Composable () -> Unit)? = null,
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
        currentReaderArticle = currentReaderArticle,
        onReaderClosed = onReaderClosed,
        onEmptyStateClick = onEmptyStateClick,
        onImportExportClick = onImportExportClick,
        multiPane = true,
        searchContent = searchContent,
    )
}

@Composable
internal fun DesktopHomeWorkspace(
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
    onImportExportClick: () -> Unit,
    multiPane: Boolean,
    currentReaderArticle: FeedItemUrlInfo? = null,
    onReaderClosed: () -> Unit = {},
    searchContent: (@Composable () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val repository = remember { DI.koin.get<DesktopHomeSettingsRepository>() }
    val state = remember(repository) {
        DesktopWorkspaceState(
            drawerVisible = repository.isDrawerVisible(),
            sidebarWidth = repository.getRequestedSidebarWidth()?.dp,
            listWidth = repository.getRequestedListWidth()?.dp,
            legacySplitIndex = repository.getPaneExpansionIndex(),
            onDrawerVisibilityChanged = repository::setDrawerVisible,
            onSidebarWidthChanged = { repository.setRequestedSidebarWidth(it.value) },
            onListWidthChanged = { repository.setRequestedListWidth(it.value) },
        )
    }
    val drawerState = rememberDrawerState()
    // Each pane pads itself below the transparent macOS title bar so its own background reaches the top edge.
    val topPadding = if (getDesktopOS().isMacOs()) 16.dp else 0.dp
    // The sidebar's first row sits right under the traffic lights, so it needs extra clearance.
    val sidebarTopPadding = if (getDesktopOS().isMacOs()) 28.dp else 0.dp
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = state.layout(maxWidth, multiPane, readerSelected = currentReaderArticle != null)
        SideEffect(maxWidth, multiPane, state.isDrawerVisible) { state.seedListWidth(maxWidth, multiPane) }
        SideEffect(layout.isSidebarDocked) { if (layout.isSidebarDocked) drawerState.close() }
        SideEffect(currentReaderArticle) {
            if (currentReaderArticle == null && state.isReaderFullscreen) state.toggleReaderFullscreen()
        }
        val toggleSidebar: () -> Unit = {
            if (layout.canDockSidebar) {
                state.toggleDrawer()
            } else {
                scope.launch {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
            }
        }
        val menuCommands = LocalDesktopMenuCommands.current
        SideEffect { menuCommands.toggleSidebar = toggleSidebar }
        DisposableEffect(menuCommands) { onDispose { menuCommands.toggleSidebar = null } }
        val drawerContent: @Composable () -> Unit = {
            Box(
                Modifier.fillMaxSize()
                    .background(JewelTheme.globalColors.toolwindowBackground)
                    .padding(top = sidebarTopPadding),
            ) {
                DesktopFeedSidebar(
                    displayState = displayState,
                    feedManagementActions = feedManagementActions,
                    onFeedFilterSelected = { filter: FeedFilter ->
                        feedManagementActions.onFeedFilterSelected(filter)
                        scope.launch {
                            listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotion)
                            drawerState.close()
                        }
                    },
                )
            }
        }
        ModalDrawer(
            isOpen = drawerState.isOpen && !layout.isSidebarDocked,
            onDismiss = { scope.launch { drawerState.close() } },
            drawerContent = drawerContent,
            paneContent = {
                DesktopWorkspace(
                    layout = layout,
                    onSidebarResize = { width, persist ->
                        state.resizeSidebar(width, persist)
                        state.layout(maxWidth, multiPane).sidebarWidth
                    },
                    onListResize = { width, persist ->
                        state.resizeList(width, maxWidth, multiPane, persist)
                        state.layout(maxWidth, multiPane).listWidth
                    },
                    sidebarContent = drawerContent,
                    listContent = {
                        Box(
                            Modifier.fillMaxSize().clipToBounds()
                                .background(JewelTheme.globalColors.panelBackground)
                                .padding(top = topPadding),
                        ) {
                            if (searchContent != null) {
                                searchContent()
                                return@Box
                            }
                            DesktopHomeScreenContent(
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
                                onImportExportClick = onImportExportClick,
                                isGridLayoutAllowed = !multiPane,
                                isDrawerOpen = layout.isSidebarDocked || drawerState.isOpen,
                                onDrawerMenuClick = toggleSidebar,
                                onEmptyStateClick = onEmptyStateClick,
                                selectedArticleId = currentReaderArticle?.id,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                    readerContent = {
                        Box(
                            Modifier.fillMaxSize()
                                .background(JewelTheme.globalColors.panelBackground)
                                .padding(top = topPadding),
                        ) {
                            ReaderPane(currentReaderArticle, onReaderClosed, layout.canSplit, state)
                        }
                    },
                )
            },
        )
    }
}

@Composable
private fun ReaderPane(
    currentReaderArticle: FeedItemUrlInfo?,
    onReaderClosed: () -> Unit,
    canSplit: Boolean,
    state: DesktopWorkspaceState,
) {
    if (currentReaderArticle != null) {
        ReaderModeScreen(
            feedItemUrlInfo = currentReaderArticle,
            navigateBack = onReaderClosed,
            showBackButton = !canSplit,
            isDetailFullscreen = state.isReaderFullscreen,
            onToggleDetailFullscreen = state::toggleReaderFullscreen.takeIf { canSplit },
        )
    } else {
        ReaderEmptyState()
    }
}

internal val drawerPaneWidth = 248.dp
internal val toolbarHeight = 56.dp
internal val listPaneTopContentFadeHeight = 30.dp
internal val listPaneTopContentPadding = 4.dp
internal val listPaneMaxContentWidth = 720.dp
