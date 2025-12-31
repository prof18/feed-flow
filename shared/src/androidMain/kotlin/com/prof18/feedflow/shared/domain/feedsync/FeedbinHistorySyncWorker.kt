package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository

class FeedbinHistorySyncWorker internal constructor(
    private val feedbinRepository: FeedbinRepository,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return feedbinRepository.syncHistoryFromBackground()
            .fold(
                onSuccess = {
                    Result.success()
                },
                onFailure = {
                    Result.failure()
                },
            )
    }
}
