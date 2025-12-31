package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

internal class FeedbinHistorySyncSchedulerAndroid(
    private val appContext: Context,
) : FeedbinHistorySyncScheduler {
    override fun startInitialSync() {
        val workRequest = OneTimeWorkRequestBuilder<FeedbinHistorySyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueueUniqueWork(WORKER_TAG, ExistingWorkPolicy.REPLACE, workRequest)
    }

    private companion object {
        const val WORKER_TAG = "FeedbinHistorySyncWorker"
    }
}
