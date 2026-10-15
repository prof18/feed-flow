package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedAddState
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.test.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FeedSuggestionOperationTest : KoinTestBase() {

    @Test
    fun `add immediately publishes adding and duplicate taps share one operation`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        var addCalls = 0
        var refreshCalls = 0
        val viewModel = viewModel(
            add = { _, _ ->
                addCalls++
                gate.await()
            },
            refresh = { refreshCalls++ },
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        runCurrent()
        val adding = viewModel.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.Adding, adding.status)
        assertEquals(FeedAddState.Adding, viewModel.feedStatesMapState.value[feedA.url])

        viewModel.addFeed(feedA, "Tech")
        runCurrent()
        assertEquals(1, addCalls)
        assertEquals(FeedSuggestionOperationStatus.Adding, viewModel.operationStates.value.getValue(feedA.url).status)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, refreshCalls)
        assertEquals(FeedAddState.Added, viewModel.feedStatesMapState.value[feedA.url])
    }

    @Test
    fun `immediate duplicate tap before queued operation does not enqueue another save`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        var addCalls = 0
        val viewModel = viewModel(
            add = { _, _ ->
                addCalls++
                gate.await()
            },
            refresh = {},
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        viewModel.addFeed(feedA, "Tech")
        assertEquals(FeedSuggestionOperationStatus.Adding, viewModel.operationStates.value.getValue(feedA.url).status)
        runCurrent()

        assertEquals(1, addCalls)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(FeedSuggestionOperationStatus.Added, viewModel.operationStates.value.getValue(feedA.url).status)
    }

    @Test
    fun `existing verified url becomes added without add or refresh`() = runTest(testDispatcher) {
        var addCalls = 0
        var refreshCalls = 0
        val viewModel = viewModel(
            add = { _, _ -> addCalls++ },
            refresh = { refreshCalls++ },
            exists = { it == feedA.url },
        )

        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()

        assertEquals(0, addCalls)
        assertEquals(0, refreshCalls)
        assertEquals(FeedAddState.Added, viewModel.feedStatesMapState.value[feedA.url])
        assertEquals(FeedSuggestionOperationStatus.Added, viewModel.operationStates.value.getValue(feedA.url).status)
    }

    @Test
    fun `successful save publishes refreshing and blocks duplicate add`() = runTest(testDispatcher) {
        val refreshGate = CompletableDeferred<Unit>()
        var addCalls = 0
        var refreshCalls = 0
        val viewModel = viewModel(
            add = { _, _ -> addCalls++ },
            refresh = {
                refreshCalls++
                refreshGate.await()
            },
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        runCurrent()
        val refreshing = viewModel.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.Refreshing, refreshing.status)
        assertEquals(FeedAddState.Added, viewModel.feedStatesMapState.value[feedA.url])

        viewModel.addFeed(feedA, "Tech")
        runCurrent()
        assertEquals(1, addCalls)
        assertEquals(1, refreshCalls)
        assertEquals(refreshing, viewModel.operationStates.value.getValue(feedA.url))

        refreshGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(FeedSuggestionOperationStatus.Added, viewModel.operationStates.value.getValue(feedA.url).status)
    }

    @Test
    fun `refresh failure preserves saved feed and reports update failure`() = runTest(testDispatcher) {
        val viewModel = viewModel(
            add = { _, _ -> },
            refresh = { error("refresh failed") },
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()

        assertEquals(FeedAddState.Added, viewModel.feedStatesMapState.value[feedA.url])
        assertEquals(
            FeedSuggestionOperationStatus.UpdateFailed,
            viewModel.operationStates.value.getValue(feedA.url).status,
        )
    }

    @Test
    fun `genuine add failure can be retried and advances generation`() = runTest(testDispatcher) {
        var addCalls = 0
        val viewModel = viewModel(
            add = { _, _ -> if (++addCalls == 1) error("add failed") },
            refresh = {},
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()
        val failed = viewModel.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.AddFailed, failed.status)
        assertEquals(FeedAddState.NotAdded, viewModel.feedStatesMapState.value[feedA.url])

        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()
        val retried = viewModel.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.Added, retried.status)
        assertNotEquals(failed.generation, retried.generation)
        assertEquals(FeedAddState.Added, viewModel.feedStatesMapState.value[feedA.url])
        assertEquals(2, addCalls)
    }

    @Test
    fun `add exception after commit is verified as added and is not duplicated`() = runTest(testDispatcher) {
        var committed = false
        var addCalls = 0
        val viewModel = viewModel(
            add = { _, _ ->
                addCalls++
                committed = true
                error("commit response lost")
            },
            refresh = {},
            exists = { committed },
        )

        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()

        val state = viewModel.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.UpdateFailed, state.status)
        assertEquals(FeedAddState.Added, viewModel.feedStatesMapState.value[feedA.url])
        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()
        assertEquals(1, addCalls)
        assertEquals(state, viewModel.operationStates.value.getValue(feedA.url))
    }

    @Test
    fun `observed saved feed stays added when both save and verification report errors`() = runTest(testDispatcher) {
        val sources = get<FeedSourcesRepository>()
        lateinit var model: FeedSuggestionsViewModel
        model = viewModel(
            add = { feed, category ->
                sources.addFeedSourceWithoutFetching(feed.url, feed.name, category, feed.logoUrl)
                model.feedStatesMapState.first { it[feed.url] == FeedAddState.Added }
                error("backup failed after commit")
            },
            refresh = {},
            exists = { url ->
                if (model.feedStatesMapState.value[url] == FeedAddState.Added) error("verification unavailable")
                false
            },
        )

        model.addFeed(feedA, "Tech")
        advanceUntilIdle()

        assertEquals(FeedAddState.Added, model.feedStatesMapState.value[feedA.url])
        assertEquals(FeedSuggestionOperationStatus.UpdateFailed, model.operationStates.value.getValue(feedA.url).status)
    }

    @Test
    fun `existence check failure reports add failure and retry verifies before writing`() = runTest(testDispatcher) {
        var existsCalls = 0
        var addCalls = 0
        val viewModel = viewModel(
            add = { _, _ -> addCalls++ },
            refresh = {},
            exists = { if (++existsCalls == 1) error("lookup failed") else false },
        )

        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()
        val first = viewModel.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.AddFailed, first.status)
        assertEquals(0, addCalls)

        viewModel.addFeed(feedA, "Tech")
        advanceUntilIdle()
        val retried = viewModel.operationStates.value.getValue(feedA.url)
        assertEquals(FeedSuggestionOperationStatus.Added, retried.status)
        assertNotEquals(first.generation, retried.generation)
        assertEquals(2, existsCalls)
        assertEquals(1, addCalls)
    }

    @Test
    fun `verification read failure reports add failure and retry preflight avoids duplicate write`() =
        runTest(testDispatcher) {
            var existsCalls = 0
            var addCalls = 0
            var committed = false
            val viewModel = viewModel(
                add = { _, _ ->
                    addCalls++
                    committed = true
                    error("commit response lost")
                },
                refresh = {},
                exists = {
                    when (++existsCalls) {
                        1 -> false
                        2 -> error("verification read failed")
                        else -> committed
                    }
                },
            )

            viewModel.addFeed(feedA, "Tech")
            advanceUntilIdle()
            val failed = viewModel.operationStates.value.getValue(feedA.url)
            assertEquals(FeedSuggestionOperationStatus.AddFailed, failed.status)
            assertEquals(FeedAddState.NotAdded, viewModel.feedStatesMapState.value[feedA.url])

            viewModel.addFeed(feedA, "Tech")
            advanceUntilIdle()

            val retried = viewModel.operationStates.value.getValue(feedA.url)
            assertEquals(FeedSuggestionOperationStatus.Added, retried.status)
            assertNotEquals(failed.generation, retried.generation)
            assertEquals(3, existsCalls)
            assertEquals(1, addCalls)
        }

    @Test
    fun `per url operation state is independent`() = runTest(testDispatcher) {
        val gates = mapOf(feedA.url to CompletableDeferred<Unit>(), feedB.url to CompletableDeferred<Unit>())
        val viewModel = viewModel(
            add = { feed, _ -> gates.getValue(feed.url).await() },
            refresh = {},
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        viewModel.addFeed(feedB, "Tech")
        runCurrent()
        assertEquals(FeedSuggestionOperationStatus.Adding, viewModel.operationStates.value.getValue(feedA.url).status)
        assertEquals(FeedSuggestionOperationStatus.Adding, viewModel.operationStates.value.getValue(feedB.url).status)

        gates.getValue(feedA.url).complete(Unit)
        runCurrent()
        assertEquals(FeedSuggestionOperationStatus.Added, viewModel.operationStates.value.getValue(feedA.url).status)
        assertEquals(FeedSuggestionOperationStatus.Adding, viewModel.operationStates.value.getValue(feedB.url).status)

        gates.getValue(feedB.url).complete(Unit)
        advanceUntilIdle()
        assertEquals(FeedSuggestionOperationStatus.Added, viewModel.operationStates.value.getValue(feedB.url).status)
    }

    @Test
    fun `cancellation is not published as add failure and does not refresh`() = runTest(testDispatcher) {
        var refreshCalls = 0
        val viewModel = viewModel(
            add = { _, _ -> throw CancellationException("cancelled") },
            refresh = { refreshCalls++ },
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        runCurrent()

        assertEquals(0, refreshCalls)
        assertFalse(
            viewModel.operationStates.value.getValue(feedA.url).status == FeedSuggestionOperationStatus.AddFailed,
        )
        assertTrue(viewModel.operationStates.value.getValue(feedA.url).status == FeedSuggestionOperationStatus.Adding)
    }

    @Test
    fun `cancellation during refresh preserves added state without update failure`() = runTest(testDispatcher) {
        var refreshCalls = 0
        val viewModel = viewModel(
            add = { _, _ -> },
            refresh = {
                refreshCalls++
                throw CancellationException("refresh cancelled")
            },
            exists = { false },
        )

        viewModel.addFeed(feedA, "Tech")
        runCurrent()

        assertEquals(1, refreshCalls)
        assertEquals(FeedAddState.Added, viewModel.feedStatesMapState.value[feedA.url])
        assertEquals(
            FeedSuggestionOperationStatus.Refreshing,
            viewModel.operationStates.value.getValue(feedA.url).status,
        )
    }

    private fun viewModel(
        add: suspend (SuggestedFeed, FeedSourceCategory) -> Unit,
        refresh: suspend () -> Unit,
        exists: suspend (String) -> Boolean,
    ): FeedSuggestionsViewModel = FeedSuggestionsViewModel(
        feedSourcesRepository = get<FeedSourcesRepository>(),
        feedFetcherRepository = get<FeedFetcherRepository>(),
        addSuggestedFeed = add,
        refreshFeeds = refresh,
        feedExists = exists,
    )

    private companion object {
        val feedA = SuggestedFeed(name = "Feed A", url = "https://a.example/feed.xml", description = "A")
        val feedB = SuggestedFeed(name = "Feed B", url = "https://b.example/feed.xml", description = "B")
    }
}
