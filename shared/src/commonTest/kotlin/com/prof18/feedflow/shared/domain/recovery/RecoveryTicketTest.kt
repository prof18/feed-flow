package com.prof18.feedflow.shared.domain.recovery

import co.touchlab.stately.concurrency.AtomicBoolean
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class RecoveryTicketTest : KoinTestBase() {

    private val settingsRepository: SettingsRepository by inject()

    private fun ticket() = RecoveryTicket(settingsRepository.cloudSyncSessionState.value, settingsRepository)

    @Test
    fun `commit runs the action while current`() {
        var ran = false

        ticket().commit { ran = true }

        assertTrue(ran)
    }

    @Test
    fun `commit after revoke throws and skips the action`() {
        val ticket = ticket()
        var ran = false
        ticket.revoke()

        assertFailsWith<StaleRecoveryException> { ticket.commit { ran = true } }
        assertFalse(ran)
        assertFalse(ticket.isValid)
    }

    @Test
    fun `commit after account rotation throws and ticket is invalid`() {
        val ticket = ticket()
        var ran = false
        settingsRepository.rotateCloudSyncSession()

        assertFailsWith<StaleRecoveryException> { ticket.commit { ran = true } }
        assertFalse(ran)
        assertFalse(ticket.isValid)
    }

    @Test
    fun `guard is cancelled when the account rotates`() = runTest(testDispatcher) {
        val ticket = ticket()
        val guarded = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { ticket.guard { awaitCancellation() } }
        }

        settingsRepository.rotateCloudSyncSession()
        runCurrent()

        assertTrue(guarded.isCompleted)
        assertTrue(guarded.await().exceptionOrNull() is CancellationException)
    }

    @Test
    fun `guard is cancelled when the ticket is revoked`() = runTest(testDispatcher) {
        val ticket = ticket()
        val guarded = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { ticket.guard { awaitCancellation() } }
        }

        ticket.revoke()
        runCurrent()

        assertTrue(guarded.isCompleted)
        assertTrue(guarded.await().exceptionOrNull() is CancellationException)
    }

    @Test
    fun `guard on a revoked ticket never runs the block`() = runTest(testDispatcher) {
        val ticket = ticket()
        ticket.revoke()
        var runs = 0

        val result = runCatching { ticket.guard { runs++ } }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(0, runs)
    }

    @Test
    fun `revoke returns while another thread is inside commit`() = runTest(testDispatcher) {
        val ticket = ticket()
        val entered = AtomicBoolean(false)
        val released = AtomicBoolean(false)
        val timedOut = AtomicBoolean(false)

        withContext(Dispatchers.Default) {
            val committing = async {
                ticket.commit {
                    entered.value = true
                    spinUntil { released.value }.also { if (!it) timedOut.value = true }
                }
            }
            check(spinUntil { entered.value })

            ticket.revoke()
            released.value = true
            committing.await()
        }

        assertFalse(timedOut.value, "revoke() blocked until the commit action finished")
        assertFalse(ticket.isValid)
    }

    private fun spinUntil(condition: () -> Boolean): Boolean {
        val deadline = TimeSource.Monotonic.markNow() + 10.seconds
        while (!condition()) {
            if (deadline.hasPassedNow()) return false
        }
        return true
    }
}
