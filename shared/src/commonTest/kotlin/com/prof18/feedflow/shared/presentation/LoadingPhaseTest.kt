package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class LoadingPhaseTest {

    @Test
    fun `a fast load goes straight to ready without a spinner`() = runTest {
        val isLoading = MutableStateFlow(true)
        phases(isLoading).test {
            assertEquals(LoadingPhase.Pending, awaitItem())
            advanceTimeBy(100.milliseconds)
            isLoading.value = false
            assertEquals(LoadingPhase.Ready, awaitItem())
            advanceTimeBy(1_000.milliseconds)
            expectNoEvents()
        }
    }

    @Test
    fun `a slow load shows the spinner after the delay`() = runTest {
        val isLoading = MutableStateFlow(true)
        phases(isLoading).test {
            assertEquals(LoadingPhase.Pending, awaitItem())
            advanceTimeBy(299.milliseconds)
            expectNoEvents()
            advanceTimeBy(2.milliseconds)
            assertEquals(LoadingPhase.Spinner, awaitItem())
        }
    }

    @Test
    fun `the spinner stays visible for the minimum time`() = runTest {
        val isLoading = MutableStateFlow(true)
        phases(isLoading).test {
            assertEquals(LoadingPhase.Pending, awaitItem())
            advanceTimeBy(301.milliseconds)
            assertEquals(LoadingPhase.Spinner, awaitItem())

            advanceTimeBy(100.milliseconds)
            isLoading.value = false
            runCurrent()
            expectNoEvents()

            advanceTimeBy(399.milliseconds)
            expectNoEvents()
            advanceTimeBy(2.milliseconds)
            assertEquals(LoadingPhase.Ready, awaitItem())
        }
    }

    @Test
    fun `the spinner hides right away when it was already visible long enough`() = runTest {
        val isLoading = MutableStateFlow(true)
        phases(isLoading).test {
            assertEquals(LoadingPhase.Pending, awaitItem())
            advanceTimeBy(301.milliseconds)
            assertEquals(LoadingPhase.Spinner, awaitItem())

            advanceTimeBy(600.milliseconds)
            isLoading.value = false
            assertEquals(LoadingPhase.Ready, awaitItem())
        }
    }

    @Test
    fun `loading again while the spinner is held keeps the spinner`() = runTest {
        val isLoading = MutableStateFlow(true)
        phases(isLoading).test {
            assertEquals(LoadingPhase.Pending, awaitItem())
            advanceTimeBy(301.milliseconds)
            assertEquals(LoadingPhase.Spinner, awaitItem())

            isLoading.value = false
            runCurrent()
            isLoading.value = true
            assertEquals(LoadingPhase.Spinner, awaitItem())
            advanceTimeBy(1_000.milliseconds)
            expectNoEvents()
        }
    }

    @Test
    fun `content that is already loaded is ready immediately`() = runTest {
        phases(MutableStateFlow(false)).test {
            assertEquals(LoadingPhase.Ready, awaitItem())
        }
    }

    private fun TestScope.phases(isLoading: MutableStateFlow<Boolean>) =
        isLoading.toLoadingPhase(timeSource = testScheduler.timeSource)
}
