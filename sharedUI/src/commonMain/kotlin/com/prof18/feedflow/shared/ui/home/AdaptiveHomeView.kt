package com.prof18.feedflow.shared.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.home.components.HomeScreenContent
import com.prof18.feedflow.shared.ui.utils.ConditionalAnimatedVisibility
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch

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
    windowSizeClass: WindowSizeClass = WindowSizeClass.Compact,
    showDropdownMenu: Boolean = false,
    feedContentWrapper: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
    onBackupClick: () -> Unit = {},
    onFeedSuggestionsClick: () -> Unit = {},
    onEmptyStateClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current

    @Composable
    fun HomeContentInternal(
        showDrawerMenu: Boolean,
        modifier: Modifier = Modifier,
        isDrawerMenuOpen: Boolean = false,
        onDrawerMenuClick: () -> Unit,
    ) {
        HomeScreenContent(
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
            showDropdownMenu = showDropdownMenu,
            feedContentWrapper = feedContentWrapper,
            shareBehavior = shareBehavior,
            onBackupClick = onBackupClick,
            onEmptyStateClick = onEmptyStateClick,
        )
    }

    @Composable
    fun DrawerInternal(
        modifier: Modifier = Modifier,
        onFeedFilterSelectedLambda: (FeedFilter) -> Unit,
    ) {
        Drawer(
            modifier = modifier,
            displayState = displayState,
            feedManagementActions = feedManagementActions,
            onFeedFilterSelected = onFeedFilterSelectedLambda,
            onFeedSuggestionsClick = onFeedSuggestionsClick,
        )
    }

    when (windowSizeClass) {
        WindowSizeClass.Compact -> {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        DrawerInternal(
                            onFeedFilterSelectedLambda = { feedFilter ->
                                feedManagementActions.onFeedFilterSelected(feedFilter)
                                scope.launch {
                                    drawerState.close()
                                    if (reduceMotionEnabled) {
                                        listState.scrollToItem(0)
                                    } else {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            },
                        )
                    }
                },
            ) {
                HomeContentInternal(
                    showDrawerMenu = true,
                    onDrawerMenuClick = {
                        scope.launch {
                            if (drawerState.isOpen) {
                                drawerState.close()
                            } else {
                                drawerState.open()
                            }
                        }
                    },
                )
            }
        }
        WindowSizeClass.Medium, WindowSizeClass.Expanded -> {
            var isDrawerMenuFullVisible by remember { mutableStateOf(true) }
            Row {
                ConditionalAnimatedVisibility(
                    modifier = Modifier.weight(1f),
                    visible = isDrawerMenuFullVisible,
                ) {
                    Scaffold { paddingValues ->
                        DrawerInternal(
                            modifier = Modifier.padding(paddingValues),
                            onFeedFilterSelectedLambda = { feedFilter ->
                                feedManagementActions.onFeedFilterSelected(feedFilter)
                                scope.launch {
                                    listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                                }
                            },
                        )
                    }
                }

                HomeContentInternal(
                    modifier = Modifier.weight(2f),
                    showDrawerMenu = true,
                    isDrawerMenuOpen = isDrawerMenuFullVisible,
                    onDrawerMenuClick = {
                        isDrawerMenuFullVisible = !isDrawerMenuFullVisible
                    },
                )
            }
        }
    }
}

enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded,
}
