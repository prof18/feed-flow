package com.prof18.feedflow.shared.domain

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.prof18.feedflow.core.model.BackgroundSyncRestrictions
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit.MINUTES

class FeedDownloadWorkerEnqueuer internal constructor(
    private val settingsRepository: SettingsRepository,
    private val context: Context,
) {
    val widgetRefreshQueued: Flow<Boolean>
        get() = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(WIDGET_REFRESH_WORK_NAME)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
            }
            .distinctUntilChanged()

    fun enqueueWork() {
        updateWorker()
    }

    fun updateWorker() {
        updateWorker(
            syncPeriod = settingsRepository.getSyncPeriod(),
            restrictions = settingsRepository.getBackgroundSyncRestrictions(),
        )
    }

    fun updateWorker(syncPeriod: SyncPeriod) {
        updateWorker(
            syncPeriod = syncPeriod,
            restrictions = settingsRepository.getBackgroundSyncRestrictions(),
        )
    }

    private fun updateWorker(
        syncPeriod: SyncPeriod,
        restrictions: BackgroundSyncRestrictions,
    ) =
        when (syncPeriod) {
            SyncPeriod.NEVER -> cancel()
            SyncPeriod.FIFTEEN_MINUTES,
            SyncPeriod.THIRTY_MINUTES,
            SyncPeriod.ONE_HOUR,
            SyncPeriod.TWO_HOURS,
            SyncPeriod.SIX_HOURS,
            SyncPeriod.TWELVE_HOURS,
            SyncPeriod.ONE_DAY,
            -> enqueue(syncPeriod.minutes, restrictions)
        }

    /**
     * Manually trigger a one-time feed download for testing notifications
     */
    fun triggerManualSync() {
        val workRequest = OneTimeWorkRequestBuilder<FeedDownloadWorker>()
            .addTag(WORKER_TAG)
            .setConstraints(buildConstraints(settingsRepository.getBackgroundSyncRestrictions()))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    suspend fun enqueueWidgetRefresh() {
        val request = OneTimeWorkRequestBuilder<FeedDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(FeedDownloadWorker.IS_MANUAL_REFRESH_KEY to true))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }
            }
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WIDGET_REFRESH_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        ).await()
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORKER_TAG)
    }

    private fun enqueue(
        minutes: Long,
        restrictions: BackgroundSyncRestrictions,
    ) {
        val instructions = PeriodicWorkRequestBuilder<FeedDownloadWorker>(minutes, MINUTES)
            .addTag(WORKER_TAG)
            .setConstraints(buildConstraints(restrictions))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORKER_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                instructions,
            )
    }

    private fun buildConstraints(restrictions: BackgroundSyncRestrictions): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(
                if (restrictions.syncOnlyOnWifi) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                },
            )
            .setRequiresCharging(restrictions.syncOnlyWhenCharging)
            .build()

    private companion object {
        const val WIDGET_REFRESH_WORK_NAME = "FeedFlowWidgetRefresh"
        const val WORKER_TAG = "FeedDownloadWorker"
    }
}
