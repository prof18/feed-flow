package com.prof18.feedflow.android.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class FeedFlowWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {

    private val repository by inject<FeedWidgetRepository>()
    private val browserManager by inject<BrowserManager>()

    override val glanceAppWidget: GlanceAppWidget = FeedFlowWidget(repository, browserManager)
}
