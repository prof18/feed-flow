package com.prof18.feedflow.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.feedflow.FeedRetrieverRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FeedsCleanerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: FeedRetrieverRepository by inject()
    
    override suspend fun doWork(): Result {
       repository.deleteOldFeeds()
        return Result.success()
    }
}