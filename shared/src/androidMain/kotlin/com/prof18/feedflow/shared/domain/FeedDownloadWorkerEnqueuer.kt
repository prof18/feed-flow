package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import java.util.concurrent.TimeUnit.MINUTES

class FeedDownloadWorkerEnqueuer internal constructor(
    private val settingsRepository: SettingsRepository,
    private val context: Context,
) {
    fun enqueueWork() {
        updateWorker(settingsRepository.getSyncPeriod())
    }

    fun updateWorker(syncPeriod: SyncPeriod) =
        when (syncPeriod) {
            SyncPeriod.NEVER -> cancel()
            SyncPeriod.FIFTEEN_MINUTES,
            SyncPeriod.THIRTY_MINUTES,
            SyncPeriod.ONE_HOUR,
            SyncPeriod.TWO_HOURS,
            SyncPeriod.SIX_HOURS,
            SyncPeriod.TWELVE_HOURS,
            SyncPeriod.ONE_DAY,
            -> enqueue(syncPeriod.minutes)
        }

    /**
     * Manually trigger a one-time feed download for testing notifications
     */
    fun triggerManualSync() {
        val workRequest = OneTimeWorkRequestBuilder<FeedDownloadWorker>()
            .addTag(WORKER_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORKER_TAG)
    }

    private fun enqueue(minutes: Long) {
        val instructions = PeriodicWorkRequestBuilder<FeedDownloadWorker>(minutes, MINUTES)
            .addTag(WORKER_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORKER_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                instructions,
            )
    }

    private companion object {
        const val WORKER_TAG = "FeedDownloadWorker"
    }
}
