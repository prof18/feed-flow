package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.presentation.WidgetRefreshState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class FeedFlowWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {

    private val repository by inject<FeedWidgetRepository>()
    private val widgetSettingsRepository by inject<WidgetSettingsRepository>()
    private val browserManager by inject<BrowserManager>()
    private val settingsRepository by inject<SettingsRepository>()
    private val widgetRefreshState by inject<WidgetRefreshState>()
    private val feedDownloadWorkerEnqueuer by inject<FeedDownloadWorkerEnqueuer>()

    override val glanceAppWidget: GlanceAppWidget by lazy {
        FeedFlowWidget(
            repository,
            widgetSettingsRepository,
            browserManager,
            settingsRepository,
            widgetRefreshState,
            feedDownloadWorkerEnqueuer,
        )
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        widgetSettingsRepository.restoreWidgetConfigurations(oldWidgetIds, newWidgetIds)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            newWidgetIds.forEach { appWidgetId ->
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                options.putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED, true)
                appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach(widgetSettingsRepository::clearWidgetConfiguration)
    }
}
