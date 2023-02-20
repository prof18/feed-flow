@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)

package com.prof18.feedflow.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.BrowserSelector
import com.prof18.feedflow.FFTopAppBar
import com.prof18.feedflow.FeedItem
import com.prof18.feedflow.FeedUpdateStatus
import com.prof18.feedflow.home.components.FeedList
import com.prof18.feedflow.home.components.NoFeedsView
import com.prof18.feedflow.ui.theme.Spacing
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
    val unReadCount = feedState.count { !it.isRead }

    val coroutineScope = rememberCoroutineScope()
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
                errorState!!.message,
                duration = SnackbarDuration.Short,
            )
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
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
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
            listState = listState,
            onRefresh = {
                homeViewModel.getNewFeeds()
            },
            updateReadStatus = { lastVisibleIndex ->
                homeViewModel.updateReadStatus(lastVisibleIndex)
            },
            onFeedItemClick = { url ->
                openUrl(url, context)
            },
            onFeedItemLongClick = { url ->
                openUrl(url, context)
            },
            onAddFeedClick = {
                onSettingsButtonClicked()
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
    listState: LazyListState,
    onRefresh: () -> Unit = {},
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (String) -> Unit,
    onFeedItemLongClick: (String) -> Unit,
    onAddFeedClick: () -> Unit,
) {
    if (!loadingState.isLoading() && feedState.isEmpty()) {
        NoFeedsView(
            modifier = Modifier.padding(paddingValues),
            onReloadClick = { onRefresh() },
            onAddFeedClick = { onAddFeedClick() }
            )
    } else {
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
                Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                FeedList(
                    modifier = Modifier,
                    feedItems = feedState,
                    listState = listState,
                    updateReadStatus = { index ->
                        updateReadStatus(index)
                    },
                    onFeedItemClick = { url ->
                        onFeedItemClick(url)
                    },
                    onFeedItemLongClick = { url ->
                        onFeedItemLongClick(url)
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

private fun openUrl(url: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
        setPackage(BrowserSelector.getBrowserPackageName(context, this))
    }
    context.startActivity(intent)
}
