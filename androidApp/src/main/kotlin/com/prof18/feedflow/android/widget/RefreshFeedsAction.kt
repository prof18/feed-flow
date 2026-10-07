package com.prof18.feedflow.android.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

internal class RefreshFeedsAction : ActionCallback, KoinComponent {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        get<FeedDownloadWorkerEnqueuer>().enqueueWidgetRefresh()
    }
}
