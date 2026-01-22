package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedAddState
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.suggestions.suggestedFeeds
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedSuggestionsViewModelTest : KoinTestBase() {

    private val viewModel: FeedSuggestionsViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `initial state is loaded correctly`() = runTest {
        viewModel.isLoadingState.test {
            assertEquals(false, awaitItem())
        }

        viewModel.suggestedCategoriesState.test {
            assertEquals(suggestedFeeds, awaitItem())
        }

        viewModel.selectedCategoryIdState.test {
            assertEquals(suggestedFeeds.first().id, awaitItem())
        }
    }

    @Test
    fun `loadExistingFeeds updates feedStatesMapState`() = runTest {
        val suggestedFeed = suggestedFeeds.first().feeds.first()
        val url = suggestedFeed.url
        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = "1",
                    url = url,
                    title = suggestedFeed.name,
                    category = null,
                    logoUrl = null,
                    websiteUrl = null,
                ),
            ),
        )

        viewModel.feedStatesMapState.test {
            // Initial state might be empty or already contain the feed if loadExistingFeeds finished fast
            var item = awaitItem()
            if (item.isEmpty()) {
                item = awaitItem()
            }
            assertEquals(FeedAddState.Added, item[url])
        }
    }

    @Test
    fun `selectCategory updates selectedCategoryIdState`() = runTest {
        val secondCategoryId = suggestedFeeds[1].id
        viewModel.selectCategory(secondCategoryId)

        viewModel.selectedCategoryIdState.test {
            assertEquals(secondCategoryId, awaitItem())
        }
    }

    @Test
    fun `addFeed updates state and database`() = runTest {
        val suggestedFeed = suggestedFeeds.first().feeds.first()
        val categoryName = "Tech"

        viewModel.addFeed(suggestedFeed, categoryName)

        viewModel.feedStatesMapState.test {
            // It might go through Adding state
            var item = awaitItem()
            if (item[suggestedFeed.url] == FeedAddState.Adding) {
                item = awaitItem()
            }
            assertEquals(FeedAddState.Added, item[suggestedFeed.url])
        }

        val sources = databaseHelper.getFeedSources()
        assertTrue(sources.any { it.url == suggestedFeed.url })
    }
}
