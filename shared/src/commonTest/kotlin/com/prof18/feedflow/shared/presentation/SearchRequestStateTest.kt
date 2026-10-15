package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.domain.feed.ArticleNavigationRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.model.SearchRequestState
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.test.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class SearchRequestStateTest : KoinTestBase() {

    @Test
    fun `initial request is idle`() = runTest(testDispatcher) {
        assertEquals(SearchRequestState.Idle, viewModel { _, _ -> flowOf(emptyList()) }.searchRequestState.value)
    }

    @Test
    fun `typing publishes loading before debounce starts repository search`() = runTest(testDispatcher) {
        var calls = 0
        val viewModel = viewModel { _, _ ->
            calls++
            flowOf(emptyList())
        }

        viewModel.searchRequestState.test {
            assertEquals(SearchRequestState.Idle, awaitItem())
            viewModel.updateSearchQuery("first")
            val loading = assertIs<SearchRequestState.Loading>(awaitItem())
            assertEquals("first", loading.request.query)
            assertEquals(SearchFilter.All, loading.request.filter)
            assertEquals(0, calls)

            advanceTimeBy(499.milliseconds)
            runCurrent()
            assertEquals(0, calls)
            assertEquals(loading, viewModel.searchRequestState.value)

            advanceTimeBy(1.milliseconds)
            runCurrent()
            assertEquals(1, calls)
            val result = assertIs<SearchRequestState.Results>(awaitItem())
            assertEquals(loading.request, result.request)
            assertEquals(SearchState.NoDataFound("first"), result.result)
        }
    }

    @Test
    fun `result keeps the request that produced it`() = runTest(testDispatcher) {
        val results = MutableSharedFlow<List<Search>>()
        val viewModel = viewModel { _, _ -> results }

        viewModel.updateSearchQuery("news")
        val request = assertIs<SearchRequestState.Loading>(viewModel.searchRequestState.value).request
        advanceTimeBy(500.milliseconds)
        runCurrent()
        results.emit(emptyList())
        runCurrent()

        val result = assertIs<SearchRequestState.Results>(viewModel.searchRequestState.value)
        assertEquals(request, result.request)
        assertEquals(SearchState.NoDataFound("news"), result.result)
        assertEquals(SearchState.NoDataFound("news"), viewModel.searchState.value)
    }

    @Test
    fun `editing cancels prior subscription before next debounce finishes`() = runTest(testDispatcher) {
        val oldResults = MutableSharedFlow<List<Search>>()
        var oldCancelled = false
        var calls = 0
        val viewModel = viewModel { _, _ ->
            calls++
            oldResults.onCompletion { oldCancelled = true }
        }

        viewModel.updateSearchQuery("old")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        assertEquals(1, calls)
        val oldRequest = assertIs<SearchRequestState.Loading>(viewModel.searchRequestState.value).request

        viewModel.updateSearchQuery("new")
        val newRequest = assertIs<SearchRequestState.Loading>(viewModel.searchRequestState.value).request
        runCurrent()
        assertTrue(oldCancelled)
        assertNotEquals(oldRequest.generation, newRequest.generation)
        assertEquals(1, calls)
        assertEquals(0, oldResults.subscriptionCount.value)
        assertEquals(SearchRequestState.Loading(newRequest), viewModel.searchRequestState.value)
    }

    @Test
    fun `blank query and reset immediately clear and cancel active search`() = runTest(testDispatcher) {
        val results = MutableSharedFlow<List<Search>>()
        var cancellations = 0
        val viewModel = viewModel { _, _ -> results.onCompletion { cancellations++ } }

        viewModel.updateSearchQuery("one")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        viewModel.updateSearchQuery("")
        runCurrent()
        assertEquals(SearchRequestState.Idle, viewModel.searchRequestState.value)
        assertEquals(SearchState.EmptyState, viewModel.searchState.value)
        assertEquals(1, cancellations)
        assertEquals(0, results.subscriptionCount.value)

        viewModel.updateSearchQuery("two")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        viewModel.resetSearch()
        runCurrent()
        assertEquals(SearchRequestState.Idle, viewModel.searchRequestState.value)
        assertEquals(SearchState.EmptyState, viewModel.searchState.value)
        assertEquals(2, cancellations)
        assertEquals(0, results.subscriptionCount.value)
    }

    @Test
    fun `filter change starts a new request with filter provenance`() = runTest(testDispatcher) {
        val calls = mutableListOf<Pair<String, FeedFilter?>>()
        val viewModel = viewModel { query, filter ->
            calls += query to filter
            flowOf(emptyList())
        }

        viewModel.updateSearchQuery("query")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        val first = assertIs<SearchRequestState.Results>(viewModel.searchRequestState.value).request

        viewModel.updateSearchFilter(SearchFilter.Bookmarks)
        runCurrent()
        val second = assertIs<SearchRequestState.Results>(viewModel.searchRequestState.value).request
        assertNotEquals(first.generation, second.generation)
        assertEquals("query", second.query)
        assertEquals(SearchFilter.Bookmarks, second.filter)
        assertEquals(FeedFilter.Bookmarks, second.feedFilter)
        assertEquals(listOf<Pair<String, FeedFilter?>>("query" to null, "query" to FeedFilter.Bookmarks), calls)
    }

    @Test
    fun `failure remains visible until retry creates a new generation and succeeds`() = runTest(testDispatcher) {
        var calls = 0
        val viewModel = viewModel { _, _ ->
            calls++
            if (calls == 1) flow { throw IllegalStateException("search failed") } else flowOf(emptyList())
        }

        viewModel.updateSearchQuery("retry")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        val failed = assertIs<SearchRequestState.Error>(viewModel.searchRequestState.value)
        assertEquals("retry", failed.request.query)
        runCurrent()
        assertEquals(failed, viewModel.searchRequestState.value)

        viewModel.retrySearch()
        runCurrent()
        val succeeded = assertIs<SearchRequestState.Results>(viewModel.searchRequestState.value)
        assertNotEquals(failed.request.generation, succeeded.request.generation)
        assertEquals(failed.request.query, succeeded.request.query)
        assertEquals(failed.request.filter, succeeded.request.filter)
        assertEquals(2, calls)
    }

    @Test
    fun `returning to a completed query after an edit creates a new request`() = runTest(testDispatcher) {
        val calls = mutableListOf<String>()
        val viewModel = viewModel { query, _ ->
            calls += query
            flowOf(emptyList())
        }

        viewModel.updateSearchQuery("A")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        val firstA = assertIs<SearchRequestState.Results>(viewModel.searchRequestState.value).request

        viewModel.updateSearchQuery("B")
        viewModel.updateSearchQuery("A")
        val secondA = assertIs<SearchRequestState.Loading>(viewModel.searchRequestState.value).request
        advanceTimeBy(500.milliseconds)
        runCurrent()

        val result = assertIs<SearchRequestState.Results>(viewModel.searchRequestState.value)
        assertEquals(listOf("A", "A"), calls)
        assertNotEquals(firstA.generation, secondA.generation)
        assertEquals(secondA, result.request)
    }

    @Test
    fun `synchronous source factory failure remains visible until retry succeeds`() = runTest(testDispatcher) {
        var calls = 0
        val viewModel = viewModel { _, _ ->
            calls++
            if (calls == 1) error("factory failed") else flowOf(emptyList())
        }

        viewModel.updateSearchQuery("retry")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        val failed = assertIs<SearchRequestState.Error>(viewModel.searchRequestState.value)
        runCurrent()
        assertEquals(failed, viewModel.searchRequestState.value)

        viewModel.retrySearch()
        runCurrent()
        val succeeded = assertIs<SearchRequestState.Results>(viewModel.searchRequestState.value)
        assertNotEquals(failed.request.generation, succeeded.request.generation)
        assertEquals(2, calls)
    }

    @Test
    fun `cancellation from search flow does not become an error`() = runTest(testDispatcher) {
        val viewModel = viewModel { _, _ -> flow { throw CancellationException("cancelled") } }

        viewModel.updateSearchQuery("cancel")
        advanceTimeBy(500.milliseconds)
        runCurrent()

        assertIs<SearchRequestState.Loading>(viewModel.searchRequestState.value)
    }

    @Test
    fun `retry while loading does not start another search`() = runTest(testDispatcher) {
        var calls = 0
        val results = MutableSharedFlow<List<Search>>()
        val viewModel = viewModel { _, _ ->
            calls++
            results
        }

        viewModel.updateSearchQuery("loading")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        val request = assertIs<SearchRequestState.Loading>(viewModel.searchRequestState.value).request

        viewModel.retrySearch()
        runCurrent()

        assertEquals(1, calls)
        assertEquals(SearchRequestState.Loading(request), viewModel.searchRequestState.value)
    }

    @Test
    fun `only current completed data results are actionable`() = runTest(testDispatcher) {
        var requests = 0
        val viewModel = viewModel { _, _ ->
            requests++
            if (requests == 1) flowOf(listOf(searchResult("item-1"))) else flow { awaitCancellation() }
        }

        assertFalse(viewModel.isCurrentSearchResult("item-1"))
        viewModel.updateSearchQuery("match")
        assertFalse(viewModel.isCurrentSearchResult("item-1"))
        advanceTimeBy(500.milliseconds)
        runCurrent()
        assertTrue(viewModel.isCurrentSearchResult("item-1"))
        assertFalse(viewModel.isCurrentSearchResult("other"))

        viewModel.updateSearchQuery("edited")
        assertFalse(viewModel.isCurrentSearchResult("item-1"))
        viewModel.updateSearchFilter(SearchFilter.Bookmarks)
        assertFalse(viewModel.isCurrentSearchResult("item-1"))
        viewModel.resetSearch()
        assertFalse(viewModel.isCurrentSearchResult("item-1"))
    }

    @Test
    fun `completed data result is not actionable after current request errors`() = runTest(testDispatcher) {
        var requests = 0
        val viewModel = viewModel { _, _ ->
            requests++
            if (requests == 1) flowOf(listOf(searchResult("item-1"))) else flow { error("search failed") }
        }

        viewModel.updateSearchQuery("first")
        advanceTimeBy(500.milliseconds)
        runCurrent()
        assertTrue(viewModel.isCurrentSearchResult("item-1"))

        viewModel.updateSearchQuery("second")
        advanceTimeBy(500.milliseconds)
        runCurrent()

        assertIs<SearchRequestState.Error>(viewModel.searchRequestState.value)
        assertFalse(viewModel.isCurrentSearchResult("item-1"))
    }

    private fun searchResult(id: String): Search = Search(
        url_hash = id,
        url = "https://example.com/$id",
        title = "Match",
        subtitle = null,
        content = null,
        image_url = null,
        feed_source_id = "source-1",
        is_read = false,
        is_bookmarked = false,
        pub_date = 1000L,
        comments_url = null,
        notification_sent = false,
        is_blocked = false,
        content_fetched = false,
        feed_source_title = "Feed Source",
        feed_source_id_ = "source-1",
        feed_source_url = "https://example.com/feed.xml",
        feed_source_last_sync_timestamp = null,
        feed_source_fetch_failed = false,
        feed_source_category_id = null,
        feed_source_category_title = null,
        feed_source_logo_url = null,
        feed_source_article_open_mode = ArticleOpenMode.DEFAULT,
        feed_source_hide_images = false,
        audio_url = null,
    )

    private fun viewModel(search: (String, FeedFilter?) -> Flow<List<Search>>): SearchViewModel = SearchViewModel(
        feedActionsRepository = get<FeedActionsRepository>(),
        dateFormatter = get<DateFormatter>(),
        feedFontSizeRepository = get<FeedFontSizeRepository>(),
        feedStateRepository = get<FeedStateRepository>(),
        articleNavigationRepository = get<ArticleNavigationRepository>(),
        feedAppearanceSettingsRepository = get<FeedAppearanceSettingsRepository>(),
        searchFeeds = search,
    )
}
