package com.prof18.feedflow.android.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.home.drawer.AndroidDrawer
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.presentation.model.HomeViewMenuState
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
    viewMenuState: HomeViewMenuState,
    onFeedOrderChange: (FeedOrder) -> Unit,
    onShowReadArticlesTimelineChange: (Boolean) -> Unit,
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
            viewMenuState = viewMenuState,
            onFeedOrderChange = onFeedOrderChange,
            onShowReadArticlesTimelineChange = onShowReadArticlesTimelineChange,
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

    var isDrawerMenuFullVisible by rememberSaveable { mutableStateOf(true) }

    if (useDockedDrawer) {
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
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val edgeWidthPx = with(LocalDensity.current) { drawerEdgeWidth.toPx() }
        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawerState,
            // Material3 puts its drag handle on the whole content, so any horizontal drag over the
            // feed list pulled the drawer open mid-scroll. Opening is left to the edge swipe below;
            // this keeps the drag-to-close gesture once the drawer is already open.
            gesturesEnabled = drawerState.isOpen,
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
                modifier = Modifier.drawerEdgeSwipeToOpen(
                    edgeWidthPx = edgeWidthPx,
                    isRtl = isRtl,
                    onOpen = { scope.launch { drawerState.open() } },
                ),
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

private val drawerEdgeWidth = 20.dp

/**
 * Opens the drawer on a horizontal drag that starts within [edgeWidthPx] of the leading screen
 * edge. Drags that start anywhere else are swallowed rather than ignored: a feed item cancels its
 * click only once something consumes the gesture, so leaving them unconsumed would turn every
 * horizontal drag into an article tap when swipe actions are off.
 *
 * Runs on the main pass, so the feed list claims vertical drags and an enabled swipe action claims
 * its own row first.
 *
 * The edge is deliberately not claimed through `systemGestureExclusion`: under gesture navigation
 * that edge belongs to the system back gesture, and the platform honours only 200dp of exclusion
 * per side, so reserving it would leave back working on most of the screen and silently dead near
 * the bottom. Opening by drag is therefore a button-navigation affordance, and the toolbar menu
 * stays the gesture-independent way in.
 */
private fun Modifier.drawerEdgeSwipeToOpen(
    edgeWidthPx: Float,
    isRtl: Boolean,
    onOpen: () -> Unit,
): Modifier = this
    .pointerInput(edgeWidthPx, isRtl) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val startedFromEdge = if (isRtl) {
                down.position.x >= size.width - edgeWidthPx
            } else {
                down.position.x <= edgeWidthPx
            }

            var overSlop = 0f
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, slop ->
                change.consume()
                overSlop = slop
            } ?: return@awaitEachGesture

            val opensDrawer = if (isRtl) overSlop < 0f else overSlop > 0f
            if (startedFromEdge && opensDrawer) {
                onOpen()
            }

            horizontalDrag(drag.id) { change -> change.consume() }
        }
    }
