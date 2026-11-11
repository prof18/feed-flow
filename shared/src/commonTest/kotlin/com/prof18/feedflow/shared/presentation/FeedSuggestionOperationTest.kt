package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.test.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedSuggestionOperationTest : KoinTestBase() {

    private val deleted = mutableListOf<String>()
    private var backups = 0
    private var refreshes = 0

    @Test
    fun `add shows adding then added with an undo offer naming the category`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = viewModel(add = { feed, _ ->
            gate.await()
            subscribe(feed)
        })

        model.addFeed(feedA, "Tech")
        runCurrent()
        assertEquals(FeedSuggestionOperationStatus.Adding, model.status(feedA))

        gate.complete(Unit)
        runCurrent()
        assertEquals(FeedSuggestionOperationStatus.Added, model.status(feedA))
        assertEquals("Tech", model.operationStates.value.getValue(feedA.url).addedToCategory)
        val offer = assertNotNull(model.undoOfferState.value)
        assertEquals(feedA.name, offer.feedName)
        assertEquals("Tech", offer.categoryName)
    }

    @Test
    fun `taps while adding or after adding are ignored`() = runTest(testDispatcher) {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val model = viewModel(add = { feed, _ ->
            calls++
            gate.await()
            subscribe(feed)
        })

        model.addFeed(feedA, "Tech")
        model.addFeed(feedA, "Tech")
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()
        model.addFeed(feedA, "Tech")
        advanceUntilIdle()

        assertEquals(1, calls)
    }

    @Test
    fun `a failed add shows the failure and can be retried`() = runTest(testDispatcher) {
        var calls = 0
        val model = viewModel(add = { feed, _ ->
            calls++
            if (calls == 1) error("offline")
            subscribe(feed)
        })

        model.addFeed(feedA, "Tech")
        advanceUntilIdle()
        assertEquals(FeedSuggestionOperationStatus.AddFailed, model.status(feedA))

        model.addFeed(feedA, "Tech")
        advanceUntilIdle()
        assertEquals(FeedSuggestionOperationStatus.Added, model.status(feedA))
    }

    @Test
    fun `undo deletes the subscription and the row goes back to add`() = runTest(testDispatcher) {
        val model = viewModel()

        model.addFeed(feedA, "Tech")
        runCurrent()
        model.undoLastAdd(assertNotNull(model.undoOfferState.value).generation)
        advanceUntilIdle()

        assertEquals(listOf(feedA.url), deleted)
        assertNull(model.undoOfferState.value)
        assertNull(model.operationStates.value[feedA.url])
    }

    @Test
    fun `an expired undo offer cannot remove the feed`() = runTest(testDispatcher) {
        val model = viewModel()

        model.addFeed(feedA, "Tech", undoTimeoutMillis = 30_000)
        runCurrent()
        val generation = assertNotNull(model.undoOfferState.value).generation
        advanceTimeBy(29_999)
        assertNotNull(model.undoOfferState.value)
        advanceTimeBy(2)
        model.undoLastAdd(generation)
        advanceUntilIdle()

        assertNull(model.undoOfferState.value)
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun `closing refreshes and backs up once after changes and a pending undo`() = runTest(testDispatcher) {
        val model = viewModel()

        model.endVisit()
        advanceUntilIdle()
        assertEquals(0, backups)
        assertEquals(0, refreshes)

        model.addFeed(feedA, "Tech")
        model.addFeed(feedB, "Tech")
        runCurrent()
        assertEquals(0, backups)

        model.undoLastAdd(assertNotNull(model.undoOfferState.value).generation)
        model.endVisit()
        advanceUntilIdle()
        assertEquals(1, backups)
        assertEquals(1, refreshes)
        assertEquals(listOf(feedB.url), deleted)

        model.endVisit()
        advanceUntilIdle()
        assertEquals(1, backups)
    }

    @Test
    fun `feeds already subscribed show as added and are not added again`() = runTest(testDispatcher) {
        subscribe(feedA)
        var calls = 0
        val model = viewModel(add = { _, _ -> calls++ })
        advanceUntilIdle()

        model.addFeed(feedA, "Tech")
        advanceUntilIdle()

        val row = model.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.Added, row.status)
        assertNull(row.addedToCategory)
        assertNull(model.undoOfferState.value)
        assertEquals(0, calls)
    }

    private fun TestScope.viewModel(
        add: suspend (SuggestedFeed, String) -> Unit = { feed, _ -> subscribe(feed) },
    ): FeedSuggestionsViewModel = FeedSuggestionsViewModel(
        feedSourcesRepository = get(),
        feedFetcherRepository = get(),
        backgroundScope = backgroundScope,
        addSuggestedFeed = add,
        deleteFeed = { source: FeedSource ->
            deleted += source.url
            get<DatabaseHelper>().deleteFeedSource(source.id)
        },
        refreshFeeds = { refreshes++ },
        backUp = { backups++ },
    )

    private suspend fun subscribe(feed: SuggestedFeed) {
        get<DatabaseHelper>().insertFeedSource(
            listOf(
                ParsedFeedSource(
                    id = feed.url.hashCode().toString(),
                    url = feed.url,
                    title = feed.name,
                    category = null,
                    logoUrl = null,
                    websiteUrl = null,
                ),
            ),
        )
    }

    private fun FeedSuggestionsViewModel.status(feed: SuggestedFeed) = operationStates.value[feed.url]?.status

    private companion object {
        val feedA = SuggestedFeed(name = "Feed A", url = "https://a.example/feed.xml", description = "A")
        val feedB = SuggestedFeed(name = "Feed B", url = "https://b.example/feed.xml", description = "B")
    }
}
