package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.notification.Notifier
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

class FeedDownloadWorker internal constructor(
    private val feedFetcherRepository: FeedFetcherRepository,
    private val widgetUpdater: WidgetUpdater,
    private val databaseHelper: DatabaseHelper,
    private val notifier: Notifier,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            feedFetcherRepository.fetchFeeds(isFetchingFromBackground = true)
            delay(1.minutes)
            val itemsToNotify = databaseHelper.getFeedSourceToNotify()
            notifier.showNewArticlesNotification(itemsToNotify)
            databaseHelper.markFeedItemsAsNotified()
            widgetUpdater.update()
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}
