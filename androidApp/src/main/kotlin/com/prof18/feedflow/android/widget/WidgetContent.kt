package com.prof18.feedflow.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.MainActivity
import com.prof18.feedflow.android.widget.components.WidgetFeedItemCard
import com.prof18.feedflow.android.widget.components.WidgetFeedItemList
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList

@SuppressLint("RestrictedApi")
@Composable
internal fun WidgetContent(
    feedItems: ImmutableList<FeedItem>,
    feedLayout: FeedLayout,
    browserManager: BrowserManager,
    showHeader: Boolean,
    fontScale: Int,
    backgroundColor: Int?,
    backgroundOpacityPercent: Int,
    textColorMode: WidgetTextColorMode,
    hideImages: Boolean,
) {
    val context = LocalContext.current
    val openAppAction = createOpenAppAction(context)
    val fontSizes = widgetFontSizes(fontScale)

    @Suppress("MagicNumber")
    val backgroundOpacity = backgroundOpacityPercent.coerceIn(minimumValue = 0, maximumValue = 100) / 100f
    val widgetBackground = resolveWidgetBackgroundColor(
        context = context,
        backgroundColor = backgroundColor,
        backgroundOpacity = backgroundOpacity,
    )
    val textColors = when {
        backgroundColor != null -> {
            val effectiveBackgroundColor = widgetEffectiveBackgroundColor(
                backgroundColor = widgetColorFromArgb(backgroundColor),
                backgroundOpacity = backgroundOpacity,
                underlayColor = GlanceTheme.colors.widgetBackground.getColor(context),
            )
            widgetTextColorsForMode(textColorMode, effectiveBackgroundColor)
        }
        textColorMode != WidgetTextColorMode.AUTOMATIC -> {
            widgetTextColorsForMode(
                textColorMode = textColorMode,
                backgroundColor = GlanceTheme.colors.widgetBackground.getColor(context),
            )
        }
        else -> null
    }
    val primaryTextColor = textColors?.primary?.let(::ColorProvider) ?: GlanceTheme.colors.onSurface
    val secondaryTextColor = textColors?.secondary?.let(::ColorProvider) ?: GlanceTheme.colors.onSurface

    Scaffold(
        titleBar = if (showHeader) {
            {
                Text(
                    modifier = GlanceModifier
                        .padding(top = Spacing.regular)
                        .padding(bottom = Spacing.small)
                        .padding(horizontal = Spacing.medium)
                        .fillMaxWidth()
                        .clickable(openAppAction),
                    text = LocalFeedFlowStrings.current.widgetLatestItems,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSizes.header.sp,
                        color = primaryTextColor,
                    ),
                )
            }
        } else {
            null
        },
        backgroundColor = widgetBackground,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        if (feedItems.isEmpty()) {
            val emptyStateModifier = if (showHeader) {
                GlanceModifier
            } else {
                GlanceModifier
                    .fillMaxSize()
                    .clickable(openAppAction)
            }
            Column(
                modifier = emptyStateModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = LocalFeedFlowStrings.current.emptyFeedMessage,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = fontSizes.empty.sp,
                        color = primaryTextColor,
                    ),
                )

                Text(
                    modifier = GlanceModifier.padding(top = Spacing.small),
                    text = LocalFeedFlowStrings.current.widgetCheckFeedSources,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = fontSizes.empty.sp,
                        color = secondaryTextColor,
                    ),
                )
            }
        } else {
            LazyColumn {
                if (!showHeader) {
                    item { Spacer(modifier = GlanceModifier.height(Spacing.small)) }
                }

                items(feedItems) { feedItem ->
                    when (feedLayout) {
                        FeedLayout.LIST -> WidgetFeedItemList(
                            feedItem = feedItem,
                            browserManager = browserManager,
                            fontSizes = fontSizes,
                            hideImages = hideImages,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                        )
                        FeedLayout.CARD -> WidgetFeedItemCard(feedItem, browserManager, fontSizes, hideImages)
                    }
                }

                item { Spacer(modifier = GlanceModifier.height(Spacing.small)) }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun resolveWidgetBackgroundColor(
    context: Context,
    backgroundColor: Int?,
    backgroundOpacity: Float,
): ColorProvider {
    if (backgroundColor != null) {
        return ColorProvider(widgetColorFromArgb(backgroundColor).copy(alpha = backgroundOpacity))
    }

    if (backgroundOpacity < 1f) {
        val themedColor = GlanceTheme.colors.widgetBackground.getColor(context)
        return ColorProvider(themedColor.copy(alpha = backgroundOpacity))
    }

    return GlanceTheme.colors.widgetBackground
}

private fun createOpenAppAction(context: Context): Action {
    return actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
    )
}
