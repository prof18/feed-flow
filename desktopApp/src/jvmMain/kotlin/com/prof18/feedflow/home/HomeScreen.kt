package com.prof18.feedflow.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.koin
import com.prof18.feedflow.openInBrowser
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.presentation.model.FeedItemClickedInfo
import com.prof18.feedflow.ui.components.AsyncImage
import com.prof18.feedflow.ui.components.loadImageBitmap
import com.prof18.feedflow.ui.style.Spacing
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    paddingValues: PaddingValues,
    homeViewModel: HomeViewModel,
    listState: LazyListState,
    onAddFeedClick: () -> Unit,
) {



    val loadingState by homeViewModel.loadingState.collectAsState()
    val feedState by homeViewModel.feedState.collectAsState()


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
        loadingState is NoFeedSourcesStatus -> {
            NoFeedsSourceView(
                modifier = Modifier.padding(paddingValues),
                onAddFeedClick = { onAddFeedClick() },
            )
        }
        !loadingState.isLoading() && feedState.isEmpty() -> {
            EmptyFeedView(
                modifier = Modifier.padding(paddingValues),
                onReloadClick = { onRefresh() }
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {

                AnimatedVisibility(loadingState.isLoading()) {
                    val feedRefreshCounter = """
                    ${loadingState.refreshedFeedCount}/${loadingState.totalFeedCount}
                """.trimIndent()
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.regular),
                        text = "Loading feeds $feedRefreshCounter",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .padding(end = 4.dp)
                ) {

                    FeedList(
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
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
                        }
                    )


//                    LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp), state) {
//                        items(feeds.value) { feedItem ->
//                            FeedList(
//                                feedItem = feedItem,
//                                onFeedItemClick = { clickedInfo ->
//                                    openInBrowser(clickedInfo.url)
//                                },
//                                onFeedItemLongClick = { clickedInfo ->
//                                    openInBrowser(clickedInfo.url)
//                                },
//                            )
//
//                        }
//                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(
                            scrollState = listState
                        )
                    )
                }

//                Box(
//                    Modifier
//                        .fillMaxSize()
//                        .pullRefresh(pullRefreshState)
//                ) {
//                    FeedList(
//                        modifier = Modifier,
//                        feedItems = feedState,
//                        listState = listState,
//                        updateReadStatus = { index ->
//                            updateReadStatus(index)
//                        },
//                        onFeedItemClick = { feedInfo ->
//                            onFeedItemClick(feedInfo)
//                        },
//                        onFeedItemLongClick = { feedInfo ->
//                            onFeedItemLongClick(feedInfo)
//                        }
//                    )
//
//                    PullRefreshIndicator(
//                        loadingState.isLoading(),
//                        pullRefreshState,
//                        Modifier.align(Alignment.TopCenter)
//                    )
//                }
            }
        }
    }
}


