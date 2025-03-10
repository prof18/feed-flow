package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

class FeedDownloadWorker internal constructor(
    private val feedFetcherRepository: FeedFetcherRepository,
    private val widgetUpdater: WidgetUpdater,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        feedFetcherRepository.fetchFeeds()
        delay(1.minutes)
        widgetUpdater.update()
        return Result.success()
    }
}
