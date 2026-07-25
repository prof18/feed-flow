package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class DesktopAutoRefreshScheduler internal constructor(
    private val feedFetcherRepository: FeedFetcherRepository,
    private val feedStateRepository: FeedStateRepository,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
    dispatcherProvider: DispatcherProvider,
    private val refreshActionOverride: (suspend () -> Unit)? = null,
) : BackgroundSyncScheduler {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
    private val scheduleLock = Any()
    private var timerJob: Job? = null
    private var refreshJob: Job? = null
    private var scheduleGeneration = 0L

    fun start() {
        restart(settingsRepository.getSyncPeriod())
    }

    override fun updateSyncPeriod(syncPeriod: SyncPeriod) {
        restart(syncPeriod)
    }

    suspend fun stop() {
        val jobsToStop = synchronized(scheduleLock) {
            scheduleGeneration += 1
            val jobs = timerJob to refreshJob
            timerJob = null
            refreshJob = null
            jobs
        }
        jobsToStop.first?.cancelAndJoin()
        jobsToStop.second?.cancelAndJoin()
    }

    private fun restart(syncPeriod: SyncPeriod) {
        synchronized(scheduleLock) {
            scheduleGeneration += 1
            val generation = scheduleGeneration
            timerJob?.cancel()
            timerJob = null
            if (syncPeriod == SyncPeriod.NEVER) {
                return
            }

            timerJob = scope.launch {
                while (isActive) {
                    delay(syncPeriod.minutes.minutes)
                    synchronized(scheduleLock) {
                        if (generation == scheduleGeneration && refreshJob?.isActive != true) {
                            refreshJob = scope.launch {
                                runRefresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun runRefresh() {
        try {
            logger.d { "Auto-refresh: starting silent feed fetch" }
            val refreshAction = refreshActionOverride
            if (refreshAction != null) {
                refreshAction()
            } else {
                if (feedStateRepository.updateState.value.isLoading()) {
                    return
                }
                feedFetcherRepository.fetchFeeds(publishToFeedList = false)
                feedStateRepository.refreshPendingNewArticlesCount()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.e(exception) { "Auto-refresh: silent feed fetch failed" }
        }
    }
}
