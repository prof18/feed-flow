package com.prof18.feedflow.android.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

internal class FeedFlowWidget(
    private val repository: FeedWidgetRepository,
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val lyricist = rememberFeedFlowStrings()
            ProvideFeedFlowStrings(lyricist) {
                val feedLayout by repository.getFeedLayout().collectAsState()
                val feedItems by repository.getFeeds().collectAsState(persistentListOf())
                GlanceTheme {
                    Content(feedItems, feedLayout)
                }
            }
        }
    }
}
