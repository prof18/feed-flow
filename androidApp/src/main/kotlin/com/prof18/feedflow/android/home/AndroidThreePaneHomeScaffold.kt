package com.prof18.feedflow.android.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.readermode.ReaderModeScreen
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.ui.readermode.ReaderModeEmptyState
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AndroidThreePaneHomeScaffold(
    currentReaderArticle: FeedItemUrlInfo?,
    readerModeState: ReaderModeState,
    readerFontSize: Int,
    themeMode: ThemeMode,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    isMultiPaneEnabled: Boolean,
    initialPaneExpansionIndex: Int,
    onReaderClosed: () -> Unit,
    onUpdateReaderFontSize: (Int) -> Unit,
    onReaderBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onNavigateToPreviousArticle: () -> Unit,
    onNavigateToNextArticle: () -> Unit,
    onPaneExpansionIndexChanged: (Int) -> Unit,
    drawerPane: @Composable (Modifier, Boolean, () -> Unit) -> Unit,
    listPane: @Composable (Modifier, Boolean, () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current
    val transitions = rememberPaneTransitions(reduceMotionEnabled)
    val initialAnchorIndex = remember(initialPaneExpansionIndex) {
        if (initialPaneExpansionIndex in 1 until androidHomePaneAnchors.lastIndex) {
            initialPaneExpansionIndex
        } else {
            defaultAndroidHomePaneAnchorIndex
        }
    }
    val paneExpansionState = rememberPaneExpansionState(
        anchors = androidHomePaneAnchors,
        initialAnchoredIndex = initialAnchorIndex,
    )
    var lastAnchorIndex by rememberSaveable { mutableIntStateOf(initialAnchorIndex) }
    val scaffoldDirective = rememberScaffoldDirective(isMultiPaneEnabled)
    val navigator = rememberListDetailPaneScaffoldNavigator<String>(
        scaffoldDirective = scaffoldDirective,
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val latestOnPaneExpansionIndexChanged = rememberUpdatedState(onPaneExpansionIndexChanged)
    var lastReaderArticle by remember { mutableStateOf(currentReaderArticle) }

    LaunchedEffect(currentReaderArticle?.id) {
        if (currentReaderArticle != null) {
            lastReaderArticle = currentReaderArticle
        }
    }

    LaunchedEffect(navigator.currentDestination?.pane, currentReaderArticle) {
        if (navigator.currentDestination?.pane != ListDetailPaneScaffoldRole.Detail &&
            currentReaderArticle == null
        ) {
            lastReaderArticle = null
        }
    }

    LaunchedEffect(paneExpansionState.currentAnchor) {
        val currentAnchor = paneExpansionState.currentAnchor ?: return@LaunchedEffect
        val anchorIndex = androidHomePaneAnchors.indexOf(currentAnchor)
        if (anchorIndex in 1 until androidHomePaneAnchors.lastIndex) {
            lastAnchorIndex = anchorIndex
            latestOnPaneExpansionIndexChanged.value(anchorIndex)
        }
    }

    SyncAndroidReaderPaneNavigation(
        currentReaderArticle = currentReaderArticle,
        navigator = navigator,
    )
    val isReaderPaneActive = isReaderPaneActive(
        currentReaderArticle = currentReaderArticle,
        currentPane = navigator.currentDestination?.pane,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen || !isReaderPaneActive,
        drawerContent = {
            ModalDrawerSheet(
                windowInsets = WindowInsets(0.dp),
            ) {
                drawerPane(
                    Modifier.fillMaxSize(),
                    drawerState.isOpen,
                    {
                        scope.launch {
                            drawerState.close()
                        }
                    },
                )
            }
        },
    ) {
        val isDetailFullscreen = paneExpansionState.currentAnchor == detailFullscreenAnchor
        val canToggleDetailFullscreen = navigator.scaffoldDirective.maxHorizontalPartitions > 1
        val toggleDetailFullscreen = if (canToggleDetailFullscreen) {
            {
                scope.launch {
                    if (isDetailFullscreen) {
                        val restoredAnchorIndex = lastAnchorIndex.coerceIn(
                            minimumValue = 1,
                            maximumValue = androidHomePaneAnchors.lastIndex - 1,
                        )
                        paneExpansionState.animateTo(androidHomePaneAnchors[restoredAnchorIndex])
                    } else {
                        paneExpansionState.animateTo(detailFullscreenAnchor)
                    }
                }
                Unit
            }
        } else {
            null
        }

        BackHandler(enabled = currentReaderArticle != null) {
            if (isDetailFullscreen && toggleDetailFullscreen != null) {
                toggleDetailFullscreen()
            } else {
                onReaderClosed()
            }
        }

        ListDetailPaneScaffold(
            modifier = Modifier.fillMaxSize(),
            directive = navigator.scaffoldDirective,
            scaffoldState = navigator.scaffoldState,
            paneExpansionDragHandle = { state ->
                val interactionSource = remember { MutableInteractionSource() }
                VerticalDragHandle(
                    modifier = Modifier.paneExpansionDraggable(
                        state,
                        LocalMinimumInteractiveComponentSize.current,
                        interactionSource,
                    ),
                    interactionSource = interactionSource,
                )
            },
            paneExpansionState = paneExpansionState,
            listPane = {
                AnimatedPane(
                    enterTransition = transitions.listEnter,
                    exitTransition = transitions.listExit,
                ) {
                    listPane(
                        Modifier.fillMaxSize(),
                        drawerState.isOpen,
                        {
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
            },
            detailPane = {
                val articleToRender = currentReaderArticle ?: if (
                    navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
                ) {
                    lastReaderArticle
                } else {
                    null
                }
                AnimatedPane(
                    enterTransition = transitions.detailEnter,
                    exitTransition = transitions.detailExit,
                ) {
                    if (articleToRender != null) {
                        ReaderModeScreen(
                            readerModeState = readerModeState,
                            fontSize = readerFontSize,
                            themeMode = themeMode,
                            onUpdateFontSize = onUpdateReaderFontSize,
                            onBookmarkClick = onReaderBookmarkClick,
                            navigateBack = onReaderClosed,
                            canNavigatePrevious = canNavigatePrevious,
                            canNavigateNext = canNavigateNext,
                            onNavigateToPrevious = onNavigateToPreviousArticle,
                            onNavigateToNext = onNavigateToNextArticle,
                            isDetailFullscreen = isDetailFullscreen,
                            onToggleDetailFullscreen = toggleDetailFullscreen,
                        )
                    } else {
                        ReaderModeEmptyState()
                    }
                }
            },
        )
    }
}

@Composable
private fun rememberPaneTransitions(reduceMotionEnabled: Boolean): PaneTransitions = remember(
    reduceMotionEnabled,
) {
    if (reduceMotionEnabled) {
        PaneTransitions(
            listEnter = EnterTransition.None,
            listExit = ExitTransition.None,
            detailEnter = EnterTransition.None,
            detailExit = ExitTransition.None,
        )
    } else {
        PaneTransitions(
            listEnter = fadeIn() + slideInHorizontally(
                spring(visibilityThreshold = IntOffset.VisibilityThreshold),
            ) { -it },
            listExit = fadeOut() + slideOutHorizontally(
                spring(visibilityThreshold = IntOffset.VisibilityThreshold),
            ) { -it },
            detailEnter = fadeIn() + slideInHorizontally(
                spring(visibilityThreshold = IntOffset.VisibilityThreshold),
            ) { it },
            detailExit = fadeOut() + slideOutHorizontally(
                spring(visibilityThreshold = IntOffset.VisibilityThreshold),
            ) { it },
        )
    }
}

private fun isReaderPaneActive(
    currentReaderArticle: FeedItemUrlInfo?,
    currentPane: ThreePaneScaffoldRole?,
): Boolean = currentReaderArticle != null || currentPane == ListDetailPaneScaffoldRole.Detail

private data class PaneTransitions(
    val listEnter: EnterTransition,
    val listExit: ExitTransition,
    val detailEnter: EnterTransition,
    val detailExit: ExitTransition,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SyncAndroidReaderPaneNavigation(
    currentReaderArticle: FeedItemUrlInfo?,
    navigator: ThreePaneScaffoldNavigator<String>,
) {
    LaunchedEffect(currentReaderArticle?.id) {
        if (currentReaderArticle != null &&
            navigator.currentDestination?.pane != ListDetailPaneScaffoldRole.Detail
        ) {
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun rememberScaffoldDirective(isMultiPaneEnabled: Boolean) =
    calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).let { base ->
        if (isMultiPaneEnabled) base else base.copy(maxHorizontalPartitions = 1)
    }

private val detailFullscreenAnchor = PaneExpansionAnchor.Proportion(0f)
private val listFullscreenAnchor = PaneExpansionAnchor.Proportion(1f)
private val androidHomePaneAnchors: List<PaneExpansionAnchor> = buildList {
    add(detailFullscreenAnchor)
    for (proportion in 30..70 step 5) {
        add(PaneExpansionAnchor.Proportion(proportion / 100f))
    }
    add(listFullscreenAnchor)
}

private val defaultAndroidHomePaneAnchorIndex = androidHomePaneAnchors.lastIndex / 2
