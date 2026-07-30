package com.prof18.feedflow.android.widget

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LocalAppWidgetOptions
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import kotlinx.coroutines.flow.first

internal class FeedFlowWidget(
    private val repository: FeedWidgetRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val browserManager: BrowserManager,
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val feedItemsFlow = repository.getFeeds()
        // Glance rebuilds can briefly render the collectAsState initial value before the DB flow emits.
        // Preloading the current items avoids flashing the widget empty state during refreshes.
        val initialFeedItems = feedItemsFlow.first()

        provideContent {
            val lyricist = rememberFeedFlowStrings()

            ProvideFeedFlowStrings(lyricist) {
                val feedItems by feedItemsFlow.collectAsState(initialFeedItems)
                val feedLayout by widgetSettingsRepository.feedWidgetLayout.collectAsState()
                val showHeader by widgetSettingsRepository.widgetShowHeader.collectAsState()
                val fontScale by widgetSettingsRepository.widgetFontScale.collectAsState()
                val backgroundColor by widgetSettingsRepository.widgetBackgroundColor.collectAsState()
                val backgroundOpacity by widgetSettingsRepository.widgetBackgroundOpacity.collectAsState()
                val textColorMode by widgetSettingsRepository.widgetTextColorMode.collectAsState()
                val hideImages by widgetSettingsRepository.widgetHideImages.collectAsState()
                val cardAppearance by widgetSettingsRepository.widgetCardAppearance.collectAsState()

                val glanceContext = LocalContext.current
                val currentSize = LocalSize.current
                val appWidgetOptions = Bundle(LocalAppWidgetOptions.current)
                val exactSizes = resolveExactSizes(
                    snapshot = WidgetOptionsSnapshot.fromBundle(appWidgetOptions),
                    currentSize = currentSize,
                    sdkInt = Build.VERSION.SDK_INT,
                )
                val displayMetrics = glanceContext.resources.displayMetrics
                val imageBudgetPolicy = resolveWidgetImageBudget(
                    screenWidthPx = displayMetrics.widthPixels,
                    screenHeightPx = displayMetrics.heightPixels,
                    exactSizes = exactSizes,
                )
                val displayDensity = displayMetrics.density.takeIf { it.isFinite() && it > 0f } ?: 1f
                val systemFontScale = glanceContext.resources.configuration.fontScale
                    .takeIf { it.isFinite() && it > 0f }
                    ?: 1f

                GlanceTheme {
                    WidgetContent(
                        feedItems = feedItems,
                        feedLayout = feedLayout,
                        browserManager = browserManager,
                        showHeader = showHeader,
                        fontScale = fontScale,
                        backgroundColor = backgroundColor,
                        backgroundOpacityPercent = backgroundOpacity,
                        textColorMode = textColorMode,
                        hideImages = hideImages,
                        cardAppearance = cardAppearance,
                        imageBudgetPolicy = imageBudgetPolicy,
                        availableSlabWidthDp = calculateWidgetAvailableSlabWidthDp(
                            widgetWidthDp = currentSize.width.value,
                        ),
                        displayDensity = displayDensity,
                        systemFontScale = systemFontScale,
                    )
                }
            }
        }
    }
}
