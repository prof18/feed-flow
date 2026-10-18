package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedRecoveryCoordinatorTest : KoinTestBase() {

    private val recoveryRepository: FeedRecoveryRepository by inject()

    private fun ticket() = recoveryRepository.issueTicket()

    @Test
    fun `same origin waiters share the visible session and consume independently`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val firstUrl = "https://example.test/feed-one.xml"
        val secondUrl = "https://example.test/feed-two.xml"
        val firstTicket = ticket()
        val secondTicket = ticket()
        val profile = profile()
        var commits = 0
        val first = async {
            coordinator.verify(firstUrl, VerificationPurpose.Feed, firstTicket) { _, _ ->
                firstTicket.commit { commits++ }
                "first"
            }
        }
        runCurrent()
        val firstRequest = assertNotNull(coordinator.visibleState.value)
        val second = async {
            coordinator.verify(secondUrl, VerificationPurpose.Feed, secondTicket) { _, _ ->
                secondTicket.commit { commits++ }
                "second"
            }
        }
        runCurrent()

        assertEquals(firstRequest.sessionId, coordinator.visibleState.value?.sessionId)
        assertTrue(coordinator.startRead(firstRequest.id))
        coordinator.captured(firstRequest.id, response(firstUrl), profile)
        runCurrent()
        assertEquals("first", first.await())

        val secondRequest = assertNotNull(coordinator.visibleState.value)
        assertEquals(firstRequest.sessionId, secondRequest.sessionId)
        assertTrue(firstRequest.id != secondRequest.id)
        assertTrue(coordinator.startRead(secondRequest.id))
        coordinator.captured(secondRequest.id, response(secondUrl), profile)

        assertEquals("second", second.await())
        assertEquals(2, commits)
    }

    @Test
    fun `a guarded verification returns the consumed value`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val ticket = ticket()
        val result = async {
            ticket.guard {
                coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket) { _, _ -> "saved" }
            }
        }
        runCurrent()
        val request = assertNotNull(coordinator.visibleState.value)
        assertTrue(coordinator.startRead(request.id))
        coordinator.captured(request.id, response(request.url), profile())

        assertEquals("saved", result.await())
    }

    @Test
    fun `different origin is busy while another verification is visible`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val first = launch {
            coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket()) { _, _ -> Unit }
        }
        runCurrent()

        val error = assertFailsWith<WebsiteVerificationFailure> {
            coordinator.verify("https://other.test/feed.xml", VerificationPurpose.Feed, ticket()) { _, _ -> Unit }
        }

        assertEquals(VerificationFailure.Busy, error.reason)
        coordinator.cancelAll()
        first.join()
        assertTrue(first.isCancelled)
    }

    @Test
    fun `cancel before capture prevents consume and revokes the ticket`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val ticket = ticket()
        var consumed = false
        val verification = launch {
            coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket) { _, _ -> consumed = true }
        }
        runCurrent()

        coordinator.cancelAll()
        verification.join()

        assertFalse(consumed)
        assertFalse(ticket.isValid)
        assertTrue(verification.isCancelled)
    }

    @Test
    fun `browser stop after capture revokes the ticket before consume can commit`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val ticket = ticket()
        val enteredConsume = CompletableDeferred<Unit>()
        val releaseConsume = CompletableDeferred<Unit>()
        var committed = false
        val verification = launch {
            coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket) { _, _ ->
                enteredConsume.complete(Unit)
                releaseConsume.await()
                ticket.commit { committed = true }
            }
        }
        runCurrent()
        val request = assertNotNull(coordinator.visibleState.value)
        assertTrue(coordinator.startRead(request.id))
        coordinator.captured(request.id, response(request.url), profile())
        runCurrent()
        enteredConsume.await()

        coordinator.failed(request.id, VerificationFailure.BrowserStopped)
        verification.join()

        assertFalse(committed)
        assertTrue(verification.isCancelled)
    }

    @Test
    fun `stale request ids cannot start fail or capture the visible request`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val verification = launch {
            coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket()) { _, _ -> Unit }
        }
        runCurrent()
        val request = assertNotNull(coordinator.visibleState.value)

        assertFalse(coordinator.startRead("stale-request"))
        coordinator.failed("stale-request", VerificationFailure.AccessDenied)
        coordinator.captured("stale-request", response(request.url), profile())

        assertEquals(request, coordinator.visibleState.value)
        coordinator.cancelAll()
        verification.join()
    }

    @Test
    fun `consume failure revokes its ticket`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val ticket = ticket()
        var committed = false
        val verification = async {
            runCatching {
                coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket) { _, _ ->
                    error("Reader renderer failed")
                }
            }.exceptionOrNull()
        }
        runCurrent()
        val request = assertNotNull(coordinator.visibleState.value)
        assertTrue(coordinator.startRead(request.id))
        coordinator.captured(request.id, response(request.url), profile())
        runCurrent()

        assertIs<IllegalStateException>(verification.await())
        assertFailsWith<StaleRecoveryException> { ticket.commit { committed = true } }
        assertFalse(committed)
    }

    @Test
    fun `disposing the host session revokes a consuming ticket`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val ticket = ticket()
        val consuming = CompletableDeferred<Unit>()
        val verification = async {
            coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket) { _, _ ->
                consuming.complete(Unit)
                awaitCancellation()
            }
        }
        runCurrent()
        val request = assertNotNull(coordinator.visibleState.value)
        assertTrue(coordinator.startRead(request.id))
        coordinator.captured(request.id, response(request.url), profile())
        runCurrent()
        consuming.await()

        coordinator.cancelSession(request.sessionId)
        verification.join()

        assertTrue(verification.isCancelled)
        assertNull(coordinator.visibleState.value)
        assertFailsWith<StaleRecoveryException> { ticket.commit {} }
    }

    @Test
    fun `cancelAll revokes the waiter ticket`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)
        val ticket = ticket()
        val verification = launch {
            coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket) { _, _ -> Unit }
        }
        runCurrent()

        coordinator.cancelAll()
        verification.join()

        assertFalse(ticket.isValid)
    }

    @Test
    fun `unavailable coordinator is unsupported without a visible request`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = false)

        val error = assertFailsWith<WebsiteVerificationFailure> {
            coordinator.verify(FEED_URL, VerificationPurpose.Feed, ticket()) { _, _ -> Unit }
        }

        assertEquals(VerificationFailure.Unsupported, error.reason)
        assertNull(coordinator.visibleState.value)
    }

    @Test
    fun `http url is unsupported`() = runTest(testDispatcher) {
        val coordinator = FeedRecoveryCoordinator(available = true)

        val error = assertFailsWith<WebsiteVerificationFailure> {
            coordinator.verify("http://example.test/feed.xml", VerificationPurpose.Feed, ticket()) { _, _ -> Unit }
        }

        assertEquals(VerificationFailure.Unsupported, error.reason)
        assertNull(coordinator.visibleState.value)
    }

    private fun profile() = FeedRecoveryProfile(
        id = "profile",
        accountEpoch = recoveryRepository.accountEpochState.value,
        origin = ORIGIN,
        userAgent = "test-agent",
        engineVersion = "test-engine",
        readerRetained = false,
        retired = false,
    )

    private fun response(url: String) = BrowserCapture(
        effectiveUrl = url,
        statusCode = 200,
        contentType = "application/rss+xml",
        bytes = byteArrayOf(1, 2, 3),
    )

    private companion object {
        const val FEED_URL = "https://example.test/feed.xml"
        const val ORIGIN = "https://example.test:443"
    }
}
