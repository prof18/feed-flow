package com.prof18.feedflow.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.prof18.feedflow.FeedRetrieverRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FeedsCleanerWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: FeedRetrieverRepository by inject()

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun doWork(): Result {
        repository.deleteOldFeeds()
        appContext.imageLoader.diskCache?.clear()
        appContext.imageLoader.memoryCache?.clear()
        return Result.success()
    }
}
