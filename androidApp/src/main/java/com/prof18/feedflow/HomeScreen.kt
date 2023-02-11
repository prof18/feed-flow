@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)

package com.prof18.feedflow

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun HomeScreen(
    onSettingsButtonClicked: () -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()

    val loadingState by homeViewModel.loadingState.collectAsStateWithLifecycle()
    val feedState by homeViewModel.feedState.collectAsStateWithLifecycle()
    val errorState by homeViewModel.errorState.collectAsStateWithLifecycle()
    val unReadCount = feedState.count { !it.isRead }

    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = loadingState.isLoading(),
        onRefresh = { homeViewModel.getNewFeeds() },
    )

    if (errorState != null) {
        LaunchedEffect(errorState) {
            launch {
                snackbarHostState.showSnackbar(
                    errorState!!.message,
                    duration = SnackbarDuration.Long,
                )
                homeViewModel.clearErrorState()
            }
        }
    }

    // TODO: Check localisation
    Scaffold(
        topBar = {
            FFTopAppBar(
                title = {
                    Row {
                        Text("FeedFlow")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(${unReadCount})")
                    }
                },
                actionIcon = Icons.Default.Settings,
                modifier = Modifier,
                onActionClick = { onSettingsButtonClicked() }
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
            onRefresh = {
                homeViewModel.getNewFeeds()
            },
            updateReadStatus = { lastVisibleIndex ->
                homeViewModel.updateReadStatus(lastVisibleIndex)
            },
        )
    }
}

@Composable
private fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    pullRefreshState: PullRefreshState,
    onRefresh: () -> Unit = {},
    updateReadStatus: (Int) -> Unit,
) {
    if (!loadingState.isLoading() && feedState.isEmpty()) {
        NoFeedsView(
            modifier = Modifier.padding(paddingValues),
            onReloadClick = { onRefresh() })
    } else {
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            AnimatedVisibility(loadingState.isLoading()) {
                val feedRefreshCounter =
                    "${loadingState.refreshedFeedCount}/${loadingState.totalFeedCount}"
                Text(
                    text = "Loading feeds $feedRefreshCounter"
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                FeedList(
                    modifier = Modifier,
                    feedItems = feedState,
                    updateReadStatus = { index ->
                        updateReadStatus(index)
                    }
                )

                PullRefreshIndicator(
                    loadingState.isLoading(),
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun NoFeedsView(
    modifier: Modifier,
    onReloadClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No feeds")
        Button(
            onClick = {
                onReloadClick()
            }
        ) {
            Text("Reload")
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun FeedList(
    modifier: Modifier,
    feedItems: List<FeedItem>,
    updateReadStatus: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        items(feedItems) { item ->
            Column {
                Text(item.title)
                if (item.isRead) {
                    Text("READ")
                } else {
                    Text("UNREAD")
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(2000)
            .collect { index ->
                if (index != 0) {
                    updateReadStatus(index)
                    Logger.d { "First visible item: $index" }
                }
            }
    }
}
