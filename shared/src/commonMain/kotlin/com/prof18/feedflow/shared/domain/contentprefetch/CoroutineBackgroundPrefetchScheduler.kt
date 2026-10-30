package com.prof18.feedflow.shared.domain.contentprefetch

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

internal class CoroutineBackgroundPrefetchScheduler(
    private val logger: Logger,
    private val contentPrefetcher: ContentPrefetcher,
    dispatcherProvider: DispatcherProvider,
) : BackgroundPrefetchScheduler {

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    private var backgroundJob: Job? = null

    override fun start() {
        if (backgroundJob?.isActive == true) return

        backgroundJob = coroutineScope.launch {
            try {
                contentPrefetcher.prefetchQueuedBatch()
                logger.d { "Background prefetch complete" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Error in background prefetch" }
            }
        }
    }

    override fun pause() {
        backgroundJob?.cancel()
    }

    override suspend fun cancel() {
        backgroundJob?.cancelAndJoin()
    }
}
