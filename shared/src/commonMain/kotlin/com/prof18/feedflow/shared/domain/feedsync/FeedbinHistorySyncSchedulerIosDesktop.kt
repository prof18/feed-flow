package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.onSuccess
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class FeedbinHistorySyncSchedulerIosDesktop(
    private val feedbinRepository: FeedbinRepository,
    private val logger: Logger,
    dispatcherProvider: DispatcherProvider,
) : FeedbinHistorySyncScheduler {
    private var backgroundJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    override fun startInitialSync() {
        if (backgroundJob?.isActive == true) {
            return
        }

        backgroundJob = scope.launch {
            logger.d { "Starting Feedbin history sync" }
            feedbinRepository.syncHistoryFromBackground().onSuccess {
                logger.d { "Feedbin history sync finished successfully" }
            }
        }
    }
}
