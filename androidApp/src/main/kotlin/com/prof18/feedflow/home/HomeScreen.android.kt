@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3WindowSizeClassApi::class)

package com.prof18.feedflow.home

import FeedFlowTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.BrowserManager
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemClickedInfo
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.home.components.FeedItemImage
import com.prof18.feedflow.home.components.HomeAppBar
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.ui.home.components.Drawer
import com.prof18.feedflow.ui.home.components.EmptyFeedView
import com.prof18.feedflow.ui.home.components.FeedItemView
import com.prof18.feedflow.ui.home.components.FeedList
import com.prof18.feedflow.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Suppress("LongMethod")
@Composable
internal fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    onSettingsButtonClicked: () -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()

    val loadingState by homeViewModel.loadingState.collectAsStateWithLifecycle()
    val feedState by homeViewModel.feedState.collectAsStateWithLifecycle()
    val navDrawerState by homeViewModel.navDrawerState.collectAsStateWithLifecycle()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsStateWithLifecycle()
    val unReadCount = feedState.count { !it.isRead }

    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = loadingState.isLoading(),
        onRefresh = { homeViewModel.getNewFeeds() },
    )
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        homeViewModel.errorState.collect { errorState ->
            snackbarHostState.showSnackbar(
                errorState!!.message.toString(context),
                duration = SnackbarDuration.Short,
            )
        }
    }

    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var isDrawerMenuFullVisible by remember {
        mutableStateOf(true)
    }

    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            if (feedState.isEmpty()) {
                HomeScaffold(
                    unReadCount = unReadCount,
                    onSettingsButtonClicked = onSettingsButtonClicked,
                    homeViewModel = homeViewModel,
                    scope = scope,
                    listState = listState,
                    snackbarHostState = snackbarHostState,
                    loadingState = loadingState,
                    feedState = feedState,
                    pullRefreshState = pullRefreshState,
                    showDrawerMenu = false,
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
            } else {
                ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            Drawer(
                                navDrawerState = navDrawerState,
                                currentFeedFilter = currentFeedFilter,
                                feedSourceImage = { imageUrl ->
                                    FeedSourceImage(imageUrl)
                                },
                                onFeedFilterSelected = { feedFilter ->
                                    homeViewModel.onFeedFilterSelected(feedFilter)
                                    scope.launch {
                                        drawerState.close()
                                        listState.animateScrollToItem(0)
                                    }
                                },
                            )
                        }
                    },
                    drawerState = drawerState,
                ) {
                    HomeScaffold(
                        unReadCount = unReadCount,
                        onSettingsButtonClicked = onSettingsButtonClicked,
                        homeViewModel = homeViewModel,
                        scope = scope,
                        listState = listState,
                        snackbarHostState = snackbarHostState,
                        loadingState = loadingState,
                        feedState = feedState,
                        pullRefreshState = pullRefreshState,
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

        WindowWidthSizeClass.Medium -> {
            Row {
                AnimatedVisibility(
                    modifier = Modifier
                        .weight(1f),
                    visible = isDrawerMenuFullVisible && feedState.isNotEmpty(),
                ) {
                    Scaffold { paddingValues ->
                        Drawer(
                            modifier = Modifier
                                .padding(paddingValues),
                            navDrawerState = navDrawerState,
                            currentFeedFilter = currentFeedFilter,
                            feedSourceImage = { imageUrl ->
                                FeedSourceImage(imageUrl)
                            },
                            onFeedFilterSelected = { feedFilter ->
                                homeViewModel.onFeedFilterSelected(feedFilter)
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            },
                        )
                    }
                }

                HomeScaffold(
                    modifier = Modifier
                        .weight(2f),
                    unReadCount = unReadCount,
                    onSettingsButtonClicked = onSettingsButtonClicked,
                    homeViewModel = homeViewModel,
                    scope = scope,
                    listState = listState,
                    snackbarHostState = snackbarHostState,
                    loadingState = loadingState,
                    feedState = feedState,
                    pullRefreshState = pullRefreshState,
                    showDrawerMenu = feedState.isNotEmpty(),
                    isDrawerMenuOpen = isDrawerMenuFullVisible,
                    onDrawerMenuClick = {
                        isDrawerMenuFullVisible = !isDrawerMenuFullVisible
                    },
                )
            }
        }

        WindowWidthSizeClass.Expanded -> {
            Row {
                if (feedState.isNotEmpty()) {
                    Scaffold(
                        modifier = Modifier
                            .weight(1f),
                    ) { paddingValues ->
                        Drawer(
                            modifier = Modifier
                                .padding(paddingValues),
                            navDrawerState = navDrawerState,
                            currentFeedFilter = currentFeedFilter,
                            feedSourceImage = { imageUrl ->
                                FeedSourceImage(imageUrl)
                            },
                            onFeedFilterSelected = { feedFilter ->
                                homeViewModel.onFeedFilterSelected(feedFilter)
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            },
                        )
                    }
                }

                HomeScaffold(
                    modifier = Modifier
                        .weight(2f),
                    unReadCount = unReadCount,
                    onSettingsButtonClicked = onSettingsButtonClicked,
                    homeViewModel = homeViewModel,
                    scope = scope,
                    listState = listState,
                    snackbarHostState = snackbarHostState,
                    loadingState = loadingState,
                    feedState = feedState,
                    pullRefreshState = pullRefreshState,
                )
            }
        }
    }
}

@Composable
private fun FeedSourceImage(imageUrl: String) {
    FeedSourceLogoImage(
        size = 24.dp,
        imageUrl = imageUrl,
    )
}

@Suppress("LongMethod")
@Composable
private fun HomeScaffold(
    unReadCount: Int,
    homeViewModel: HomeViewModel,
    scope: CoroutineScope,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    pullRefreshState: PullRefreshState,
    modifier: Modifier = Modifier,
    showDrawerMenu: Boolean = false,
    isDrawerMenuOpen: Boolean = false,
    onSettingsButtonClicked: () -> Unit,
    onDrawerMenuClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val browserManager = koinInject<BrowserManager>()

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeAppBar(
                showDrawerMenu = showDrawerMenu,
                isDrawerOpen = isDrawerMenuOpen,
                onDrawerMenuClick = onDrawerMenuClick,
                unReadCount = unReadCount,
                onSettingsButtonClicked = onSettingsButtonClicked,
                onMarkAllReadClicked = {
                    homeViewModel.markAllRead()
                },
                onClearOldArticlesClicked = {
                    homeViewModel.deleteOldFeedItems()
                },
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onDoubleClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        homeViewModel.getNewFeeds()
                    }
                },
                onForceRefreshClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        homeViewModel.forceFeedRefresh()
                    }
                },
                onDeleteDatabase = {
                    homeViewModel.deleteAllFeeds()
                },
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HomeScreenContent(
            paddingValues = padding,
            loadingState = loadingState,
            feedState = feedState,
            pullRefreshState = pullRefreshState,
            listState = listState,
            onRefresh = {
                homeViewModel.getNewFeeds()
            },
            updateReadStatus = { lastVisibleIndex ->
                homeViewModel.updateReadStatus(lastVisibleIndex)
            },
            onFeedItemClick = { feedInfo ->
                browserManager.openUrlWithFavoriteBrowser(feedInfo.url, context)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onFeedItemLongClick = { feedInfo ->
                browserManager.openUrlWithFavoriteBrowser(feedInfo.url, context)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onAddFeedClick = {
                onSettingsButtonClicked()
            },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    pullRefreshState: PullRefreshState,
    listState: LazyListState,
    onRefresh: () -> Unit = {},
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
    onAddFeedClick: () -> Unit,
) {
    when {
        loadingState is NoFeedSourcesStatus -> {
            NoFeedsSourceView(
                modifier = Modifier
                    .padding(paddingValues),
                onAddFeedClick = {
                    onAddFeedClick()
                },
            )
        }

        !loadingState.isLoading() && feedState.isEmpty() -> {
            EmptyFeedView(
                modifier = Modifier
                    .padding(paddingValues),
                onReloadClick = {
                    onRefresh()
                },
            )
        }

        else -> FeedWithContentView(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            loadingState = loadingState,
            pullRefreshState = pullRefreshState,
            feedState = feedState,
            listState = listState,
            updateReadStatus = updateReadStatus,
            onFeedItemClick = onFeedItemClick,
            onFeedItemLongClick = onFeedItemLongClick,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun FeedWithContentView(
    modifier: Modifier = Modifier,
    loadingState: FeedUpdateStatus,
    pullRefreshState: PullRefreshState,
    feedState: List<FeedItem>,
    listState: LazyListState,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
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

        Box(
            Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState),
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
                                .padding(start = Spacing.regular),
                            url = url,
                            width = 96.dp,
                        )
                    },
                )
            }

            PullRefreshIndicator(
                loadingState.isLoading(),
                pullRefreshState,
                Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@FeedFlowPreview
@Composable
fun HomeScreeContentLoadingPreview() {
    FeedFlowTheme {
        HomeScreenContent(
            paddingValues = PaddingValues(0.dp),
            loadingState = InProgressFeedUpdateStatus(
                refreshedFeedCount = 10,
                totalFeedCount = 42,
            ),
            feedState = feedItemsForPreview,
            pullRefreshState = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
            ),
            listState = rememberLazyListState(),
            updateReadStatus = {},
            onFeedItemClick = {},
            onFeedItemLongClick = {},
            onAddFeedClick = {},
            onRefresh = {},
        )
    }
}

@FeedFlowPreview
@Composable
fun HomeScreeContentLoadedPreview() {
    FeedFlowTheme {
        HomeScreenContent(
            paddingValues = PaddingValues(0.dp),
            loadingState = FinishedFeedUpdateStatus,
            feedState = feedItemsForPreview,
            pullRefreshState = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
            ),
            listState = rememberLazyListState(),
            updateReadStatus = {},
            onFeedItemClick = {},
            onFeedItemLongClick = {},
            onAddFeedClick = {},
        )
    }
}
