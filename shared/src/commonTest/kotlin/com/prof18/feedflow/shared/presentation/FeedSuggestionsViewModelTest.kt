package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.suggestions.suggestedFeeds
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FeedSuggestionsViewModelTest : KoinTestBase() {

    private val viewModel: FeedSuggestionsViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `initial state is loaded correctly`() = runTest {
        viewModel.isLoadingState.test {
            assertFalse(awaitItem())
        }

        viewModel.suggestedCategoriesState.test {
            assertEquals(suggestedFeeds, awaitItem())
        }

        viewModel.selectedCategoryIdState.test {
            assertEquals(suggestedFeeds.first().id, awaitItem())
        }
    }

    @Test
    fun `subscribed feeds are shown as added`() = runTest {
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

        viewModel.operationStates.test {
            // Initial state might be empty or already contain the feed if loadExistingFeeds finished fast
            var item = awaitItem()
            if (item.isEmpty()) {
                item = awaitItem()
            }
            assertEquals(FeedSuggestionOperationStatus.Added, item[url]?.status)
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
    fun `removing an existing suggested feed clears added state`() = runTest {
        val suggestedFeed = suggestedFeeds.first().feeds.first()
        val feedId = "1"

        databaseHelper.insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = feedId,
                    url = suggestedFeed.url,
                    title = suggestedFeed.name,
                    category = null,
                    logoUrl = null,
                    websiteUrl = null,
                ),
            ),
        )

        viewModel.operationStates.test {
            var item = awaitItem()
            if (item[suggestedFeed.url]?.status != FeedSuggestionOperationStatus.Added) {
                item = awaitItem()
            }
            assertEquals(FeedSuggestionOperationStatus.Added, item[suggestedFeed.url]?.status)

            databaseHelper.deleteFeedSource(feedId)

            item = awaitItem()
            assertFalse(item.containsKey(suggestedFeed.url))
        }
    }
}
