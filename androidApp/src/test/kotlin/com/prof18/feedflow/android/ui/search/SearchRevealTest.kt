package com.prof18.feedflow.android.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.prof18.feedflow.android.ui.preview.feedItemsForPreview
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalReduceMotion
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.runtime.CompositionLocalProvider as ProvideLocals

@RunWith(RobolectricTestRunner::class)
class SearchRevealTest {

    @get:Rule
    val compose = createComposeRule()

    private val items = (0 until ITEM_COUNT).map { index ->
        feedItemsForPreview.first().copy(id = "reveal-$index", title = "Reveal article $index")
    }.toImmutableList()

    private var revealArticleId by mutableStateOf<String?>(null)
    private var searchState by mutableStateOf<SearchState>(dataFound())
    private var isSearchInFlight by mutableStateOf(false)
    private var revealedCount = 0

    @Test
    fun `an off-screen result is scrolled into view and acknowledged`() {
        setSearch()

        revealArticleId = "reveal-30"
        compose.waitForIdle()

        compose.onNodeWithText("Reveal article 30").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, revealedCount) }
    }

    @Test
    fun `a visible result is acknowledged and the list stays at the top`() {
        setSearch()

        revealArticleId = "reveal-0"
        compose.waitForIdle()

        compose.onNodeWithText("Reveal article 0").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, revealedCount) }
    }

    @Test
    fun `a result missing from the list is acknowledged`() {
        setSearch()

        revealArticleId = "missing"
        compose.waitForIdle()

        compose.runOnIdle { assertEquals(1, revealedCount) }
    }

    @Test
    fun `a request stays pending while the search is in flight`() {
        isSearchInFlight = true
        setSearch()

        revealArticleId = "reveal-30"
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, revealedCount) }

        isSearchInFlight = false
        compose.waitForIdle()

        compose.onNodeWithText("Reveal article 30").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, revealedCount) }
    }

    @Test
    fun `a request stays pending while there are no results yet`() {
        searchState = SearchState.EmptyState
        setSearch()

        revealArticleId = "reveal-30"
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, revealedCount) }

        searchState = dataFound()
        compose.waitForIdle()

        compose.onNodeWithText("Reveal article 30").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, revealedCount) }
    }

    private fun dataFound() = SearchState.DataFound(
        items = items,
        feedLayout = FeedLayout.LIST,
        isGridLayoutEnabled = false,
    )

    private fun setSearch() {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) {
                    ProvideLocals(LocalReduceMotion provides true) {
                        SearchScreenContent(
                            searchState = searchState,
                            searchQuery = "Reveal",
                            searchFilter = SearchFilter.All,
                            currentFeedFilter = FeedFilter.Timeline,
                            feedFontSizes = FeedFontSizes(),
                            shareMenuLabel = "Share",
                            shareCommentsMenuLabel = "Share with comments",
                            updateSearchQuery = {},
                            onSearchFilterSelected = {},
                            navigateBack = {},
                            onFeedItemClick = {},
                            onBookmarkClick = { _, _ -> },
                            onReadStatusClick = { _, _ -> },
                            onMarkAllAboveAsRead = {},
                            onMarkAllBelowAsRead = {},
                            onCommentClick = {},
                            onShareClick = {},
                            onOpenFeedSettings = {},
                            onOpenFeedWebsite = {},
                            isSearchInFlight = isSearchInFlight,
                            revealArticleId = revealArticleId,
                            onArticleRevealed = {
                                revealedCount++
                                revealArticleId = null
                            },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val ITEM_COUNT = 40
    }
}
