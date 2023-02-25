package com.prof18.feedflow.workmanager

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerHandler(
    private val context: Context
) {
    fun enqueueCleanupWork() {
        val saveRequest = PeriodicWorkRequestBuilder<FeedsCleanerWorker>(7, TimeUnit.DAYS)
            .addTag(WORK_TAG)
            // Additional configuration
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "cleaning",
            ExistingPeriodicWorkPolicy.KEEP,
            saveRequest
        )
    }

    private companion object {
        const val WORK_TAG = "feed-item-cleanup-work"
    }
}
