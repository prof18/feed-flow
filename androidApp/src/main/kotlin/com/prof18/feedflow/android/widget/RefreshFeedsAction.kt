package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.action.ActionCallback
import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal class RefreshFeedsAction : ActionCallback, KoinComponent {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runWidgetRefreshAction(
            enqueueRefresh = { get<FeedDownloadWorkerEnqueuer>().enqueueWidgetRefresh() },
            updateButton = { isRefreshing -> updateButton(context, glanceId, isRefreshing) },
            updateWidget = { get<WidgetUpdater>().update() },
            reportFailure = { exception -> get<Logger>().e(exception) { "Unable to enqueue widget refresh" } },
        )
    }

    private fun updateButton(context: Context, glanceId: GlanceId, isRefreshing: Boolean) {
        if (glanceId is AppWidgetId) {
            AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(
                glanceId.appWidgetId,
                widgetRefreshButtonFeedback(context, isRefreshing),
            )
        }
    }
}

internal suspend fun runWidgetRefreshAction(
    enqueueRefresh: suspend () -> Unit,
    updateButton: (Boolean) -> Unit,
    updateWidget: suspend () -> Unit,
    reportFailure: (Exception) -> Unit,
) {
    updateButton(true)
    try {
        enqueueRefresh()
    } catch (exception: CancellationException) {
        updateButton(false)
        throw exception
    } catch (exception: Exception) {
        updateButton(false)
        reportFailure(exception)
        return
    }
    updateWidget()
}
