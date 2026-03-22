package com.prof18.feedflow.desktop.home

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.drawer.DesktopDrawer
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.desktop.ui.components.drawerHazeStyle
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Cursor

@Composable
internal fun DesktopHomeScaffold(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    currentReaderArticle: FeedItemUrlInfo?,
    onReaderClosed: () -> Unit,
    onEmptyStateClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current
    val focusManager = LocalFocusManager.current
    val homeSettingsRepository = DI.koin.get<DesktopHomeSettingsRepository>()

    @Suppress("MagicNumber")
    LaunchedEffect(Unit) {
        delay(100)
        focusManager.clearFocus()
    }
    val hazeState = rememberHazeState()
    val hazeStyle = drawerHazeStyle()
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

    val baseDirective = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(currentWindowAdaptiveInfo())
    val scaffoldDirective = baseDirective.copy(horizontalPartitionSpacerSize = 8.dp)
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
            DesktopDrawer(
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
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .paneExpansionDraggable(
                                state = state,
                                minTouchTargetSize = 0.dp,
                                interactionSource = interactionSource,
                            )
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = toolbarHeight)
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        )
                    }
                },
                listPane = {
                    val tweenSpec = tween<Float>(durationMillis = DESKTOP_PANE_TRANSITION_DURATION)
                    AnimatedPane(
                        enterTransition = fadeIn(animationSpec = tweenSpec),
                        exitTransition = fadeOut(animationSpec = tweenSpec),
                        boundsAnimationSpec = snap(),
                    ) {
                        DesktopHomeScreenContent(
                            modifier = Modifier.fillMaxSize(),
                            displayState = displayState,
                            feedListActions = feedListActions,
                            feedManagementActions = feedManagementActions,
                            shareBehavior = shareBehavior,
                            listState = listState,
                            snackbarHostState = snackbarHostState,
                            onSearchClick = onSearchClick,
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
                            onEmptyStateClick = onEmptyStateClick,
                        )
                    }
                },
                detailPane = {
                    val tweenSpec = tween<Float>(durationMillis = DESKTOP_PANE_TRANSITION_DURATION)
                    AnimatedPane(
                        enterTransition = fadeIn(animationSpec = tweenSpec),
                        exitTransition = fadeOut(animationSpec = tweenSpec),
                        boundsAnimationSpec = snap(),
                    ) {
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

        val elevatedPaneContent: @Composable () -> Unit = {
            Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(toolbarHeight),
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {}
                paneContent()
            }
        }

        if (isThreePaneLayout) {
            DockedDrawerLayout(
                isDockedDrawerVisible = isDockedDrawerVisible,
                hazeState = hazeState,
                hazeStyle = hazeStyle,
                drawerContent = drawerContent,
                paneContent = elevatedPaneContent,
            )
        } else {
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = drawerContent,
                paneContent = elevatedPaneContent,
            )
        }
    }
}

@Composable
internal fun DockedDrawerLayout(
    isDockedDrawerVisible: Boolean,
    hazeState: HazeState,
    hazeStyle: HazeStyle?,
    drawerContent: @Composable () -> Unit,
    paneContent: @Composable () -> Unit,
) {
    val macOsTopPadding = if (getDesktopOS().isMacOs()) Spacing.medium else 0.dp
    val colorScheme = MaterialTheme.colorScheme

    Row(modifier = Modifier.fillMaxSize()) {
        if (isDockedDrawerVisible) {
            Box(
                modifier = Modifier
                    .width(drawerPaneWidth)
                    .fillMaxHeight()
                    .zIndex(1f)
                    .let { base ->
                        if (hazeStyle != null) {
                            base.hazeEffect(state = hazeState, style = hazeStyle)
                        } else {
                            base.background(colorScheme.surface)
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = macOsTopPadding),
                ) {
                    drawerContent()
                }

                if (hazeStyle == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    )
                }
            }
        }
        val paneTopPadding = if (isDockedDrawerVisible) Spacing.small else macOsTopPadding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paneTopPadding)
                .hazeSource(state = hazeState),
        ) {
            paneContent()
        }
    }
}

@Composable
internal fun ModalDrawerLayout(
    drawerState: DrawerState,
    drawerContent: @Composable () -> Unit,
    paneContent: @Composable () -> Unit,
) {
    val macOsTopPadding = if (getDesktopOS().isMacOs()) Spacing.medium else 0.dp
    Box(modifier = Modifier.padding(top = macOsTopPadding)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                ) {
                    drawerContent()
                }
            },
        ) {
            paneContent()
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

internal val threePaneMinWidth = 1360.dp
internal val drawerPaneWidth = 320.dp
internal val toolbarHeight = 48.dp
internal val listPaneTopContentFadeHeight = 30.dp
internal val listPaneMaxContentWidth = 720.dp
private const val DESKTOP_PANE_TRANSITION_DURATION = 120

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
