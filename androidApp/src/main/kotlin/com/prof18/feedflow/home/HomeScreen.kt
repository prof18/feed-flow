@file:OptIn(ExperimentalMaterialApi::class)

package com.prof18.feedflow.home

import FeedFlowTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.BrowserManager
import com.prof18.feedflow.MR
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.home.components.EmptyFeedView
import com.prof18.feedflow.home.components.FeedList
import com.prof18.feedflow.home.components.HomeAppBar
import com.prof18.feedflow.home.components.NoFeedsSourceView
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.model.FeedItemClickedInfo
import com.prof18.feedflow.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.theme.Spacing
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterialApi::class)
@Suppress("LongMethod")
@Composable
internal fun HomeScreen(
    onSettingsButtonClicked: () -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val browserManager = koinInject<BrowserManager>()

    val loadingState by homeViewModel.loadingState.collectAsStateWithLifecycle()
    val feedState by homeViewModel.feedState.collectAsStateWithLifecycle()
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

    Scaffold(
        topBar = {
            HomeAppBar(
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
                browserManager.openUrl(feedInfo.url, context)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onFeedItemLongClick = { feedInfo ->
                browserManager.openUrl(feedInfo.url, context)
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
                onFeedItemClick = { feedInfo ->
                    onFeedItemClick(feedInfo)
                },
                onFeedItemLongClick = { feedInfo ->
                    onFeedItemLongClick(feedInfo)
                },
            )

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
