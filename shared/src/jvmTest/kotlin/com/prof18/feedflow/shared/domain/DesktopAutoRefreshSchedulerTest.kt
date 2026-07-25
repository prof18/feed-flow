package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class DesktopAutoRefreshSchedulerTest : KoinTestBase() {

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()

    @Test
    fun `never does not schedule and period updates reschedule`() = runTest(testDispatcher) {
        val scheduler = createScheduler()
        settingsRepository.setSyncPeriod(SyncPeriod.NEVER)
        scheduler.start()

        advanceTimeBy(1.minutes.inWholeMilliseconds)
        runCurrent()
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)

        scheduler.updateSyncPeriod(SyncPeriod.FIFTEEN_MINUTES)
        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()
        assertEquals(NoFeedSourcesStatus, feedStateRepository.updateState.value)

        feedStateRepository.emitUpdateStatus(FinishedFeedUpdateStatus)
        scheduler.updateSyncPeriod(SyncPeriod.THIRTY_MINUTES)
        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()
        assertEquals(NoFeedSourcesStatus, feedStateRepository.updateState.value)
        scheduler.stop()
    }

    @Test
    fun `tick is skipped while another refresh is loading`() = runTest(testDispatcher) {
        val scheduler = createScheduler()
        feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)

        scheduler.updateSyncPeriod(SyncPeriod.FIFTEEN_MINUTES)
        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()

        assertEquals(StartedFeedUpdateStatus, feedStateRepository.updateState.value)
        scheduler.stop()
    }

    @Test
    fun `changing period does not cancel active refresh`() = runTest(testDispatcher) {
        val refreshStarted = CompletableDeferred<Unit>()
        val finishRefresh = CompletableDeferred<Unit>()
        var refreshCompleted = false
        val scheduler = createScheduler(refreshActionOverride = {
            refreshStarted.complete(Unit)
            finishRefresh.await()
            refreshCompleted = true
        })
        scheduler.updateSyncPeriod(SyncPeriod.FIFTEEN_MINUTES)
        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()
        refreshStarted.await()

        scheduler.updateSyncPeriod(SyncPeriod.THIRTY_MINUTES)
        runCurrent()

        assertFalse(refreshCompleted)
        finishRefresh.complete(Unit)
        runCurrent()
        assertTrue(refreshCompleted)
        scheduler.stop()
    }

    @Test
    fun `failed refresh does not stop later ticks`() = runTest(testDispatcher) {
        var refreshCount = 0
        val scheduler = createScheduler(refreshActionOverride = {
            refreshCount += 1
            if (refreshCount == 1) {
                error("Transient failure")
            }
        })
        scheduler.updateSyncPeriod(SyncPeriod.FIFTEEN_MINUTES)

        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()
        assertEquals(1, refreshCount)
        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()
        assertEquals(2, refreshCount)
        scheduler.stop()
    }

    @Test
    fun `stop cancels active refresh and future ticks`() = runTest(testDispatcher) {
        val refreshStarted = CompletableDeferred<Unit>()
        var refreshCancelled = false
        var refreshCount = 0
        val scheduler = createScheduler(refreshActionOverride = {
            refreshCount += 1
            refreshStarted.complete(Unit)
            try {
                CompletableDeferred<Unit>().await()
            } finally {
                refreshCancelled = true
            }
        })
        scheduler.updateSyncPeriod(SyncPeriod.FIFTEEN_MINUTES)
        advanceTimeBy(15.minutes.inWholeMilliseconds)
        runCurrent()
        refreshStarted.await()

        scheduler.stop()
        advanceTimeBy(30.minutes.inWholeMilliseconds)
        runCurrent()

        assertTrue(refreshCancelled)
        assertEquals(1, refreshCount)
    }

    @Test
    fun `disabling period prevents a due tick`() = runTest(testDispatcher) {
        var refreshCount = 0
        val scheduler = createScheduler(refreshActionOverride = { refreshCount += 1 })
        scheduler.updateSyncPeriod(SyncPeriod.FIFTEEN_MINUTES)
        advanceTimeBy(15.minutes.inWholeMilliseconds)

        scheduler.updateSyncPeriod(SyncPeriod.NEVER)
        runCurrent()

        assertEquals(0, refreshCount)
        scheduler.stop()
    }

    private fun createScheduler(
        refreshActionOverride: (suspend () -> Unit)? = null,
    ) = DesktopAutoRefreshScheduler(
        feedFetcherRepository = feedFetcherRepository,
        feedStateRepository = feedStateRepository,
        settingsRepository = settingsRepository,
        logger = Logger.withTag("DesktopAutoRefreshSchedulerTest"),
        dispatcherProvider = dispatcherProvider,
        refreshActionOverride = refreshActionOverride,
    )
}
