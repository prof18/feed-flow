package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import io.kotest.property.arbitrary.next
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

class FeedSourceListViewModelTest : KoinTestBase() {

    private val viewModel: FeedSourceListViewModel by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    @Test
    fun `initial state is empty`() = runTest(testDispatcher) {
        assertTrue(viewModel.feedSourcesState.value.isEmpty())
    }

    @Test
    fun `uncategorized feed sources are sorted by title`() = runTest(testDispatcher) {
        val sourceB = createFeedSource(id = "source-b", title = "Beta")
        val sourceA = createFeedSource(id = "source-a", title = "Alpha")
        insertFeedSources(databaseHelper, sourceB, sourceA)

        advanceUntilIdle()

        val state = viewModel.feedSourcesState.value
        assertEquals(listOf("Alpha", "Beta"), state.feedSourcesWithoutCategory.map { it.title })
        assertTrue(state.feedSourcesWithCategory.isEmpty())
    }

    @Test
    fun `feed sources with categories are grouped`() = runTest(testDispatcher) {
        val techCategory = FeedSourceCategory(id = "tech", title = "Tech")
        val newsCategory = FeedSourceCategory(id = "news", title = "News")
        val techSource = createFeedSource(id = "tech-source", title = "Tech Source", category = techCategory)
        val newsSource = createFeedSource(id = "news-source", title = "News Source", category = newsCategory)

        insertFeedSources(databaseHelper, techSource, newsSource)

        advanceUntilIdle()

        val grouped = viewModel.feedSourcesState.value.feedSourcesWithCategory.associate { state ->
            state.categoryId to state.feedSources.map { it.id }
        }

        assertEquals(setOf(CategoryId("tech"), CategoryId("news")), grouped.keys)
        assertEquals(listOf(techSource.id), grouped[CategoryId("tech")])
        assertEquals(listOf(newsSource.id), grouped[CategoryId("news")])
    }

    @Test
    fun `expandCategory toggles expanded state`() = runTest(testDispatcher) {
        val techCategory = FeedSourceCategory(id = "tech", title = "Tech")
        val techSource = createFeedSource(id = "tech-source", title = "Tech Source", category = techCategory)
        insertFeedSources(databaseHelper, techSource)

        advanceUntilIdle()

        viewModel.expandCategory(CategoryId("tech"))

        var state = viewModel.feedSourcesState.value
        assertTrue(state.feedSourcesWithCategory.first().isExpanded)

        viewModel.expandCategory(CategoryId("tech"))

        state = viewModel.feedSourcesState.value
        assertTrue(state.feedSourcesWithCategory.first().isExpanded.not())
    }

    @Test
    fun `updateFeedName updates database and feed state`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-1", title = "Old Name")
        val feedItem = createFeedItem(id = "item-1", title = "Item Title", feedSource = feedSource)
        insertFeedSources(databaseHelper, feedSource)
        databaseHelper.insertFeedItems(listOf(feedItem), lastSyncTimestamp = 0)

        feedStateRepository.feedState.test {
            assertTrue(awaitItem().isEmpty())

            viewModel.updateFeedName(feedSource, "New Name")
            advanceUntilIdle()

            val updatedItems = awaitItem()
            assertEquals("New Name", updatedItems.first().feedSource.title)
        }
    }

    @Test
    fun `toggleFeedPin updates database preference`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-2", title = "Pinned Source")
        insertFeedSources(databaseHelper, feedSource)

        viewModel.toggleFeedPin(feedSource)
        advanceUntilIdle()

        val updatedSource = databaseHelper.getFeedSource(feedSource.id)
        assertEquals(true, updatedSource?.isPinned)
    }

    @Test
    fun `deleteFeedSource removes it from database and state`() = runTest(testDispatcher) {
        val feedSource = createFeedSource(id = "source-3", title = "Delete Me")
        insertFeedSources(databaseHelper, feedSource)

        advanceUntilIdle()
        assertTrue(viewModel.feedSourcesState.value.isEmpty().not())

        viewModel.deleteFeedSource(feedSource)
        advanceUntilIdle()

        assertTrue(viewModel.feedSourcesState.value.isEmpty())
    }

    @Test
    fun `deleteAllFeedsInCategory removes all feeds in category`() = runTest(testDispatcher) {
        val techCategory = FeedSourceCategory(id = "tech", title = "Tech")
        val newsCategory = FeedSourceCategory(id = "news", title = "News")
        val techSource1 = createFeedSource(id = "tech-1", title = "Tech Source 1", category = techCategory)
        val techSource2 = createFeedSource(id = "tech-2", title = "Tech Source 2", category = techCategory)
        val newsSource = createFeedSource(id = "news-1", title = "News Source", category = newsCategory)

        insertFeedSources(databaseHelper, techSource1, techSource2, newsSource)
        advanceUntilIdle()

        val stateBefore = viewModel.feedSourcesState.value
        assertEquals(2, stateBefore.feedSourcesWithCategory.size)

        viewModel.deleteAllFeedsInCategory(listOf(techSource1, techSource2))
        advanceUntilIdle()

        val stateAfter = viewModel.feedSourcesState.value
        val remainingIds = stateAfter.feedSourcesWithCategory.flatMap { it.feedSources.map { fs -> fs.id } }
        assertEquals(listOf("news-1"), remainingIds)
    }

    @Test
    fun `errorState emits mapped errors`() = runTest(testDispatcher) {
        viewModel.errorState.test {
            val feedJob = launch {
                feedStateRepository.emitErrorState(FeedErrorState(failingSourceName = "Failing Feed"))
            }
            advanceUntilIdle()
            assertEquals(UIErrorState.FeedErrorState(feedName = "Failing Feed"), awaitItem())
            feedJob.cancel()

            val dbJob = launch {
                feedStateRepository.emitErrorState(DatabaseError(DatabaseErrorCode.FeedRetrievalFailed))
            }
            advanceUntilIdle()
            assertEquals(UIErrorState.DatabaseError(errorCode = DatabaseErrorCode.FeedRetrievalFailed), awaitItem())
            dbJob.cancel()

            val syncJob = launch {
                feedStateRepository.emitErrorState(SyncError(FeedSyncError.DeleteFeedSourceFailed))
            }
            advanceUntilIdle()
            assertEquals(UIErrorState.SyncError(errorCode = FeedSyncError.DeleteFeedSourceFailed), awaitItem())
            syncJob.cancel()

            val deleteJob = launch {
                feedStateRepository.emitErrorState(DeleteFeedSourceError())
            }
            advanceUntilIdle()
            assertEquals(UIErrorState.DeleteFeedSourceError, awaitItem())
            deleteJob.cancel()
        }
    }

    private suspend fun insertFeedSources(databaseHelper: DatabaseHelper, vararg sources: FeedSource) {
        val categories = sources.mapNotNull { it.category }.distinctBy { it.id }
        if (categories.isNotEmpty()) {
            databaseHelper.insertCategories(categories)
        }
        databaseHelper.insertFeedSource(
            sources.map { source ->
                ParsedFeedSource(
                    id = source.id,
                    url = source.url,
                    title = source.title,
                    category = source.category,
                    logoUrl = source.logoUrl,
                    websiteUrl = source.websiteUrl,
                )
            },
        )
    }

    private fun createFeedSource(
        id: String,
        title: String,
        category: FeedSourceCategory? = null,
    ) = FeedSourceGenerator.feedSourceArb.next().copy(
        id = id,
        url = "https://example.com/$id/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        fetchFailed = false,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )

    private fun createFeedItem(
        id: String,
        title: String,
        feedSource: FeedSource,
    ) = FeedItemGenerator.unreadFeedItemArb().next().copy(
        id = id,
        url = "https://example.com/articles/$id",
        title = title,
        subtitle = null,
        content = null,
        imageUrl = null,
        feedSource = feedSource,
        pubDateMillis = 1000,
        dateString = null,
        commentsUrl = null,
        isBookmarked = false,
    )
}
