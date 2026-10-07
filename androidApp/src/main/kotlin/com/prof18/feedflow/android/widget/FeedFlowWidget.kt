package com.prof18.feedflow.android.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.util.rememberAndroidFeedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetConfiguration
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.feed.WidgetRenderState
import com.prof18.feedflow.shared.presentation.WidgetRefreshState
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal class FeedFlowWidget(
    private val repository: FeedWidgetRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val browserManager: BrowserManager,
    private val settingsRepository: SettingsRepository,
    private val widgetRefreshState: WidgetRefreshState,
) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val widgetStateFlow = widgetSettingsRepository.observeWidgetConfiguration(appWidgetId)
            .flatMapLatest { config ->
                repository.getWidgetRenderState(config.contentFilter).map { renderState ->
                    WidgetContentState(config, renderState)
                }
            }
        // Glance rebuilds can briefly render the collectAsState initial value before the DB flow emits.
        // Preloading the current items avoids flashing the widget empty state during refreshes.
        val initialWidgetState = widgetStateFlow.first()

        provideContent {
            val lyricist = rememberAndroidFeedFlowStrings(settingsRepository)

            ProvideFeedFlowStrings(lyricist) {
                val widgetState by widgetStateFlow.collectAsState(initialWidgetState)
                val isRefreshing by widgetRefreshState.isRefreshing.collectAsState()
                GlanceTheme {
                    WidgetContent(
                        feedItems = widgetState.renderState.feedItems,
                        feedLayout = widgetState.configuration.feedLayout,
                        browserManager = browserManager,
                        showHeader = widgetState.configuration.showHeader,
                        showRefreshButton = widgetState.configuration.showRefreshButton,
                        isRefreshing = isRefreshing,
                        headerTitle = widgetState.renderState.title,
                        filter = widgetState.renderState.contentFilter,
                        fontScale = widgetState.configuration.fontScale,
                        backgroundColor = widgetState.configuration.backgroundColor,
                        backgroundOpacityPercent = widgetState.configuration.backgroundOpacityPercent,
                        textColorMode = widgetState.configuration.textColorMode,
                        hideImages = widgetState.configuration.hideImages,
                    )
                }
            }
        }
    }
}

private data class WidgetContentState(
    val configuration: WidgetConfiguration,
    val renderState: WidgetRenderState,
)
