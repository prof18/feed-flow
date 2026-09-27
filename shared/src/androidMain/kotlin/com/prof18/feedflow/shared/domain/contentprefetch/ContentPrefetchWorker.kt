package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException

internal class ContentPrefetchWorker(
    private val contentPrefetcher: ContentPrefetcher,
    private val logger: Logger,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        contentPrefetcher.prefetchQueuedBatch()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e(e) { "Error in background prefetch" }
        Result.failure()
    }
}
