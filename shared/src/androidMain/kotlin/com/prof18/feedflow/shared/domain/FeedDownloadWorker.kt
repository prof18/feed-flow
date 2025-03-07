package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.coroutines.flow.lastOrNull

class FeedDownloadWorker internal constructor(
    private val feedFetcherRepository: FeedFetcherRepository,
    private val feedStateRepository: FeedStateRepository,
    private val widgetUpdater: WidgetUpdater,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        feedFetcherRepository.fetchFeeds()
        val errors = feedStateRepository.updateState.lastOrNull()
        return if (errors == null) {
            widgetUpdater.update()
            Result.success()
        } else {
            Result.retry()
        }
    }
}
