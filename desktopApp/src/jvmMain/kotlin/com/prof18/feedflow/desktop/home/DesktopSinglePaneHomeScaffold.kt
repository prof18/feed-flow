package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.components.drawerHazeStyle
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.components.HomeScreenContent
import com.prof18.feedflow.shared.ui.home.components.drawer.Drawer
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun DesktopSinglePaneHomeScaffold(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    onEmptyStateClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current
    val focusManager = LocalFocusManager.current
    val homeSettingsRepository = DI.koin.get<DesktopHomeSettingsRepository>()

    LaunchedEffect(Unit) {
        delay(timeMillis = 100)
        focusManager.clearFocus()
    }

    val hazeState = rememberHazeState()
    val hazeStyle = drawerHazeStyle()
    val drawerItemVisualStyle = desktopDrawerItemVisualStyle()
    var isDockedDrawerVisible by remember { mutableStateOf(homeSettingsRepository.isDrawerVisible()) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val usesDockedDrawer = calculateWindowSizeClass().widthSizeClass != WindowWidthSizeClass.Compact
        val drawerState = key(usesDockedDrawer) {
            rememberDrawerState(initialValue = DrawerValue.Closed)
        }

        val drawerContent: @Composable () -> Unit = {
            Drawer(
                displayState = displayState,
                feedManagementActions = feedManagementActions,
                drawerItemVisualStyle = drawerItemVisualStyle,
                onFeedFilterSelected = { feedFilter: FeedFilter ->
                    feedManagementActions.onFeedFilterSelected(feedFilter)
                    scope.launch {
                        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                        if (drawerState.isOpen) {
                            drawerState.close()
                        }
                    }
                },
                onFeedSuggestionsClick = {
                    scope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        }
                    }
                    onFeedSuggestionsClick()
                },
            )
        }

        val listContent: @Composable () -> Unit = {
            Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(toolbarHeight),
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {}
                HomeScreenContent(
                    modifier = Modifier.fillMaxSize(),
                    displayState = displayState,
                    feedListActions = feedListActions,
                    feedManagementActions = feedManagementActions,
                    shareBehavior = shareBehavior,
                    listState = listState,
                    snackbarHostState = snackbarHostState,
                    onSearchClick = onSearchClick,
                    onSettingsButtonClicked = onSettingsButtonClicked,
                    toolbarElevation = 2.dp,
                    topToolbarContentFadeHeight = listPaneTopContentFadeHeight,
                    toolbarExpandedHeight = toolbarHeight,
                    showDrawerMenu = true,
                    isDrawerOpen = if (usesDockedDrawer) isDockedDrawerVisible else drawerState.isOpen,
                    onDrawerMenuClick = {
                        if (usesDockedDrawer) {
                            isDockedDrawerVisible = !isDockedDrawerVisible
                            homeSettingsRepository.setDrawerVisible(isDockedDrawerVisible)
                        } else {
                            scope.launch {
                                if (drawerState.isOpen) {
                                    drawerState.close()
                                } else {
                                    drawerState.open()
                                }
                            }
                        }
                    },
                    onRefresh = feedListActions.refreshData,
                    showDropdownMenu = false,
                    onEmptyStateClick = onEmptyStateClick,
                    feedContentWrapper = { content ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = listPaneMaxContentWidth)
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                ) {
                                    content()
                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(6.dp),
                                adapter = rememberScrollbarAdapter(scrollState = listState),
                            )
                        }
                    },
                )
            }
        }

        if (usesDockedDrawer) {
            DockedDrawerLayout(
                isDockedDrawerVisible = isDockedDrawerVisible,
                hazeState = hazeState,
                hazeStyle = hazeStyle,
                drawerContent = drawerContent,
                paneContent = listContent,
            )
        } else {
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = drawerContent,
                paneContent = listContent,
            )
        }
    }
}
