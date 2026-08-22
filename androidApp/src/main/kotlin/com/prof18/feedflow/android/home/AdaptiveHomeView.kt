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
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.home.drawer.AndroidDrawer
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.SwipeActionType.NONE
import com.prof18.feedflow.shared.presentation.model.HomeViewMenuState
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawerState,
            // Material3 puts its drag handle on the whole content, so any horizontal drag over the
            // feed list pulled the drawer open mid-scroll. Those drags are handled by
            // claimHorizontalDragsForDrawer below; this keeps drag-to-close once it is open.
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
                modifier = Modifier.claimHorizontalDragsForDrawer(
                    isRtl = isRtl,
                    // SwipeableActionsBox mirrors startActions in RTL, so rightSwipeAction is the
                    // drawer-opening row action in both layout directions.
                    drawerOpeningSwipeHasPriority = displayState.swipeActions.rightSwipeAction == NONE,
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

/**
 * Claims horizontal drags over the home content on behalf of the drawer.
 *
 * Consuming them is the part that carries weight, including the drags that open nothing: a feed
 * item cancels its click only once something consumes the gesture, so leaving them unconsumed
 * turns every horizontal drag into an article tap when swipe actions are off. Material3 used to
 * consume them as a side effect of its own drag handle, which `gesturesEnabled` now switches off
 * while the drawer is closed. Dropping this modifier therefore reintroduces that tap.
 *
 * An unclaimed horizontal drag toward the drawer opens it. Because this modifier runs inside
 * [ModalNavigationDrawer], descendants handle the gesture first: the feed list keeps vertical
 * drags and a configured feed-item swipe action keeps horizontal drags over its row.
 *
 * SwipeableActionsBox consumes both directions whenever either row action exists. When the action
 * in the drawer-opening direction is disabled, [drawerOpeningSwipeHasPriority] claims only that
 * direction during the initial pointer pass so the row cannot consume an action it does not have.
 * If that action is configured, the initial detector stays inactive and the row keeps priority.
 *
 * This deliberately does not require a drag from the screen edge. Gesture navigation reserves that
 * edge for Back, while a content swipe remains available in every navigation mode.
 */
private fun Modifier.claimHorizontalDragsForDrawer(
    isRtl: Boolean,
    drawerOpeningSwipeHasPriority: Boolean,
    onOpen: () -> Unit,
): Modifier = this
    .claimDisabledDrawerDirection(
        isRtl = isRtl,
        enabled = drawerOpeningSwipeHasPriority,
        onOpen = onOpen,
    )
    .claimUnhandledHorizontalDrags(isRtl = isRtl, onOpen = onOpen)

private fun Modifier.claimDisabledDrawerDirection(
    isRtl: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
): Modifier = pointerInput(isRtl, enabled) {
    if (!enabled) return@pointerInput

    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial)
                .changes
                .firstOrNull { it.id == down.id }
                ?: return@awaitEachGesture
            if (!change.pressed) {
                return@awaitEachGesture
            }

            val dragOffset = change.position - down.position
            val horizontalDistance = abs(dragOffset.x)
            val verticalDistance = abs(dragOffset.y)
            val crossedTouchSlop = horizontalDistance > viewConfiguration.touchSlop ||
                verticalDistance > viewConfiguration.touchSlop
            if (!crossedTouchSlop) {
                continue
            }

            val isHorizontalSwipe = horizontalDistance > verticalDistance * 2f
            val opensDrawer = if (isRtl) dragOffset.x < 0f else dragOffset.x > 0f
            if (isHorizontalSwipe && opensDrawer) {
                change.consume()
                onOpen()
                consumeUntilRelease(down.id)
            }
            return@awaitEachGesture
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilRelease(pointerId: PointerId) {
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial)
            .changes
            .firstOrNull { it.id == pointerId }
            ?: return
        change.consume()
        if (!change.pressed) {
            return
        }
    }
}

private fun Modifier.claimUnhandledHorizontalDrags(
    isRtl: Boolean,
    onOpen: () -> Unit,
): Modifier = pointerInput(isRtl) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        var overSlop = 0f
        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, slop ->
            change.consume()
            overSlop = slop
        } ?: return@awaitEachGesture

        val opensDrawer = if (isRtl) overSlop < 0f else overSlop > 0f
        if (opensDrawer) {
            onOpen()
        }

        horizontalDrag(drag.id) { change -> change.consume() }
    }
}
