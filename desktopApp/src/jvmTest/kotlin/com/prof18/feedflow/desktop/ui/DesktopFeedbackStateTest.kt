package com.prof18.feedflow.desktop.ui

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopFeedbackStateTest {
    @Test
    fun `messages queue until dismissed or acted on`() = runTest {
        val state = DesktopFeedbackState()
        val first = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("first") }
        val second = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("second", actionLabel = "retry") }
        val visible = assertNotNull(state.currentFeedback)
        assertEquals("first", visible.message)
        assertEquals(DesktopFeedbackDuration.Short, visible.duration)
        assertFalse(first.isCompleted)
        assertFalse(second.isCompleted)
        visible.dismiss()
        assertEquals(DesktopFeedbackResult.Dismissed, first.await())
        testScheduler.runCurrent()
        val next = assertNotNull(state.currentFeedback)
        assertEquals("second", next.message)
        assertEquals("retry", next.actionLabel)
        assertEquals(DesktopFeedbackDuration.Indefinite, next.duration)
        next.performAction()
        assertEquals(DesktopFeedbackResult.ActionPerformed, second.await())
        assertNull(state.currentFeedback)
    }

    @Test
    fun `canceling visible feedback releases the next queued message`() = runTest {
        val state = DesktopFeedbackState()
        val first = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("first") }
        val second = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("second") }
        assertEquals("first", assertNotNull(state.currentFeedback).message)
        first.cancel()
        first.join()
        testScheduler.runCurrent()
        val next = assertNotNull(state.currentFeedback)
        assertEquals("second", next.message)
        next.dismiss()
        second.await()
        assertNull(state.currentFeedback)
    }

    @Test
    fun `canceled queued feedback never replaces the visible message`() = runTest {
        val state = DesktopFeedbackState()
        val first = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("first") }
        val second = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("second") }
        assertNotNull(state.currentFeedback)
        second.cancel()
        second.join()
        assertEquals("first", assertNotNull(state.currentFeedback).message)
        state.currentFeedback?.dismiss()
        first.await()
        testScheduler.runCurrent()
        assertNull(state.currentFeedback)
    }

    @Test
    fun `stale and repeated completion cannot dismiss the next message`() = runTest {
        val state = DesktopFeedbackState()
        val first = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("first", actionLabel = "retry") }
        val second = async(start = CoroutineStart.UNDISPATCHED) { state.showFeedback("second") }
        val stale = assertNotNull(state.currentFeedback)
        stale.performAction()
        stale.dismiss()
        assertEquals(DesktopFeedbackResult.ActionPerformed, first.await())
        testScheduler.runCurrent()
        stale.dismiss()
        assertEquals("second", assertNotNull(state.currentFeedback).message)
        state.currentFeedback?.dismiss()
        second.await()
        assertNull(state.currentFeedback)
    }
}
