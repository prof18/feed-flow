package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.home.DesktopFeedTreeState
import com.prof18.feedflow.desktop.home.LocalDesktopFeedTreeState
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class FeedSidebarCollapseMotionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `rows below a category move monotonically while it collapses and expands`() {
        rule.mainClock.autoAdvance = false
        val drawer = DesktopLibraryFixture.load().drawerState()
        val titles = drawer.feedSourcesByCategory.keys.mapNotNull { it.feedSourceCategory?.title }
        val target = titles[0]
        val next = titles[1]
        val state = HomeDisplayState(
            feedItems = persistentListOf(),
            navDrawerState = drawer,
            unReadCount = 0,
            isUnreadCountHidden = false,
            feedUpdateStatus = NoFeedSourcesStatus,
            feedFontSizes = FeedFontSizes(),
            currentFeedFilter = FeedFilter.Timeline,
            nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
            swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
            feedLayout = FeedLayout.LIST,
            isGridLayoutEnabled = false,
        )
        val actions = FeedManagementActions(
            onAddFeedClick = {},
            onFeedFilterSelected = {},
            onEditFeedClick = {},
            onDeleteFeedSourceClick = {},
            onPinFeedClick = {},
            onEditCategoryClick = { _: CategoryId, _: CategoryName -> },
            validateCategoryName = { _: CategoryId?, _: CategoryName -> CategoryNameValidationResult.VALID },
            onDeleteCategoryClick = {},
            onOpenWebsite = {},
        )
        val treeState = DesktopFeedTreeState()
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDesktopFeedTreeState provides treeState,
                LocalReduceMotion provides false,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(260.dp, 760.dp)) {
                        FeedSidebar(displayState = state, feedManagementActions = actions, onFeedFilterSelected = {})
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(SETTLE_MS)

        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopCollapseFeedCategory(target)).performClick()
        val collapsing = recordHeaderTops(next)
        assertMonotonic(collapsing, rising = false, phase = "collapse")

        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopExpandFeedCategory(target)).performClick()
        val expanding = recordHeaderTops(next)
        assertMonotonic(expanding, rising = true, phase = "expand")
    }

    private fun recordHeaderTops(title: String): List<Float> = List(FRAMES) {
        val top = rule.onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        rule.mainClock.advanceTimeBy(FRAME_MS)
        top
    }

    private fun assertMonotonic(tops: List<Float>, rising: Boolean, phase: String) {
        assertTrue(tops.first() != tops.last(), "$phase: the next header should move, got $tops")
        tops.zipWithNext().forEach { (previous, current) ->
            val ok = if (rising) current >= previous else current <= previous
            assertTrue(ok, "$phase: the next header jumped from $previous to $current in $tops")
        }
    }

    private companion object {
        const val SETTLE_MS = 2_000L
        const val FRAME_MS = 16L
        const val FRAMES = 60
    }
}
