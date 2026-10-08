package com.prof18.feedflow.shared.domain.tts

import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderSpeechTextTest {

    @Test
    fun `segments runs extraction on default dispatcher`() = runTest {
        val defaultDispatcher = StandardTestDispatcher(testScheduler)
        val service = ReaderSpeechText(testDispatchers(defaultDispatcher))
        var completed = false

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            assertEquals(listOf("Title", "Body text."), service.segments("Title", "<p>Body text.</p>"))
            completed = true
        }

        assertFalse(completed)
        runCurrent()
        job.join()
        assertTrue(completed)
    }

    @Test
    fun `segments returns empty list for empty input`() = runTest {
        val service = ReaderSpeechText(testDispatchers(StandardTestDispatcher(testScheduler)))
        var result: List<String>? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { result = service.segments(null, "") }

        runCurrent()
        job.join()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `segments propagates cancellation while waiting for default dispatcher`() = runTest {
        val service = ReaderSpeechText(testDispatchers(StandardTestDispatcher(testScheduler)))
        var completed = false
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            service.segments("Title", "<p>Body text.</p>")
            completed = true
        }

        job.cancelAndJoin()
        runCurrent()

        assertFalse(completed)
        assertTrue(job.isCancelled)
    }

    private fun testDispatchers(defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher) =
        object : DispatcherProvider {
            override val main = kotlinx.coroutines.Dispatchers.Unconfined
            override val default = defaultDispatcher
            override val io = defaultDispatcher
        }
}
