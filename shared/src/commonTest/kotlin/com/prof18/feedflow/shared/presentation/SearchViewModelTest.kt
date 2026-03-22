package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

class SearchViewModelTest : KoinTestBase() {

    @Test
    fun `initial state uses timeline defaults`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.searchState.value shouldBe SearchState.EmptyState
        viewModel.searchQueryState.value shouldBe ""
        viewModel.searchFilterState.value shouldBe SearchFilter.All
        viewModel.searchFeedFilterState.value shouldBe null
    }

    @Test
    fun `initial search filter uses bookmarks when current feed filter is bookmarks`() = runTest(testDispatcher) {
        val feedStateRepository = getFeedStateRepository()

        feedStateRepository.updateFeedFilter(FeedFilter.Bookmarks)
        val viewModel = getViewModel()

        viewModel.searchFilterState.value shouldBe SearchFilter.Bookmarks
        viewModel.searchFeedFilterState.value shouldBe null
    }

    @Test
    fun `initial search filter uses read when current feed filter is read`() = runTest(testDispatcher) {
        val feedStateRepository = getFeedStateRepository()

        feedStateRepository.updateFeedFilter(FeedFilter.Read)
        val viewModel = getViewModel()

        viewModel.searchFilterState.value shouldBe SearchFilter.Read
        viewModel.searchFeedFilterState.value shouldBe null
    }

    @Test
    fun `initial search filter uses current feed for source filter and narrows search`() = runTest(testDispatcher) {
        val feedStateRepository = getFeedStateRepository()
        val databaseHelper = getDatabaseHelper()

        val sourceA = createFeedSource(id = "source-a", title = "Source A")
        val sourceB = createFeedSource(id = "source-b", title = "Source B")
        insertFeedSources(databaseHelper, sourceA, sourceB)

        val itemA = createFeedItem(
            id = "item-a",
            title = "Query Match",
            feedSource = sourceA,
            pubDateMillis = 2000,
        )
        val itemB = createFeedItem(
            id = "item-b",
            title = "Query Match",
            feedSource = sourceB,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(itemA, itemB), lastSyncTimestamp = 0)

        feedStateRepository.updateFeedSourceFilter(sourceA.id)
        val viewModel = getViewModel()

        viewModel.searchFilterState.value shouldBe SearchFilter.CurrentFeed
        val searchFeedFilter = viewModel.searchFeedFilterState.value
        assertIs<FeedFilter.Source>(searchFeedFilter)
        searchFeedFilter.feedSource.id shouldBe sourceA.id

        viewModel.updateSearchQuery("Query")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        val state = viewModel.searchState.value as SearchState.DataFound
        state.items.map { it.id } shouldBe listOf(itemA.id)
    }

    @Test
    fun `updateSearchFilter does not search when query is blank`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.updateSearchFilter(SearchFilter.Read)

        viewModel.searchState.value shouldBe SearchState.EmptyState
        viewModel.searchFilterState.value shouldBe SearchFilter.Read
    }

    @Test
    fun `search returns no data when query does not match`() = runTest(testDispatcher) {
        val viewModel = getViewModel()

        viewModel.updateSearchQuery("missing")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        viewModel.searchState.value shouldBe SearchState.NoDataFound(searchQuery = "missing")
    }

    @Test
    fun `search treats hyphen as separator for FTS queries`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val databaseHelper = getDatabaseHelper()

        val feedSource = createFeedSource(id = "source-hyphen", title = "Source Hyphen")
        insertFeedSources(databaseHelper, feedSource)

        val hyphenatedItem = createFeedItem(
            id = "item-hyphenated",
            title = "Mini-Solaranlage guide",
            feedSource = feedSource,
            pubDateMillis = 2000,
        )
        val controlItem = createFeedItem(
            id = "item-control",
            title = "Miniature lights",
            feedSource = feedSource,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(hyphenatedItem, controlItem), lastSyncTimestamp = 0)

        viewModel.updateSearchQuery("Mini-Solar")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        val state = viewModel.searchState.value as SearchState.DataFound
        state.items.map { it.id } shouldBe listOf(hyphenatedItem.id)
    }

    @Test
    fun `search preserves plus symbols for FTS queries`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val databaseHelper = getDatabaseHelper()

        val feedSource = createFeedSource(id = "source-symbols", title = "Source Symbols")
        insertFeedSources(databaseHelper, feedSource)

        val cppItem = createFeedItem(
            id = "item-cpp",
            title = "C++ Memory model explained",
            feedSource = feedSource,
            pubDateMillis = 2000,
        )
        val controlItem = createFeedItem(
            id = "item-control-symbols",
            title = "Cats weekly roundup",
            feedSource = feedSource,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(cppItem, controlItem), lastSyncTimestamp = 0)

        viewModel.updateSearchQuery("C++")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        val state = viewModel.searchState.value as SearchState.DataFound
        state.items.map { it.id } shouldBe listOf(cppItem.id)
    }

    @Test
    fun `search preserves hash symbols for FTS queries`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val databaseHelper = getDatabaseHelper()

        val feedSource = createFeedSource(id = "source-hash-symbols", title = "Source Hash Symbols")
        insertFeedSources(databaseHelper, feedSource)

        val cSharpItem = createFeedItem(
            id = "item-csharp",
            title = "C# Pattern matching tips",
            feedSource = feedSource,
            pubDateMillis = 2000,
        )
        val controlItem = createFeedItem(
            id = "item-control-hash-symbols",
            title = "Cats weekly roundup",
            feedSource = feedSource,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(cSharpItem, controlItem), lastSyncTimestamp = 0)

        viewModel.updateSearchQuery("C#")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        val state = viewModel.searchState.value as SearchState.DataFound
        state.items.map { it.id } shouldBe listOf(cSharpItem.id)
    }

    @Test
    fun `updateSearchFilter re-runs search for read and bookmarked items`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val databaseHelper = getDatabaseHelper()

        val feedSource = createFeedSource(id = "source-3", title = "Source 3")
        insertFeedSources(databaseHelper, feedSource)

        val readItem = createFeedItem(
            id = "item-read",
            title = "Filter Match",
            feedSource = feedSource,
            pubDateMillis = 3000,
            isRead = true,
        )
        val bookmarkedItem = createFeedItem(
            id = "item-bookmarked",
            title = "Filter Match",
            feedSource = feedSource,
            pubDateMillis = 2000,
            isBookmarked = true,
        )
        val otherItem = createFeedItem(
            id = "item-other",
            title = "Filter Match",
            feedSource = feedSource,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(readItem, bookmarkedItem, otherItem), lastSyncTimestamp = 0)
        databaseHelper.updateReadStatus(FeedItemId(readItem.id), true)
        databaseHelper.updateBookmarkStatus(FeedItemId(bookmarkedItem.id), true)

        viewModel.updateSearchQuery("Filter")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        val initialState = viewModel.searchState.value as SearchState.DataFound
        initialState.items.map { it.id } shouldBe listOf(
            readItem.id,
            bookmarkedItem.id,
            otherItem.id,
        )

        viewModel.updateSearchFilter(SearchFilter.Read)

        advanceUntilIdle()

        val readState = viewModel.searchState.value as SearchState.DataFound
        readState.items.map { it.id } shouldBe listOf(readItem.id)

        viewModel.updateSearchFilter(SearchFilter.Bookmarks)

        advanceUntilIdle()

        val bookmarkState = viewModel.searchState.value as SearchState.DataFound
        bookmarkState.items.map { it.id } shouldBe listOf(bookmarkedItem.id)
    }

    @Test
    fun `bookmark and read actions update the database`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val databaseHelper = getDatabaseHelper()

        val feedSource = createFeedSource(id = "source-4", title = "Source 4")
        insertFeedSources(databaseHelper, feedSource)

        val item = createFeedItem(
            id = "item-actions",
            title = "Actions Item",
            feedSource = feedSource,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)

        viewModel.onBookmarkClick(FeedItemId(item.id), bookmarked = true)
        viewModel.onReadStatusClick(FeedItemId(item.id), read = true)
        advanceUntilIdle()

        val dbItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val dbItem = dbItems.first { it.url_hash == item.id }
        assertEquals(true, dbItem.is_bookmarked)
        assertEquals(true, dbItem.is_read)
    }

    @Test
    fun `markAllAboveAsRead marks unread items up to target`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val databaseHelper = getDatabaseHelper()

        val feedSource = createFeedSource(id = "source-5", title = "Source 5")
        insertFeedSources(databaseHelper, feedSource)

        val item1 = createFeedItem(
            id = "above-1",
            title = "Mark Above",
            feedSource = feedSource,
            pubDateMillis = 3000,
            isRead = true,
        )
        val item2 = createFeedItem(
            id = "above-2",
            title = "Mark Above",
            feedSource = feedSource,
            pubDateMillis = 2000,
        )
        val item3 = createFeedItem(
            id = "above-3",
            title = "Mark Above",
            feedSource = feedSource,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(item1, item2, item3), lastSyncTimestamp = 0)
        databaseHelper.updateReadStatus(FeedItemId(item1.id), true)
        viewModel.updateSearchQuery("Mark")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        viewModel.markAllAboveAsRead(item2.id)
        advanceUntilIdle()

        val dbItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val itemsById = dbItems.associateBy { it.url_hash }
        assertEquals(true, itemsById.getValue(item1.id).is_read)
        assertEquals(true, itemsById.getValue(item2.id).is_read)
        assertEquals(false, itemsById.getValue(item3.id).is_read)
    }

    @Test
    fun `markAllBelowAsRead marks unread items from target to end`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val databaseHelper = getDatabaseHelper()

        val feedSource = createFeedSource(id = "source-6", title = "Source 6")
        insertFeedSources(databaseHelper, feedSource)

        val item1 = createFeedItem(
            id = "below-1",
            title = "Mark Below",
            feedSource = feedSource,
            pubDateMillis = 3000,
        )
        val item2 = createFeedItem(
            id = "below-2",
            title = "Mark Below",
            feedSource = feedSource,
            pubDateMillis = 2000,
        )
        val item3 = createFeedItem(
            id = "below-3",
            title = "Mark Below",
            feedSource = feedSource,
            pubDateMillis = 1000,
        )
        databaseHelper.insertFeedItems(listOf(item1, item2, item3), lastSyncTimestamp = 0)
        viewModel.updateSearchQuery("Mark")

        advanceTimeBy(500.milliseconds)
        advanceUntilIdle()

        viewModel.markAllBelowAsRead(item2.id)
        advanceUntilIdle()

        val dbItems = databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = 10,
            offset = 0,
            showReadItems = true,
            sortOrder = FeedOrder.NEWEST_FIRST,
        )
        val itemsById = dbItems.associateBy { it.url_hash }
        assertEquals(false, itemsById.getValue(item1.id).is_read)
        assertEquals(true, itemsById.getValue(item2.id).is_read)
        assertEquals(true, itemsById.getValue(item3.id).is_read)
    }

    @Test
    fun `errorState maps repository errors to UI errors`() = runTest(testDispatcher) {
        val viewModel = getViewModel()
        val feedStateRepository = getFeedStateRepository()

        viewModel.errorState.test {
            val feedJob = launch {
                feedStateRepository.emitErrorState(FeedErrorState(failingSourceName = "Feed"))
            }
            advanceUntilIdle()
            assertEquals(UIErrorState.FeedErrorState(feedName = "Feed"), awaitItem())
            feedJob.cancel()

            val dbJob = launch {
                feedStateRepository.emitErrorState(DatabaseError(DatabaseErrorCode.PaginationFailed))
            }
            advanceUntilIdle()
            assertEquals(UIErrorState.DatabaseError(errorCode = DatabaseErrorCode.PaginationFailed), awaitItem())
            dbJob.cancel()

            val syncJob = launch {
                feedStateRepository.emitErrorState(SyncError(FeedSyncError.MarkItemsAsReadFailed))
            }
            advanceUntilIdle()
            assertEquals(UIErrorState.SyncError(errorCode = FeedSyncError.MarkItemsAsReadFailed), awaitItem())
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
    ) = FeedSource(
        id = id,
        url = "https://example.com/$id/feed.xml",
        title = title,
        category = category,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com/$id",
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )

    private fun createFeedItem(
        id: String,
        title: String,
        feedSource: FeedSource,
        pubDateMillis: Long,
        subtitle: String? = null,
        isRead: Boolean = false,
        isBookmarked: Boolean = false,
    ) = FeedItem(
        id = id,
        url = "https://example.com/articles/$id",
        title = title,
        subtitle = subtitle,
        content = null,
        imageUrl = null,
        feedSource = feedSource,
        pubDateMillis = pubDateMillis,
        isRead = isRead,
        dateString = null,
        commentsUrl = null,
        isBookmarked = isBookmarked,
    )

    private fun getViewModel(): SearchViewModel = get()

    private fun getFeedStateRepository(): FeedStateRepository = get()

    private fun getDatabaseHelper(): DatabaseHelper = get()
}
