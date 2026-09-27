package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger

internal class WorkManagerBackgroundPrefetchScheduler(
    private val appContext: Context,
    private val logger: Logger,
) : BackgroundPrefetchScheduler {

    override fun start() {
        logger.d { "Enqueueing background content prefetch" }

        val workRequest = OneTimeWorkRequestBuilder<ContentPrefetchWorker>()
            .addTag(WORKER_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                WORKER_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
    }

    // Only iOS pauses prefetching when the app moves to the background.
    override fun pause() = Unit

    override suspend fun cancel() {
        WorkManager.getInstance(appContext).cancelUniqueWork(WORKER_TAG)
    }

    private companion object {
        const val WORKER_TAG = "ContentPrefetchWorker"
    }
}
