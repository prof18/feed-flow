package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.components.HomeScreenContent
import com.prof18.feedflow.shared.ui.home.components.drawer.Drawer
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch

@Composable
internal fun DesktopHomeScaffold(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    currentReaderArticle: FeedItemUrlInfo?,
    onReaderClosed: () -> Unit,
    onEmptyStateClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current
    val homeSettingsRepository = DI.koin.get<DesktopHomeSettingsRepository>()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isDockedDrawerVisible by remember { mutableStateOf(homeSettingsRepository.isDrawerVisible()) }

    val initialAnchorIndex = remember {
        val persistedAnchorIndex = homeSettingsRepository.getPaneExpansionIndex()
        if (persistedAnchorIndex in 1 until paneAnchors.lastIndex) {
            persistedAnchorIndex
        } else {
            defaultSplitAnchorIndex
        }
    }
    var lastAnchorIndex by rememberSaveable { mutableIntStateOf(initialAnchorIndex) }
    val paneExpansionState = rememberPaneExpansionState(
        anchors = paneAnchors,
        initialAnchoredIndex = initialAnchorIndex,
    )

    val scaffoldDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(currentWindowAdaptiveInfo())
    val navigator = rememberListDetailPaneScaffoldNavigator<String>(
        scaffoldDirective = scaffoldDirective,
    )

    LaunchedEffect(paneExpansionState.currentAnchor) {
        val currentAnchor = paneExpansionState.currentAnchor ?: return@LaunchedEffect
        val anchorIndex = paneAnchors.indexOf(currentAnchor)
        if (anchorIndex in 1 until paneAnchors.lastIndex) {
            lastAnchorIndex = anchorIndex
            homeSettingsRepository.setPaneExpansionIndex(anchorIndex)
        }
    }

    SyncReaderPaneNavigation(
        currentReaderArticle = currentReaderArticle,
        navigator = navigator,
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val isThreePaneLayout = maxWidth >= threePaneMinWidth
        val isListExpanded = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
        val isDetailFullscreen = paneExpansionState.currentAnchor == detailFullscreenAnchor
        val canToggleDetailFullscreen = navigator.scaffoldDirective.maxHorizontalPartitions > 1

        val drawerContent: @Composable () -> Unit = {
            Drawer(
                displayState = displayState,
                feedManagementActions = feedManagementActions,
                onFeedFilterSelected = { feedFilter: FeedFilter ->
                    feedManagementActions.onFeedFilterSelected(feedFilter)
                    scope.launch {
                        listState.scrollToItemConditionally(
                            0,
                            reduceMotionEnabled = reduceMotionEnabled,
                        )
                        if (drawerState.isOpen) {
                            drawerState.close()
                        }
                        navigator.navigateTo(ListDetailPaneScaffoldRole.List)
                    }
                },
                onFeedSuggestionsClick = {
                    scope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        }
                    }
                    onFeedSuggestionsClick()
                },
            )
        }

        val paneContent: @Composable () -> Unit = {
            ListDetailPaneScaffold(
                modifier = Modifier.fillMaxSize(),
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                paneExpansionState = paneExpansionState,
                paneExpansionDragHandle = { state ->
                    val interactionSource = remember { MutableInteractionSource() }
                    VerticalDragHandle(
                        modifier = Modifier.paneExpansionDraggable(
                            state = state,
                            minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                            interactionSource = interactionSource,
                        ),
                        interactionSource = interactionSource,
                    )
                },
                listPane = {
                    AnimatedPane {
                        HomeScreenContent(
                            modifier = Modifier.fillMaxSize(),
                            displayState = displayState,
                            feedListActions = feedListActions,
                            feedManagementActions = feedManagementActions,
                            shareBehavior = shareBehavior,
                            listState = listState,
                            snackbarHostState = snackbarHostState,
                            onSearchClick = onSearchClick,
                            onSettingsButtonClicked = onSettingsButtonClicked,
                            showDrawerMenu = true,
                            isDrawerOpen = if (isThreePaneLayout) {
                                isDockedDrawerVisible
                            } else {
                                drawerState.isOpen
                            },
                            onDrawerMenuClick = {
                                if (isThreePaneLayout) {
                                    isDockedDrawerVisible = !isDockedDrawerVisible
                                    homeSettingsRepository.setDrawerVisible(isDockedDrawerVisible)
                                } else {
                                    scope.launch {
                                        if (drawerState.isOpen) {
                                            drawerState.close()
                                        } else {
                                            drawerState.open()
                                        }
                                    }
                                }
                            },
                            onRefresh = feedListActions.refreshData,
                            showDropdownMenu = false,
                            onEmptyStateClick = onEmptyStateClick,
                            feedContentWrapper = { content ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                    ) {
                                        content()
                                    }

                                    VerticalScrollbar(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(Spacing.xsmall),
                                        adapter = rememberScrollbarAdapter(scrollState = listState),
                                    )
                                }
                            },
                        )
                    }
                },
                detailPane = {
                    AnimatedPane {
                        if (currentReaderArticle != null) {
                            ReaderModeScreen(
                                feedItemUrlInfo = currentReaderArticle,
                                navigateBack = {
                                    onReaderClosed()
                                    scope.launch {
                                        navigator.navigateBack(
                                            backNavigationBehavior = BackNavigationBehavior.PopLatest,
                                        )
                                    }
                                },
                                showBackButton = !isListExpanded,
                                isDetailFullscreen = isDetailFullscreen,
                                onToggleDetailFullscreen = if (canToggleDetailFullscreen) {
                                    {
                                        scope.launch {
                                            if (isDetailFullscreen) {
                                                val restoredAnchorIndex = lastAnchorIndex.coerceIn(
                                                    minimumValue = 1,
                                                    maximumValue = paneAnchors.lastIndex - 1,
                                                )
                                                paneExpansionState.animateTo(paneAnchors[restoredAnchorIndex])
                                            } else {
                                                paneExpansionState.animateTo(detailFullscreenAnchor)
                                            }
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                },
            )
        }

        if (isThreePaneLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (isDockedDrawerVisible) {
                    Box(
                        modifier = Modifier
                            .width(drawerPaneWidth)
                            .fillMaxHeight(),
                    ) {
                        drawerContent()
                    }

                    VerticalDivider()
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    paneContent()
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        drawerContent()
                    }
                },
            ) {
                paneContent()
            }
        }
    }
}

@Composable
private fun SyncReaderPaneNavigation(
    currentReaderArticle: FeedItemUrlInfo?,
    navigator: ThreePaneScaffoldNavigator<String>,
) {
    LaunchedEffect(currentReaderArticle?.id) {
        if (currentReaderArticle != null) {
            if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
                navigator.navigateBack(
                    backNavigationBehavior = BackNavigationBehavior.PopLatest,
                )
            }
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                contentKey = currentReaderArticle.id,
            )
        }
    }

    LaunchedEffect(currentReaderArticle) {
        if (currentReaderArticle == null &&
            navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
        ) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.List)
        }
    }
}

private val threePaneMinWidth = 1360.dp
private val drawerPaneWidth = 320.dp
private val detailFullscreenAnchor = PaneExpansionAnchor.Proportion(0f)
private val listFullscreenAnchor = PaneExpansionAnchor.Proportion(1f)
private val paneAnchors: List<PaneExpansionAnchor> = buildList {
    add(detailFullscreenAnchor)
    for (proportion in 30..70 step 5) {
        add(PaneExpansionAnchor.Proportion(proportion / 100f))
    }
    add(listFullscreenAnchor)
}
private val defaultSplitAnchorIndex = paneAnchors.lastIndex / 2
