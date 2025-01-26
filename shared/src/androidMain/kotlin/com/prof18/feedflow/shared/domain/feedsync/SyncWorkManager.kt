package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.feedflow.core.model.SyncResult

class SyncWorkManager internal constructor(
    private val syncWorker: FeedSyncAndroidWorker,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val result = syncWorker.performUpload()
        return if (result is SyncResult.Success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
