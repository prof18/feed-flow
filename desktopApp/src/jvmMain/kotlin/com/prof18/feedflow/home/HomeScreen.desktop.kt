package com.prof18.feedflow.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemClickedInfo
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.openInBrowser
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.ui.home.components.Drawer
import com.prof18.feedflow.ui.home.components.EmptyFeedView
import com.prof18.feedflow.ui.home.components.FeedItemView
import com.prof18.feedflow.ui.home.components.FeedList
import com.prof18.feedflow.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
internal fun HomeScreen(
    paddingValues: PaddingValues,
    homeViewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    onAddFeedClick: () -> Unit,
) {
    val loadingState by homeViewModel.loadingState.collectAsState()
    val feedState by homeViewModel.feedState.collectAsState()
    val drawerItems by homeViewModel.drawerItems.collectAsState()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.errorState.collect { errorState ->
            snackbarHostState.showSnackbar(
                errorState!!.message.localized(),
                duration = SnackbarDuration.Short,
            )
        }
    }

    val windowSize = calculateWindowSizeClass()

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            CompactView(
                drawerItems = drawerItems,
                currentFeedFilter = currentFeedFilter,
                homeViewModel = homeViewModel,
                paddingValues = paddingValues,
                loadingState = loadingState,
                feedState = feedState,
                listState = listState,
                onAddFeedClick = onAddFeedClick,
            )
        }

        WindowWidthSizeClass.Medium -> {
            MediumView(
                drawerItems = drawerItems,
                currentFeedFilter = currentFeedFilter,
                homeViewModel = homeViewModel,
                paddingValues = paddingValues,
                loadingState = loadingState,
                feedState = feedState,
                listState = listState,
                onAddFeedClick = onAddFeedClick,
            )
        }

        WindowWidthSizeClass.Expanded -> {
            ExpandedView(
                drawerItems = drawerItems,
                currentFeedFilter = currentFeedFilter,
                homeViewModel = homeViewModel,
                paddingValues = paddingValues,
                loadingState = loadingState,
                feedState = feedState,
                listState = listState,
                onAddFeedClick = onAddFeedClick,
            )
        }
    }
}

@Composable
private fun CompactView(
    drawerItems: List<DrawerItem>,
    currentFeedFilter: FeedFilter,
    homeViewModel: HomeViewModel,
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    listState: LazyListState,
    onAddFeedClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Drawer(
                    drawerItems = drawerItems,
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = { feedFilter ->
                        homeViewModel.onFeedFilterSelected(feedFilter)
                        scope.launch {
                            drawerState.close()
                        }
                    },
                )
            }
        },
        drawerState = drawerState,
    ) {
        HomeScreenContent(
            paddingValues = paddingValues,
            loadingState = loadingState,
            feedState = feedState,
            listState = listState,
            onRefresh = {
                homeViewModel.getNewFeeds()
            },
            updateReadStatus = { lastVisibleIndex ->
                homeViewModel.updateReadStatus(lastVisibleIndex)
            },
            onFeedItemClick = { feedInfo ->
                openInBrowser(feedInfo.url)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onFeedItemLongClick = { feedInfo ->
                openInBrowser(feedInfo.url)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onAddFeedClick = {
                onAddFeedClick()
            },
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

@Composable
private fun MediumView(
    drawerItems: List<DrawerItem>,
    currentFeedFilter: FeedFilter,
    homeViewModel: HomeViewModel,
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    listState: LazyListState,
    onAddFeedClick: () -> Unit,
) {
    var isDrawerMenuFullVisible by remember {
        mutableStateOf(true)
    }

    Row {
        AnimatedVisibility(
            modifier = Modifier
                .weight(1f),
            visible = isDrawerMenuFullVisible,
        ) {
            Scaffold { paddingValues ->
                Drawer(
                    modifier = Modifier
                        .padding(paddingValues),
                    drawerItems = drawerItems,
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = { feedFilter ->
                        homeViewModel.onFeedFilterSelected(feedFilter)
                    },
                )
            }
        }

        HomeScreenContent(
            modifier = Modifier
                .weight(2f),
            paddingValues = paddingValues,
            loadingState = loadingState,
            feedState = feedState,
            listState = listState,
            onRefresh = {
                homeViewModel.getNewFeeds()
            },
            updateReadStatus = { lastVisibleIndex ->
                homeViewModel.updateReadStatus(lastVisibleIndex)
            },
            onFeedItemClick = { feedInfo ->
                openInBrowser(feedInfo.url)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onFeedItemLongClick = { feedInfo ->
                openInBrowser(feedInfo.url)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onAddFeedClick = {
                onAddFeedClick()
            },
            showDrawerMenu = true,
            isDrawerMenuOpen = isDrawerMenuFullVisible,
            onDrawerMenuClick = {
                isDrawerMenuFullVisible = !isDrawerMenuFullVisible
            },
        )
    }
}

@Composable
private fun ExpandedView(
    drawerItems: List<DrawerItem>,
    currentFeedFilter: FeedFilter,
    homeViewModel: HomeViewModel,
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    listState: LazyListState,
    onAddFeedClick: () -> Unit,
) {
    Row {
        AnimatedVisibility(
            modifier = Modifier
                .weight(1f),
            visible = true,
        ) {
            Scaffold { paddingValues ->
                Drawer(
                    modifier = Modifier
                        .padding(paddingValues),
                    drawerItems = drawerItems,
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = { feedFilter ->
                        homeViewModel.onFeedFilterSelected(feedFilter)
                    },
                )
            }
        }

        HomeScreenContent(
            modifier = Modifier
                .weight(2f),
            paddingValues = paddingValues,
            loadingState = loadingState,
            feedState = feedState,
            listState = listState,
            onRefresh = {
                homeViewModel.getNewFeeds()
            },
            updateReadStatus = { lastVisibleIndex ->
                homeViewModel.updateReadStatus(lastVisibleIndex)
            },
            onFeedItemClick = { feedInfo ->
                openInBrowser(feedInfo.url)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onFeedItemLongClick = { feedInfo ->
                openInBrowser(feedInfo.url)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onAddFeedClick = {
                onAddFeedClick()
            },
        )
    }
}

@Composable
private fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    showDrawerMenu: Boolean = false,
    isDrawerMenuOpen: Boolean = false,
    onDrawerMenuClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
    onAddFeedClick: () -> Unit,
) {
    when {
        loadingState is NoFeedSourcesStatus -> NoFeedsSourceView(
            modifier = modifier
                .padding(paddingValues),
            onAddFeedClick = {
                onAddFeedClick()
            },
        )

        !loadingState.isLoading() && feedState.isEmpty() -> EmptyFeedView(
            modifier = modifier
                .padding(paddingValues),
            onReloadClick = {
                onRefresh()
            },
        )

        else -> FeedWithContentView(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize(),
            paddingValues = paddingValues,
            feedState = feedState,
            loadingState = loadingState,
            listState = listState,
            updateReadStatus = updateReadStatus,
            showDrawerMenu = showDrawerMenu,
            isDrawerMenuOpen = isDrawerMenuOpen,
            onDrawerMenuClick = onDrawerMenuClick,
            onFeedItemClick = onFeedItemClick,
            onFeedItemLongClick = onFeedItemLongClick,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun FeedWithContentView(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    feedState: List<FeedItem>,
    loadingState: FeedUpdateStatus,
    listState: LazyListState,
    showDrawerMenu: Boolean,
    isDrawerMenuOpen: Boolean,
    onDrawerMenuClick: () -> Unit,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        val unReadCount = feedState.count { !it.isRead }

        FeedContentToolbar(
            unReadCount = unReadCount,
            showDrawerMenu = showDrawerMenu,
            isDrawerOpen = isDrawerMenuOpen,
            onDrawerMenuClick = onDrawerMenuClick,
        )

        FeedLoader(loadingState = loadingState)

        Box(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(end = 4.dp),
        ) {
            FeedList(
                modifier = Modifier,
                feedItems = feedState,
                listState = listState,
                updateReadStatus = { index ->
                    updateReadStatus(index)
                },
            ) { feedItem ->
                FeedItemView(
                    feedItem = feedItem,
                    onFeedItemClick = onFeedItemClick,
                    onFeedItemLongClick = onFeedItemLongClick,
                    feedItemImage = { url ->
                        FeedItemImage(
                            modifier = Modifier
                                .wrapContentHeight(),
                            url = url,
                            width = 96.dp,
                        )
                    },
                )
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = listState,
                ),
            )
        }
    }
}

@Composable
private fun ColumnScope.FeedLoader(loadingState: FeedUpdateStatus) {
    AnimatedVisibility(loadingState.isLoading()) {
        val feedRefreshCounter = """
                    ${loadingState.refreshedFeedCount}/${loadingState.totalFeedCount}
        """.trimIndent()
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular),
            text = stringResource(
                resource = MR.strings.loading_feed_message,
                feedRefreshCounter,
            ),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContentToolbar(
    unReadCount: Int,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    onDrawerMenuClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = if (showDrawerMenu) {
            {
                IconButton(
                    onClick = {
                        onDrawerMenuClick()
                    },
                ) {
                    Icon(
                        imageVector = if (isDrawerOpen) {
                            Icons.Default.MenuOpen
                        } else {
                            Icons.Default.Menu
                        },
                        contentDescription = null,
                    )
                }
            }
        } else {
            { }
        },
        title = {
            Row {
                Text(
                    stringResource(resource = MR.strings.app_name),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("($unReadCount)")
            }
        },
    )
}
