package com.prof18.feedflow.shared.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.notification.Notifier
import com.prof18.feedflow.shared.presentation.WidgetRefreshState
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class FeedDownloadWorker internal constructor(
    private val feedFetcherRepository: FeedFetcherRepository,
    private val widgetUpdater: WidgetUpdater,
    private val databaseHelper: DatabaseHelper,
    private val notifier: Notifier,
    private val appForegroundState: AppForegroundState,
    private val widgetRefreshState: WidgetRefreshState,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val isManualRefresh = inputData.getBoolean(IS_MANUAL_REFRESH_KEY, false)
        if (!isManualRefresh && appForegroundState.isAppInForeground()) {
            return Result.success()
        }
        return try {
            if (isManualRefresh) {
                widgetRefreshState.setRefreshing(true)
                widgetUpdater.update()
            }
            feedFetcherRepository.fetchFeeds(forceRefresh = isManualRefresh)
            if (!isManualRefresh) {
                val itemsToNotify = databaseHelper.getFeedSourceToNotify()
                val hasShownNotifications = notifier.showNewArticlesNotification(itemsToNotify)
                if (hasShownNotifications) {
                    databaseHelper.markFeedItemsAsNotified()
                }
                widgetUpdater.update()
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.failure()
        } finally {
            if (isManualRefresh) {
                withContext(NonCancellable) {
                    widgetRefreshState.setRefreshing(false)
                    widgetUpdater.update()
                }
            }
        }
    }

    companion object {
        const val IS_MANUAL_REFRESH_KEY = "is_manual_refresh"
    }
}
