package com.prof18.feedflow.android.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.home.drawer.AndroidDrawer
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch

@Suppress("MultipleEmitters")
@Composable
fun AdaptiveHomeView(
    snackbarHostState: SnackbarHostState,
    onSettingsButtonClicked: () -> Unit,
    onSearchClick: () -> Unit,
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    useDockedDrawer: Boolean = false,
    onBackupClick: () -> Unit = {},
    onFeedSuggestionsClick: () -> Unit = {},
    onImportExportClick: () -> Unit = {},
    onEmptyStateClick: (() -> Unit)? = null,
    onNavigateToNextFeed: (() -> Unit) = {},
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current
    val drawerListState = rememberLazyListState()

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
            showDrawerMenu = showDrawerMenu,
            isDrawerOpen = isDrawerMenuOpen,
            onDrawerMenuClick = onDrawerMenuClick,
            onRefresh = feedListActions.refreshData,
            shareBehavior = shareBehavior,
            onBackupClick = onBackupClick,
            onEmptyStateClick = onEmptyStateClick,
            onNavigateToNextFeed = onNavigateToNextFeed,
        )
    }

    @Composable
    fun DrawerInternal(
        onFeedFilterSelectedLambda: (FeedFilter) -> Unit,
        onAddFeedClick: () -> Unit,
        onFeedSuggestionsClick: () -> Unit,
        onImportExportClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        AndroidDrawer(
            modifier = modifier,
            displayState = displayState,
            feedManagementActions = feedManagementActions,
            onFeedFilterSelected = onFeedFilterSelectedLambda,
            onSettingsClick = onSettingsButtonClicked,
            onAddFeedClick = onAddFeedClick,
            onFeedSuggestionsClick = onFeedSuggestionsClick,
            onImportExportClick = onImportExportClick,
            listState = drawerListState,
        )
    }

    if (useDockedDrawer) {
        var isDrawerMenuFullVisible by remember { mutableStateOf(true) }
        Row(
            modifier = modifier.fillMaxSize(),
        ) {
            Box(
                modifier = if (isDrawerMenuFullVisible) {
                    Modifier.weight(1f)
                } else {
                    Modifier
                        .width(0.dp)
                        .clipToBounds()
                },
            ) {
                DrawerInternal(
                    onFeedFilterSelectedLambda = { feedFilter ->
                        feedManagementActions.onFeedFilterSelected(feedFilter)
                        scope.launch {
                            listState.scrollToItemConditionally(
                                0,
                                reduceMotionEnabled = reduceMotionEnabled,
                            )
                        }
                    },
                    onAddFeedClick = feedManagementActions.onAddFeedClick,
                    onFeedSuggestionsClick = onFeedSuggestionsClick,
                    onImportExportClick = onImportExportClick,
                )
            }

            HomeContentInternal(
                modifier = Modifier.weight(if (isDrawerMenuFullVisible) 2f else 1f),
                showDrawerMenu = true,
                isDrawerMenuOpen = isDrawerMenuFullVisible,
                onDrawerMenuClick = {
                    isDrawerMenuFullVisible = !isDrawerMenuFullVisible
                },
            )
        }
    } else {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    windowInsets = WindowInsets(0),
                ) {
                    DrawerInternal(
                        onFeedFilterSelectedLambda = { feedFilter ->
                            feedManagementActions.onFeedFilterSelected(feedFilter)
                            scope.launch {
                                drawerState.close()
                                listState.scrollToItemConditionally(
                                    0,
                                    reduceMotionEnabled = reduceMotionEnabled,
                                )
                            }
                        },
                        onAddFeedClick = {
                            scope.launch {
                                drawerState.close()
                                feedManagementActions.onAddFeedClick()
                            }
                        },
                        onFeedSuggestionsClick = {
                            scope.launch {
                                drawerState.close()
                                onFeedSuggestionsClick()
                            }
                        },
                        onImportExportClick = {
                            scope.launch {
                                drawerState.close()
                                onImportExportClick()
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
}
