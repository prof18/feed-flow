package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedUpdateStatus
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.FeedListActions
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.ShareBehavior
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopNoFeedsStateTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `compact dark empty state has separate Add and Import actions`() {
        val calls = mutableListOf<String>()
        setNoFeedsContent(
            width = 360.dp,
            darkTheme = true,
            useOledTheme = false,
            onAddFeed = { calls += "add" },
            onImportExport = { calls += "import" },
            onOldEmptyStateAction = { calls += "old chooser" },
        )

        rule.onNodeWithText(EnFeedFlowStrings.addFeed).assertIsDisplayed().performClick()
        rule.onNodeWithText(EnFeedFlowStrings.importExportLabel).assertIsDisplayed().performClick()
        assertEquals(listOf("add", "import"), calls)
    }

    @Test
    fun `compact OLED empty state displays both actions`() {
        setNoFeedsContent(
            width = 360.dp,
            darkTheme = true,
            useOledTheme = true,
            onAddFeed = {},
            onImportExport = {},
            onOldEmptyStateAction = {},
        )
        rule.onNodeWithText(EnFeedFlowStrings.addFeed).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.importExportLabel).assertIsDisplayed()
    }

    @Test
    fun `actual empty library Light actions remain readable with enlarged text`() {
        assertLargeActions(darkTheme = false, useOledTheme = false, direction = LayoutDirection.Ltr)
    }

    @Test
    fun `actual empty library OLED RTL actions remain readable with enlarged text`() {
        assertLargeActions(darkTheme = true, useOledTheme = true, direction = LayoutDirection.Rtl)
    }

    private fun assertLargeActions(darkTheme: Boolean, useOledTheme: Boolean, direction: LayoutDirection) {
        val calls = mutableListOf<String>()
        setNoFeedsContent(
            width = 360.dp,
            darkTheme = darkTheme,
            useOledTheme = useOledTheme,
            onAddFeed = { calls += "add" },
            onImportExport = { calls += "import" },
            onOldEmptyStateAction = { calls += "old chooser" },
            fontScale = 2f,
            direction = direction,
        )
        listOf(EnFeedFlowStrings.addFeed, EnFeedFlowStrings.importExportLabel).forEach { label ->
            val node = rule.onNodeWithText(label)
            node.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse(layouts.single().hasVisualOverflow, "$label must not clip")
            node.performClick()
        }
        assertEquals(listOf("add", "import"), calls)
    }

    @Test
    fun `loading empty timeline shows progress instead of no feeds actions`() {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = 500.dp, height = 400.dp)) {
                        DesktopHomeScreenContent(
                            displayState = displayState(StartedFeedUpdateStatus),
                            feedListActions = feedListActions(),
                            feedManagementActions = feedManagementActions {},
                            shareBehavior = shareBehavior(),
                            listState = remember { LazyListState() },
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

        rule.onNodeWithText(EnFeedFlowStrings.noFeedsFoundMessage).assertDoesNotExist()
        rule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun `actual empty timeline keeps actions readable and inside compact host`() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val fontScale = mutableStateOf(1f)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val calls = mutableListOf<String>()
        setEmptyTimelineContent(
            theme = theme,
            fontScale = fontScale,
            direction = direction,
            filter = mutableStateOf(FeedFilter.Timeline),
            onRefresh = { calls += "refresh" },
            onDrawer = { calls += "drawer" },
            onNext = { calls += "next" },
        )
        val labels = listOf(EnFeedFlowStrings.refreshFeeds, EnFeedFlowStrings.openAnotherFeed, "Kotlin")
        for (scale in listOf(1f, 2f)) {
            for (layoutDirection in LayoutDirection.entries) {
                for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED)) {
                    rule.runOnIdle {
                        fontScale.value = scale
                        direction.value = layoutDirection
                        theme.value = mode
                    }
                    labels.forEach(::assertReadableInHost)
                }
            }
        }
        labels.forEach { rule.onNodeWithText(it).performClick() }
        assertEquals(listOf("refresh", "drawer", "next"), calls)
    }

    @Test
    fun `empty read and bookmarks return to timeline and keep drawer and next actions`() {
        val filter = mutableStateOf<FeedFilter>(FeedFilter.Read)
        val calls = mutableListOf<String>()
        setEmptyTimelineContent(
            theme = mutableStateOf(ThemeMode.OLED),
            fontScale = mutableStateOf(2f),
            direction = mutableStateOf(LayoutDirection.Rtl),
            filter = filter,
            onRefresh = { calls += "refresh" },
            onTimeline = { calls += "timeline" },
            onDrawer = { calls += "drawer" },
            onNext = { calls += "next" },
        )
        listOf(
            FeedFilter.Read to EnFeedFlowStrings.readArticlesEmptyScreenMessage,
            FeedFilter.Bookmarks to EnFeedFlowStrings.bookmarkedArticlesEmptyScreenMessage,
        ).forEach { (currentFilter, message) ->
            rule.runOnIdle {
                filter.value = currentFilter
                calls.clear()
            }
            assertReadableInHost(message)
            listOf(EnFeedFlowStrings.emptyScreenBackToTimeline, EnFeedFlowStrings.openAnotherFeed, "Kotlin")
                .forEach { label ->
                    assertReadableInHost(label)
                    rule.onNodeWithText(label).performClick()
                }
            rule.onNodeWithText(EnFeedFlowStrings.refreshFeeds).assertDoesNotExist()
            assertEquals(listOf("timeline", "drawer", "next"), calls)
        }
    }

    private fun assertReadableInHost(label: String) {
        val host = rule.onNodeWithTag("empty-state-host").fetchSemanticsNode().boundsInRoot
        val node = rule.onNodeWithText(label)
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow, "$label must not clip: ${layouts.single().size}")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertFalse(bounds.left < host.left || bounds.top < host.top, "$label: $bounds in $host")
        assertFalse(bounds.right > host.right || bounds.bottom > host.bottom, "$label: $bounds in $host")
    }

    private fun setNoFeedsContent(
        width: androidx.compose.ui.unit.Dp,
        darkTheme: Boolean,
        useOledTheme: Boolean,
        onAddFeed: () -> Unit,
        onImportExport: () -> Unit,
        onOldEmptyStateAction: () -> Unit,
        fontScale: Float = 1f,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalLayoutDirection provides direction,
            ) {
                FeedFlowTheme(darkTheme = darkTheme, useOledTheme = useOledTheme) {
                    Box(Modifier.requiredSize(width = width, height = 400.dp)) {
                        DesktopHomeScreenContent(
                            displayState = displayState(NoFeedSourcesStatus),
                            feedListActions = feedListActions(),
                            feedManagementActions = feedManagementActions(onAddFeed),
                            shareBehavior = shareBehavior(),
                            listState = remember { LazyListState() },
                            snackbarHostState = remember { DesktopFeedbackState() },
                            onSearchClick = {},
                            pendingNewArticlesCount = 0,
                            onShowNewArticlesClicked = {},
                            onEmptyStateClick = onOldEmptyStateAction,
                            onImportExportClick = onImportExport,
                        )
                    }
                }
            }
        }
    }

    private fun setEmptyTimelineContent(
        theme: State<ThemeMode>,
        fontScale: State<Float>,
        direction: State<LayoutDirection>,
        filter: State<FeedFilter>,
        onRefresh: () -> Unit = {},
        onTimeline: () -> Unit = {},
        onDrawer: () -> Unit = {},
        onNext: () -> Unit = {},
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(LocalDensity.current.density, fontScale.value),
                LocalLayoutDirection provides direction.value,
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    Box(Modifier.requiredSize(width = 360.dp, height = 500.dp).testTag("empty-state-host")) {
                        DesktopHomeScreenContent(
                            displayState = displayState(
                                status = FinishedFeedUpdateStatus,
                                filter = filter.value,
                                nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayEnabledState("Kotlin"),
                            ),
                            feedListActions = feedListActions(
                                onRefresh = onRefresh,
                                onTimeline = onTimeline,
                                onNext = onNext,
                            ),
                            feedManagementActions = feedManagementActions {},
                            shareBehavior = shareBehavior(),
                            listState = remember { LazyListState() },
                            snackbarHostState = remember { DesktopFeedbackState() },
                            onSearchClick = {},
                            pendingNewArticlesCount = 0,
                            onShowNewArticlesClicked = {},
                            onImportExportClick = {},
                            onDrawerMenuClick = onDrawer,
                        )
                    }
                }
            }
        }
    }

    private fun displayState(
        status: FeedUpdateStatus,
        filter: FeedFilter = FeedFilter.Timeline,
        nextFeedDisplayState: NextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
    ) = HomeDisplayState(
        feedItems = persistentListOf(),
        navDrawerState = com.prof18.feedflow.core.model.NavDrawerState(),
        unReadCount = 0,
        isUnreadCountHidden = false,
        feedUpdateStatus = status,
        feedFontSizes = FeedFontSizes(),
        currentFeedFilter = filter,
        nextFeedDisplayState = nextFeedDisplayState,
        swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
        feedLayout = FeedLayout.LIST,
        isGridLayoutEnabled = false,
        feedItemDisplaySettings = FeedItemDisplaySettings(),
    )

    private fun feedListActions(
        onRefresh: () -> Unit = {},
        onTimeline: () -> Unit = {},
        onNext: () -> Unit = {},
    ) = FeedListActions(
        onClearOldArticlesClicked = {},
        onDeleteDatabaseClick = {},
        refreshData = onRefresh,
        requestNewData = {},
        markAllRead = {},
        onBackToTimelineClick = onTimeline,
        onVisibleFeedItemsChanged = {},
        markAsRead = {},
        openUrl = {},
        openInBrowser = {},
        updateBookmarkStatus = { _, _ -> },
        updateReadStatus = { _, _ -> },
        markAllAboveAsRead = {},
        markAllBelowAsRead = {},
        onNavigateNext = onNext,
    )

    private fun feedManagementActions(onAddFeed: () -> Unit) = FeedManagementActions(
        onAddFeedClick = onAddFeed,
        onFeedFilterSelected = {},
        onEditFeedClick = {},
        onDeleteFeedSourceClick = {},
        onPinFeedClick = {},
        onEditCategoryClick = { _, _ -> },
        validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
        onDeleteCategoryClick = {},
        onOpenWebsite = {},
    )

    private fun shareBehavior() = ShareBehavior(
        onShareClick = {},
        shareLinkTitle = EnFeedFlowStrings.menuCopyLink,
        shareCommentsTitle = EnFeedFlowStrings.menuCopyLinkComments,
    )
}
