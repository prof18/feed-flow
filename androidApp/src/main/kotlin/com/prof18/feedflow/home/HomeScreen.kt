@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)

package com.prof18.feedflow.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.BrowserSelector
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.presentation.model.FeedItemClickedInfo
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.home.components.EmptyFeedView
import com.prof18.feedflow.home.components.FeedList
import com.prof18.feedflow.home.components.NoFeedsSourceView
import com.prof18.feedflow.presentation.HomeViewModel
import com.prof18.feedflow.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun HomeScreen(
    onSettingsButtonClicked: () -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val browserSelector = koinInject<BrowserSelector>()

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
                errorState!!.message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    val scope = rememberCoroutineScope()

    // TODO: Check localisation
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
                }
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
                openUrl(feedInfo.url, context, browserSelector)
                homeViewModel.markAsRead(feedInfo.id)
            },
            onFeedItemLongClick = { feedInfo ->
                openUrl(feedInfo.url, context, browserSelector)
                homeViewModel.markAsRead(feedInfo.id)
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
                        onFeedItemClick = { feedInfo ->
                            onFeedItemClick(feedInfo)
                        },
                        onFeedItemLongClick = { feedInfo ->
                            onFeedItemLongClick(feedInfo)
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
}

@Composable
private fun HomeAppBar(
    unReadCount: Int,
    onMarkAllReadClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row {
                Text("FeedFlow")
                Spacer(modifier = Modifier.width(4.dp))
                Text("(${unReadCount})")
            }
        },
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        onMarkAllReadClicked()
                        showMenu = false
                    },
                    text = {
                        Text("Mark all read")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                        )
                    },
                )

                DropdownMenuItem(
                    onClick = {
                        onClearOldArticlesClicked()
                        showMenu = false
                    },
                    text = {
                        Text("Clear old articles")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                        )
                    },
                )

                DropdownMenuItem(
                    onClick = {
                        onSettingsButtonClicked()
                    },
                    text = {
                        Text("Settings")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                        )
                    },
                )
            }

        },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { onDoubleClick() },
                onTap = { onClick() }
            )
        }
    )
}

private fun openUrl(
    url: String,
    context: Context,
    browserSelector: BrowserSelector
) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
        browserSelector.getBrowserPackageName()?.let { packageName ->
            setPackage(packageName)
        }
    }
    context.startActivity(intent)
}
