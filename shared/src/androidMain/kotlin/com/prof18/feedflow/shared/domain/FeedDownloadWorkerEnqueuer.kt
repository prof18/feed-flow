package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import java.util.concurrent.TimeUnit.HOURS

class FeedDownloadWorkerEnqueuer internal constructor(
    private val settingsRepository: SettingsRepository,
    private val context: Context,
) {
    fun enqueueForTheFirstTime() {
        if (settingsRepository.isFirstAppLaunch()) {
            enqueue()
            settingsRepository.setIsFirstAppLaunch(false)
        }
    }

    fun updateWorker(syncPeriod: SyncPeriod) =
        when (syncPeriod) {
            SyncPeriod.NEVER -> cancel()
            SyncPeriod.ONE_HOUR,
            SyncPeriod.TWO_HOURS,
            SyncPeriod.SIX_HOURS,
            SyncPeriod.TWELVE_HOURS,
            -> enqueue(syncPeriod.hours)
        }

    private fun cancel() {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORKER_TAG)
    }

    private fun enqueue(hours: Long = 1) {
        val instructions = PeriodicWorkRequestBuilder<FeedDownloadWorker>(hours, HOURS)
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
                ExistingPeriodicWorkPolicy.KEEP,
                instructions,
            )
    }

    private companion object {
        const val WORKER_TAG = "FeedDownloadWorker"
    }
}
