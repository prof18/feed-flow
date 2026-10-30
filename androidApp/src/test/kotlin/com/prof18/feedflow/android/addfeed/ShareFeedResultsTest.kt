package com.prof18.feedflow.android.addfeed

import com.prof18.feedflow.shared.domain.model.FeedAddedState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareFeedResultsTest {

    @Test
    fun `immediate duplicate result is observed after share request starts`() = runTest {
        val results = MutableSharedFlow<FeedAddedState>()
        val received = mutableListOf<FeedAddedState>()
        val duplicate = FeedAddedState.FeedAlreadyExists("Existing feed")
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeShareFeedResults(results) {
                backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    results.emit(duplicate)
                }
            }.toList(received)
        }

        runCurrent()
        assertEquals(listOf(duplicate), received)
        observer.cancelAndJoin()
    }

    @Test
    fun `loading and immediate completion preserve their order`() = runTest {
        val results = MutableSharedFlow<FeedAddedState>()
        val received = mutableListOf<FeedAddedState>()
        val complete = FeedAddedState.FeedAdded("New feed")
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeShareFeedResults(results) {
                backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    results.emit(FeedAddedState.Loading)
                    results.emit(complete)
                }
            }.toList(received)
        }

        runCurrent()
        assertEquals(listOf(FeedAddedState.Loading, complete), received)
        observer.cancelAndJoin()
    }

    @Test
    fun `owner cancellation releases the collector and starts only one request`() = runTest {
        val results = MutableSharedFlow<FeedAddedState>()
        val received = mutableListOf<FeedAddedState>()
        var requests = 0
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeShareFeedResults(results) { requests++ }.toList(received)
        }

        assertEquals(1, requests)
        assertEquals(1, results.subscriptionCount.value)
        observer.cancelAndJoin()
        assertEquals(0, results.subscriptionCount.value)
        results.emit(FeedAddedState.FeedAlreadyExists("Late feed"))
        runCurrent()
        assertEquals(emptyList<FeedAddedState>(), received)
        assertEquals(1, requests)
    }
}
