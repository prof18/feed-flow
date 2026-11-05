package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.desktop.testing.FeedItemE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DesktopSearchRevealTest {
    @get:Rule
    val rule = createComposeRule()

    private var revealArticleId by mutableStateOf<String?>(null)
    private var searchState by mutableStateOf<SearchState>(
        SearchState.DataFound(
            items = createRevealItems(ARTICLE_COUNT).toImmutableList(),
            feedLayout = FeedLayout.LIST,
            isGridLayoutEnabled = false,
        ),
    )
    private var revealedCount = 0

    @Test
    fun `the result reached in the reader is scrolled into view and selected`() {
        setSearch()

        revealArticleId = revealId(TARGET)
        rule.waitForIdle()

        rule.onNodeWithTag(FeedItemE2eIds.row(revealId(TARGET))).assertIsDisplayed().assertIsSelected()
        rule.runOnIdle { assertEquals(1, revealedCount) }
    }

    @Test
    fun `no results acknowledge the request`() {
        searchState = SearchState.NoDataFound(searchQuery = "Article")
        setSearch()

        revealArticleId = revealId(TARGET)
        rule.waitForIdle()

        rule.runOnIdle { assertEquals(1, revealedCount) }
    }

    private fun setSearch() {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalReduceMotion provides true,
            ) {
                FeedFlowTheme(darkTheme = false) {
                    Box(Modifier.requiredSize(width = 500.dp, height = 480.dp)) {
                        DesktopSearchScreenContent(
                            searchState = searchState,
                            searchQuery = "Article",
                            searchFilter = SearchFilter.All,
                            currentFeedFilter = FeedFilter.Timeline,
                            feedFontSizes = FeedFontSizes(),
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
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
        rule.waitForIdle()
    }

    private companion object {
        const val ARTICLE_COUNT = 60
        const val TARGET = 40
    }
}
