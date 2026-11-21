package com.prof18.feedflow.desktop.window

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MainWindowCloseSequenceTest {

    private val calls = mutableListOf<String>()

    @Test
    fun `the window state is saved before the cleanup, and the app exits last`() = runTest {
        sequence().run()

        assertEquals(listOf("save", "cleanup", "exit"), calls)
    }

    @Test
    fun `a second close request while the first one runs does nothing`() = runTest {
        val cleanupGate = CompletableDeferred<Unit>()
        val closeSequence = sequence(cleanup = { cleanupGate.await() })

        val first = launch(start = CoroutineStart.UNDISPATCHED) { closeSequence.run() }
        closeSequence.run()
        cleanupGate.complete(Unit)
        first.join()

        assertEquals(listOf("save", "cleanup", "exit"), calls)
    }

    @Test
    fun `the app still exits when the cleanup fails`() = runTest {
        val closeSequence = sequence(cleanup = { error("backup failed") })

        assertFailsWith<IllegalStateException> { closeSequence.run() }

        assertEquals(listOf("save", "cleanup", "exit"), calls)
    }

    @Test
    fun `the app still exits when the close is cancelled`() = runTest {
        val closeSequence = sequence(cleanup = { CompletableDeferred<Unit>().await() })

        val job = launch(start = CoroutineStart.UNDISPATCHED) { closeSequence.run() }
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(listOf("save", "cleanup", "exit"), calls)
    }

    private fun sequence(cleanup: suspend () -> Unit = {}) = MainWindowCloseSequence(
        saveWindowState = { calls += "save" },
        cleanup = {
            calls += "cleanup"
            cleanup()
        },
        exit = { calls += "exit" },
    )
}
