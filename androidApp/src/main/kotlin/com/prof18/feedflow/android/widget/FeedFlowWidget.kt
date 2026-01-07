package com.prof18.feedflow.android.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

internal class FeedFlowWidget(
    private val repository: FeedWidgetRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val browserManager: BrowserManager,
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val lyricist = rememberFeedFlowStrings()

            ProvideFeedFlowStrings(lyricist) {
                val feedItems by repository.getFeeds().collectAsState(persistentListOf())
                val feedLayout by widgetSettingsRepository.feedWidgetLayout.collectAsState()
                val showHeader by widgetSettingsRepository.widgetShowHeader.collectAsState()
                val fontScale by widgetSettingsRepository.widgetFontScale.collectAsState()
                val backgroundColor by widgetSettingsRepository.widgetBackgroundColor.collectAsState()
                val backgroundOpacity by widgetSettingsRepository.widgetBackgroundOpacity.collectAsState()

                GlanceTheme {
                    WidgetContent(
                        feedItems = feedItems,
                        feedLayout = feedLayout,
                        browserManager = browserManager,
                        showHeader = showHeader,
                        fontScale = fontScale,
                        backgroundColor = backgroundColor,
                        backgroundOpacityPercent = backgroundOpacity,
                    )
                }
            }
        }
    }
}
