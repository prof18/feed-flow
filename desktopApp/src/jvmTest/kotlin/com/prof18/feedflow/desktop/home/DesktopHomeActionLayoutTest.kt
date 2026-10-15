package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.FeedListActions
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.ShareBehavior
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopHomeActionLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `pending new articles action stays readable in compact populated workspace`() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val fontScale = mutableStateOf(1f)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        var callbackCount = 0

        rule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(currentDensity.density, fontScale.value),
                LocalLayoutDirection provides direction.value,
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    Box(Modifier.requiredSize(width = 360.dp, height = 440.dp).testTag(HOST_TAG)) {
                        DesktopHomeScreenContent(
                            displayState = populatedDisplayState(),
                            feedListActions = feedListActions(),
                            feedManagementActions = feedManagementActions(),
                            shareBehavior = shareBehavior(),
                            listState = remember { LazyListState() },
                            snackbarHostState = remember { DesktopFeedbackState() },
                            onSearchClick = {},
                            pendingNewArticlesCount = 7,
                            onShowNewArticlesClicked = { callbackCount++ },
                            onImportExportClick = {},
                        )
                    }
                }
            }
        }

        val label = EnFeedFlowStrings.newArticlesButton("7")
        for (scale in listOf(1f, 2f)) {
            for (layoutDirection in LayoutDirection.entries) {
                for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED)) {
                    rule.runOnIdle {
                        fontScale.value = scale
                        direction.value = layoutDirection
                        theme.value = mode
                    }
                    assertReadableInsideHost(label)
                }
            }
        }

        rule.onNodeWithText(label).performClick()
        rule.runOnIdle { assertEquals(1, callbackCount) }
    }

    @Test
    fun `Tab reaches pending new articles action and Enter activates it once`() {
        var callbackCount = 0
        val pendingCount = mutableStateOf(PENDING_ARTICLE_COUNT)
        val showDrawer = mutableStateOf(true)
        val refreshEnabled = mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = 520.dp, height = 440.dp)) {
                        DesktopHomeScreenContent(
                            displayState = populatedDisplayState(),
                            feedListActions = feedListActions().copy(refreshEnabled = refreshEnabled.value),
                            feedManagementActions = feedManagementActions(),
                            shareBehavior = shareBehavior(),
                            listState = remember { LazyListState() },
                            snackbarHostState = remember { DesktopFeedbackState() },
                            onSearchClick = {},
                            pendingNewArticlesCount = pendingCount.value,
                            onShowNewArticlesClicked = {
                                callbackCount++
                                pendingCount.value = 0
                            },
                            onImportExportClick = {},
                            showDrawerMenu = showDrawer.value,
                        )
                    }
                }
            }
        }

        val label = EnFeedFlowStrings.newArticlesButton("7")
        rule.onNodeWithContentDescription(EnFeedFlowStrings.drawerMenuButtonContentDescription).requestFocus()
        assertTrue(tabUntilFocused(label), "The pending-new-articles action should be reachable with Tab")
        rule.onNodeWithText(label).assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(1, callbackCount) }
        rule.onNodeWithText(label).assertDoesNotExist()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.drawerMenuButtonContentDescription)
            .assertIsDisplayed().assertIsFocused()

        rule.runOnIdle {
            showDrawer.value = false
            refreshEnabled.value = false
            pendingCount.value = PENDING_ARTICLE_COUNT
        }
        val search = rule.onNodeWithContentDescription(EnFeedFlowStrings.searchButtonContentDescription)
        search.requestFocus()
        assertTrue(tabUntilFocused(label), "New articles should remain keyboard-accessible without the drawer")
        rule.onNodeWithText(label).performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(2, callbackCount) }
        rule.onNodeWithText(label).assertDoesNotExist()
        search.assertIsDisplayed().assertIsFocused()
    }

    @Test
    fun `scroll to top returns populated article list to first item with reduce motion`() {
        val listState = LazyListState()
        val exposeScrollAction = mutableStateOf(false)
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val fontScale = mutableStateOf(1f)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalReduceMotion provides true,
                LocalDensity provides Density(currentDensity.density, fontScale.value),
                LocalLayoutDirection provides direction.value,
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    LaunchedEffect(exposeScrollAction.value) {
                        if (exposeScrollAction.value) listState.scrollToItem(SCROLLED_ITEM_INDEX)
                    }
                    Box(Modifier.requiredSize(width = 360.dp, height = 440.dp).testTag(HOST_TAG)) {
                        DesktopHomeScreenContent(
                            displayState = populatedDisplayState(),
                            feedListActions = feedListActions(),
                            feedManagementActions = feedManagementActions(),
                            shareBehavior = shareBehavior(),
                            listState = listState,
                            snackbarHostState = remember { DesktopFeedbackState() },
                            onSearchClick = {},
                            pendingNewArticlesCount = 0,
                            onShowNewArticlesClicked = {},
                            onImportExportClick = {},
                        )
                    }
                }
            }
        }

        rule.runOnIdle { exposeScrollAction.value = true }
        rule.waitForIdle()
        assertEquals(SCROLLED_ITEM_INDEX, listState.firstVisibleItemIndex)
        val scrollLabel = EnFeedFlowStrings.scrollToTopButtonContentDescription
        for (scale in listOf(1f, 2f)) {
            for (layoutDirection in LayoutDirection.entries) {
                for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED)) {
                    rule.runOnIdle {
                        fontScale.value = scale
                        direction.value = layoutDirection
                        theme.value = mode
                    }
                    assertReadableInsideHost(scrollLabel)
                }
            }
        }
        rule.onNodeWithText(scrollLabel).assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(0, listState.firstVisibleItemIndex)
    }

    @Test
    fun `Tab reaches scroll to top and Space activates it with useful focus remaining`() {
        val listState = LazyListState()
        val exposeScrollAction = mutableStateOf(false)
        setScrollableKeyboardHost(listState, exposeScrollAction, reduceMotion = true)

        rule.runOnIdle { exposeScrollAction.value = true }
        rule.waitForIdle()
        assertEquals(SCROLLED_ITEM_INDEX, listState.firstVisibleItemIndex)

        val scrollLabel = EnFeedFlowStrings.scrollToTopButtonContentDescription
        rule.onNodeWithContentDescription(EnFeedFlowStrings.drawerMenuButtonContentDescription).requestFocus()
        assertTrue(tabUntilFocused(scrollLabel), "The scroll-to-top action should be reachable with Tab")
        rule.onNodeWithText(scrollLabel).assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        rule.waitForIdle()

        assertEquals(0, listState.firstVisibleItemIndex)
        rule.onNodeWithText(scrollLabel).assertDoesNotExist()
        rule.onNodeWithText("Synthetic article 0").assertIsDisplayed().assertIsFocused()
    }

    @Test
    fun `grid scroll to top returns keyboard focus to the first article`() {
        val gridState = LazyStaggeredGridState()
        val exposeScrollAction = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalReduceMotion provides true,
            ) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    LaunchedEffect(exposeScrollAction.value) {
                        if (exposeScrollAction.value) gridState.scrollToItem(SCROLLED_ITEM_INDEX)
                    }
                    Box(Modifier.requiredSize(width = 740.dp, height = 440.dp)) {
                        DesktopHomeScreenContent(
                            displayState = populatedDisplayState().copy(
                                feedLayout = FeedLayout.CARD,
                                isGridLayoutEnabled = true,
                            ),
                            feedListActions = feedListActions(),
                            feedManagementActions = feedManagementActions(),
                            shareBehavior = shareBehavior(),
                            listState = remember { LazyListState() },
                            gridState = gridState,
                            snackbarHostState = remember { DesktopFeedbackState() },
                            onSearchClick = {},
                            pendingNewArticlesCount = 0,
                            onShowNewArticlesClicked = {},
                            onImportExportClick = {},
                        )
                    }
                }
            }
        }
        rule.runOnIdle { exposeScrollAction.value = true }
        rule.waitForIdle()
        assertEquals(SCROLLED_ITEM_INDEX, gridState.firstVisibleItemIndex)
        val label = EnFeedFlowStrings.scrollToTopButtonContentDescription
        rule.onNodeWithContentDescription(EnFeedFlowStrings.drawerMenuButtonContentDescription).requestFocus()
        assertTrue(tabUntilFocused(label), "Grid scroll to top should be keyboard-accessible")
        rule.onNodeWithText(label).assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals(0, gridState.firstVisibleItemIndex)
        rule.onNodeWithText(label).assertDoesNotExist()
        rule.onNodeWithText("Synthetic article 0").assertIsDisplayed().assertIsFocused()
    }

    @Test
    fun `animated scroll completion preserves a newly chosen toolbar focus`() {
        val listState = LazyListState()
        val exposeScrollAction = mutableStateOf(false)
        setScrollableKeyboardHost(listState, exposeScrollAction, reduceMotion = false)
        rule.runOnIdle { exposeScrollAction.value = true }
        rule.waitForIdle()
        val label = EnFeedFlowStrings.scrollToTopButtonContentDescription
        rule.onNodeWithText(label).requestFocus()
        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithText(label).performKeyInput { pressKey(Key.Spacebar) }
            rule.mainClock.advanceTimeByFrame()
            assertTrue(listState.firstVisibleItemIndex > 1, "The scroll must still be in progress")
            val search = rule.onNodeWithContentDescription(EnFeedFlowStrings.searchButtonContentDescription)
            search.requestFocus()
            search.assertIsFocused()
            rule.mainClock.advanceTimeBy(milliseconds = 2_000)
            rule.waitForIdle()
            assertEquals(0, listState.firstVisibleItemIndex)
            search.assertIsFocused()
            rule.onNodeWithText(label).assertDoesNotExist()
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    private fun setScrollableKeyboardHost(
        listState: LazyListState,
        exposeScrollAction: androidx.compose.runtime.State<Boolean>,
        reduceMotion: Boolean,
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalReduceMotion provides reduceMotion,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    LaunchedEffect(exposeScrollAction.value) {
                        if (exposeScrollAction.value) listState.scrollToItem(SCROLLED_ITEM_INDEX)
                    }
                    Box(Modifier.requiredSize(width = 520.dp, height = 440.dp).testTag(HOST_TAG)) {
                        DesktopHomeScreenContent(
                            displayState = populatedDisplayState(),
                            feedListActions = feedListActions(),
                            feedManagementActions = feedManagementActions(),
                            shareBehavior = shareBehavior(),
                            listState = listState,
                            snackbarHostState = remember { DesktopFeedbackState() },
                            onSearchClick = {},
                            pendingNewArticlesCount = 0,
                            onShowNewArticlesClicked = {},
                            onImportExportClick = {},
                        )
                    }
                }
            }
        }
    }

    private fun tabUntilFocused(label: String): Boolean {
        val target = isFocused() and hasText(label)
        repeat(MAX_TAB_STOPS) {
            if (rule.onAllNodes(target).fetchSemanticsNodes().isNotEmpty()) return true
            rule.onRoot().performKeyInput { pressKey(Key.Tab) }
            rule.waitForIdle()
        }
        return rule.onAllNodes(target).fetchSemanticsNodes().isNotEmpty()
    }

    private fun assertReadableInsideHost(label: String) {
        val host = rule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val node = rule.onNodeWithText(label)
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow, "$label must not clip")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertFalse(bounds.left < host.left || bounds.top < host.top, "$label: $bounds in $host")
        assertFalse(bounds.right > host.right || bounds.bottom > host.bottom, "$label: $bounds in $host")
    }

    private fun populatedDisplayState() = HomeDisplayState(
        feedItems = List(ARTICLE_COUNT) { index ->
            FeedItem(
                id = "article-$index",
                url = "https://example.com/$index",
                title = "Synthetic article $index",
                subtitle = null,
                content = "Synthetic article content for layout and scrolling.",
                imageUrl = null,
                feedSource = SOURCE,
                pubDateMillis = index.toLong(),
                isRead = false,
                dateString = null,
                commentsUrl = null,
                isBookmarked = false,
            )
        }.toImmutableList(),
        navDrawerState = NavDrawerState(),
        unReadCount = 12,
        isUnreadCountHidden = false,
        feedUpdateStatus = FinishedFeedUpdateStatus,
        feedFontSizes = FeedFontSizes(),
        currentFeedFilter = FeedFilter.Timeline,
        nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
        swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
        feedLayout = FeedLayout.LIST,
        isGridLayoutEnabled = false,
        feedItemDisplaySettings = FeedItemDisplaySettings(),
    )

    private fun feedListActions() = FeedListActions(
        onClearOldArticlesClicked = {},
        onDeleteDatabaseClick = {},
        refreshData = {},
        requestNewData = {},
        markAllRead = {},
        onBackToTimelineClick = {},
        onVisibleFeedItemsChanged = {},
        markAsRead = {},
        openUrl = {},
        openInBrowser = {},
        updateBookmarkStatus = { _, _ -> },
        updateReadStatus = { _, _ -> },
        markAllAboveAsRead = {},
        markAllBelowAsRead = {},
    )

    private fun feedManagementActions() = FeedManagementActions(
        onAddFeedClick = {},
        onFeedFilterSelected = {},
        onEditFeedClick = {},
        onDeleteFeedSourceClick = {},
        onPinFeedClick = {},
        onEditCategoryClick = { _, _ -> },
        validateCategoryName = { _, _ -> com.prof18.feedflow.core.model.CategoryNameValidationResult.VALID },
        onDeleteCategoryClick = {},
        onOpenWebsite = {},
    )

    private fun shareBehavior() = ShareBehavior(
        onShareClick = {},
        shareLinkTitle = EnFeedFlowStrings.menuCopyLink,
        shareCommentsTitle = EnFeedFlowStrings.menuCopyLinkComments,
    )

    private companion object {
        const val SCROLLED_ITEM_INDEX = 4
        const val ARTICLE_COUNT = 12
        const val PENDING_ARTICLE_COUNT = 7
        const val HOST_TAG = "home-action-host"
        const val MAX_TAB_STOPS = 48
        val SOURCE = FeedSource(
            id = "source",
            url = "https://example.com/feed.xml",
            title = "Example feed",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
    }
}
