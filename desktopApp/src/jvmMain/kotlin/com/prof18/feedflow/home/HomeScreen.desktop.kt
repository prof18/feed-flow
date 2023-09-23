package com.prof18.feedflow.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemClickedInfo
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.openInBrowser
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.ui.home.components.EmptyFeedView
import com.prof18.feedflow.ui.home.components.FeedList
import com.prof18.feedflow.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

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

    LaunchedEffect(Unit) {
        homeViewModel.errorState.collect { errorState ->
            snackbarHostState.showSnackbar(
                errorState!!.message.localized(),
                duration = SnackbarDuration.Short,
            )
        }
    }

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
    )
}

@Composable
private fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    listState: LazyListState,
    onRefresh: () -> Unit = {},
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
    onAddFeedClick: () -> Unit,
) {
    when {
        loadingState is NoFeedSourcesStatus -> NoFeedsSourceView(
            modifier = Modifier
                .padding(paddingValues),
            onAddFeedClick = {
                onAddFeedClick()
            },
        )

        !loadingState.isLoading() && feedState.isEmpty() -> EmptyFeedView(
            modifier = Modifier
                .padding(paddingValues),
            onReloadClick = {
                onRefresh()
            },
        )

        else -> FeedWithContentView(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            paddingValues = paddingValues,
            feedState = feedState,
            loadingState = loadingState,
            listState = listState,
            updateReadStatus = updateReadStatus,
            onFeedItemClick = onFeedItemClick,
            onFeedItemLongClick = onFeedItemLongClick,
        )
    }
}

@Composable
private fun FeedWithContentView(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    feedState: List<FeedItem>,
    loadingState: FeedUpdateStatus,
    listState: LazyListState,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        val unReadCount = feedState.count { !it.isRead }

        FeedContentToolbar(unReadCount)

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
                com.prof18.feedflow.ui.home.components.FeedItemView(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContentToolbar(unReadCount: Int) {
    TopAppBar(
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
