package com.prof18.feedflow.android.widget

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RefreshFeedsActionTest {
    @Test
    fun `enqueue failure restores idle feedback and is reported without escaping the action`() = runTest {
        val feedback = mutableListOf<Boolean>()
        val failure = IllegalStateException("Enqueue failed")
        val errors = mutableListOf<Exception>()
        var widgetUpdates = 0

        runWidgetRefreshAction(
            enqueueRefresh = { throw failure },
            updateButton = { feedback += it },
            updateWidget = { widgetUpdates++ },
            reportFailure = { errors += it },
        )

        assertEquals(listOf(true, false), feedback)
        assertSame(failure, errors.single())
        assertEquals(0, widgetUpdates)
    }

    @Test
    fun `cancellation restores idle feedback and propagates without reporting an error`() = runTest {
        val feedback = mutableListOf<Boolean>()
        val cancellation = CancellationException("Action cancelled")
        val errors = mutableListOf<Exception>()
        var widgetUpdates = 0

        val thrown = assertFailsWith<CancellationException> {
            runWidgetRefreshAction(
                enqueueRefresh = { throw cancellation },
                updateButton = { feedback += it },
                updateWidget = { widgetUpdates++ },
                reportFailure = { errors += it },
            )
        }

        assertSame(cancellation, thrown)
        assertEquals(listOf(true, false), feedback)
        assertTrue(errors.isEmpty())
        assertEquals(0, widgetUpdates)
    }

    @Test
    fun `successful enqueue shows loading before enqueueing and then renders persisted state`() = runTest {
        val events = mutableListOf<String>()

        runWidgetRefreshAction(
            enqueueRefresh = { events += "enqueue" },
            updateButton = { events += "loading=$it" },
            updateWidget = { events += "render" },
            reportFailure = { events += "error" },
        )

        assertEquals(listOf("loading=true", "enqueue", "render"), events)
    }
}
