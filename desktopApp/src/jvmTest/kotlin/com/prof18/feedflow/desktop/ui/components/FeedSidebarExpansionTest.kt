@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.home.DesktopFeedTreeState
import com.prof18.feedflow.desktop.home.LocalDesktopFeedTreeState
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedSidebarExpansionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `category rows move while collapsing and reopening and reversal settles expanded`() {
        rule.mainClock.autoAdvance = false
        var feedCallbackCount = 0
        val fixture = sidebarFixture()
        val treeState = DesktopFeedTreeState()
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDesktopFeedTreeState provides treeState,
                LocalReduceMotion provides false,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    FeedSidebar(
                        displayState = fixture.displayState,
                        feedManagementActions = fixture.managementActions,
                        onFeedFilterSelected = { if (it is FeedFilter.Source) feedCallbackCount++ },
                        modifier = Modifier.requiredSize(width = 360.dp, height = 600.dp).testTag(SIDEBAR_TAG),
                    )
                }
            }
        }
        rule.waitForIdle()

        val beforeCollapse = sourceBounds()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopCollapseFeedCategory(CATEGORY_TITLE))
            .performClick()
        rule.mainClock.advanceTimeBy(milliseconds = 80)
        val duringCollapse = sourceBounds()
        assertRowsAreMoving(beforeCollapse, duringCollapse, "collapse")
        rule.mainClock.advanceTimeBy(milliseconds = 5_000)
        rule.waitForIdle()
        SOURCE_TITLES.forEach { rule.onNodeWithText(it, useUnmergedTree = true).assertDoesNotExist() }

        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopExpandFeedCategory(CATEGORY_TITLE))
            .performClick()
        rule.mainClock.advanceTimeBy(milliseconds = 80)
        val duringExpand = sourceBounds()
        assertTrue(
            duringExpand.any { (title, bounds) ->
                val settled = beforeCollapse.getValue(title)
                bounds.height != settled.height || bounds.top != settled.top
            },
            "Source row bounds should change during category expansion",
        )
        rule.mainClock.advanceTimeBy(milliseconds = 5_000)
        rule.waitForIdle()
        SOURCE_TITLES.forEach { rule.onNodeWithText(it, useUnmergedTree = true).assertExists() }

        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopCollapseFeedCategory(CATEGORY_TITLE))
            .performClick()
        rule.mainClock.advanceTimeBy(milliseconds = 80)
        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopExpandFeedCategory(CATEGORY_TITLE))
            .performClick()
        rule.mainClock.advanceTimeBy(milliseconds = 5_000)
        rule.waitForIdle()
        SOURCE_TITLES.forEach { rule.onNodeWithText(it, useUnmergedTree = true).assertExists() }
        assertEquals(0, feedCallbackCount, "Disclosure actions must not select a feed")
        rule.onNodeWithText(SOURCE_TITLES.first(), useUnmergedTree = true).performClick()
        rule.runOnIdle { assertEquals(1, feedCallbackCount) }
    }

    @Test
    fun `reduce motion removes collapsed source rows immediately`() {
        rule.mainClock.autoAdvance = false
        val fixture = sidebarFixture()
        val treeState = DesktopFeedTreeState()
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDesktopFeedTreeState provides treeState,
                LocalReduceMotion provides true,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    FeedSidebar(
                        displayState = fixture.displayState,
                        feedManagementActions = fixture.managementActions,
                        onFeedFilterSelected = {},
                        modifier = Modifier.requiredSize(width = 360.dp, height = 600.dp),
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopCollapseFeedCategory(CATEGORY_TITLE))
            .performClick()
        rule.mainClock.advanceTimeByFrame()
        SOURCE_TITLES.forEach { rule.onNodeWithText(it, useUnmergedTree = true).assertDoesNotExist() }
        rule.mainClock.advanceTimeBy(milliseconds = 5_000)
        rule.waitForIdle()
        SOURCE_TITLES.forEach { rule.onNodeWithText(it, useUnmergedTree = true).assertDoesNotExist() }
    }

    private fun sourceBounds(): Map<String, androidx.compose.ui.geometry.Rect> = SOURCE_TITLES.associateWith { title ->
        rule.onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    }

    private fun assertRowsAreMoving(
        before: Map<String, androidx.compose.ui.geometry.Rect>,
        during: Map<String, androidx.compose.ui.geometry.Rect>,
        direction: String,
    ) {
        assertEquals(SOURCE_TITLES.toSet(), during.keys, "All source rows should remain represented mid-$direction")
        assertTrue(
            SOURCE_TITLES.any { title ->
                val start = before.getValue(title)
                val current = during.getValue(title)
                current.top != start.top || current.height != start.height
            },
            "Source row bounds should change during category $direction instead of remaining in place",
        )
    }

    private fun sidebarFixture(): SidebarFixture {
        val category = FeedSourceCategory(id = "expansion-category", title = CATEGORY_TITLE, position = 0)
        val wrappedCategory = DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper(category)
        val sources = SOURCE_TITLES.mapIndexed { index, title ->
            DrawerItem.DrawerFeedSource(
                feedSource = FeedSource(
                    id = "expansion-feed-$index",
                    url = "http://127.0.0.1/feed-$index.xml",
                    title = title,
                    category = category,
                    lastSyncTimestamp = null,
                    logoUrl = null,
                    websiteUrl = null,
                    fetchFailed = false,
                    articleOpenMode = com.prof18.feedflow.core.model.ArticleOpenMode.DEFAULT,
                    isHiddenFromTimeline = false,
                    isPinned = false,
                    isNotificationEnabled = false,
                    isHideImagesEnabled = false,
                ),
                unreadCount = index + 1L,
            )
        }
        val drawerState = NavDrawerState(
            feedSourcesByCategory = persistentMapOf(wrappedCategory to sources),
        )
        return SidebarFixture(
            displayState = HomeDisplayState(
                feedItems = persistentListOf<FeedItem>(),
                navDrawerState = drawerState,
                unReadCount = 0,
                isUnreadCountHidden = false,
                feedUpdateStatus = NoFeedSourcesStatus,
                feedFontSizes = FeedFontSizes(),
                currentFeedFilter = FeedFilter.Timeline,
                nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                feedLayout = FeedLayout.LIST,
                isGridLayoutEnabled = false,
            ),
            managementActions = FeedManagementActions(
                onAddFeedClick = {},
                onFeedFilterSelected = {},
                onEditFeedClick = {},
                onDeleteFeedSourceClick = {},
                onPinFeedClick = {},
                onEditCategoryClick = { _: CategoryId, _: CategoryName -> },
                validateCategoryName = { _: CategoryId?, _: CategoryName -> CategoryNameValidationResult.VALID },
                onDeleteCategoryClick = {},
                onOpenWebsite = {},
            ),
        )
    }

    private data class SidebarFixture(
        val displayState: HomeDisplayState,
        val managementActions: FeedManagementActions,
    )

    private companion object {
        const val CATEGORY_TITLE = "Local research"
        const val SIDEBAR_TAG = "sidebar-expansion-regression"
        val SOURCE_TITLES = listOf("Local feed alpha", "Local feed beta", "Local feed gamma")
    }
}
