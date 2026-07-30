package com.prof18.feedflow.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import com.prof18.feedflow.android.R
import com.prof18.feedflow.android.widget.components.WidgetFeedItemCard
import com.prof18.feedflow.android.widget.components.WidgetFeedItemList
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.domain.model.normalized
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

private const val FULL_OPACITY_PERCENT = 100
private const val FULL_OPACITY_ALPHA = 255

@SuppressLint("RestrictedApi")
@Composable
internal fun WidgetContent(
    feedItems: ImmutableList<FeedItem>,
    feedLayout: WidgetFeedLayout,
    browserManager: BrowserManager,
    showHeader: Boolean,
    fontScale: Int,
    backgroundColor: Int?,
    backgroundOpacityPercent: Int,
    textColorMode: WidgetTextColorMode,
    hideImages: Boolean,
    cardAppearance: WidgetCardAppearance,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    availableSlabWidthDp: Float,
    displayDensity: Float,
    systemFontScale: Float,
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
    val themedWidgetUnderlayColor = GlanceTheme.colors.widgetBackground.getColor(context)
    val normalizedCardAppearance = cardAppearance.normalized()
    val resolvedCardAppearance = resolveWidgetCardAppearance(
        appearance = normalizedCardAppearance,
        textColorMode = textColorMode,
        outerSurfaceColor = backgroundColor?.let(::widgetColorFromArgb) ?: themedWidgetUnderlayColor,
        outerSurfaceOpacityPercent = backgroundOpacityPercent,
        themedWidgetUnderlayColor = themedWidgetUnderlayColor,
        themedCardSurfaceColor = GlanceTheme.colors.secondaryContainer.getColor(context),
        themedOnSurfaceColor = GlanceTheme.colors.onSurface.getColor(context),
    )
    val cardColorProviderPolicy = resolveWidgetCardColorProviderPolicy(
        appearance = normalizedCardAppearance,
        textColorMode = textColorMode,
    )
    val cardSlabFillColor = resolvedCardAppearance.slabFillColor?.let { resolvedColor ->
        if (cardColorProviderPolicy.surface == WidgetColorProviderSource.THEMED) {
            GlanceTheme.colors.secondaryContainer
        } else {
            ColorProvider(resolvedColor)
        }
    }
    val cardPrimaryTextColor = if (
        cardColorProviderPolicy.primaryText == WidgetColorProviderSource.THEMED
    ) {
        GlanceTheme.colors.onSurface
    } else {
        ColorProvider(resolvedCardAppearance.textColors.primary)
    }
    val cardSecondaryTextColor = if (
        cardColorProviderPolicy.secondaryText == WidgetColorProviderSource.THEMED
    ) {
        GlanceTheme.colors.onSurface
    } else {
        ColorProvider(resolvedCardAppearance.textColors.secondary)
    }
    val textColors = when {
        backgroundColor != null -> {
            val effectiveBackgroundColor = widgetEffectiveBackgroundColor(
                backgroundColor = widgetColorFromArgb(backgroundColor),
                backgroundOpacity = backgroundOpacity,
                underlayColor = themedWidgetUnderlayColor,
            )
            widgetTextColorsForMode(textColorMode, effectiveBackgroundColor)
        }
        textColorMode != WidgetTextColorMode.AUTOMATIC -> {
            widgetTextColorsForMode(
                textColorMode = textColorMode,
                backgroundColor = themedWidgetUnderlayColor,
            )
        }
        else -> null
    }
    val primaryTextColor = textColors?.primary?.let(::ColorProvider) ?: GlanceTheme.colors.onSurface
    val secondaryTextColor = textColors?.secondary?.let(::ColorProvider) ?: GlanceTheme.colors.onSurface
    val cardLayout = resolveWidgetCardLayout(
        requestedImageSizing = normalizedCardAppearance.imageSizing,
        availableSlabWidthDp = availableSlabWidthDp,
        fontSizes = fontSizes,
        systemFontScale = systemFontScale,
    )
    val listImageLayout = resolveWidgetListImageLayout(displayDensity = displayDensity)
    val imageDensity = displayDensity.takeIf { it.isFinite() && it > 0f } ?: 1f
    val cardImageTargetPx = (cardLayout.displayTargetDp * imageDensity)
        .roundToInt()
        .coerceAtLeast(1)

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
        horizontalPadding = WIDGET_SCAFFOLD_HORIZONTAL_PADDING_DP.dp,
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
            WidgetFeedItems(
                feedItems = feedItems,
                feedLayout = feedLayout,
                browserManager = browserManager,
                showHeader = showHeader,
                fontSizes = fontSizes,
                hideImages = hideImages,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                cardAppearance = normalizedCardAppearance,
                resolvedCardAppearance = resolvedCardAppearance,
                cardSlabFillColor = cardSlabFillColor,
                cardSlabColorSource = cardColorProviderPolicy.surface,
                cardPrimaryTextColor = cardPrimaryTextColor,
                cardSecondaryTextColor = cardSecondaryTextColor,
                cardDividerColorSource = cardColorProviderPolicy.divider,
                cardLayout = cardLayout,
                imageBudgetPolicy = imageBudgetPolicy,
                listImageLayout = listImageLayout,
                cardImageTargetPx = cardImageTargetPx,
            )
        }
    }
}

@Composable
private fun WidgetFeedItems(
    feedItems: ImmutableList<FeedItem>,
    feedLayout: WidgetFeedLayout,
    browserManager: BrowserManager,
    showHeader: Boolean,
    fontSizes: WidgetFontSizes,
    hideImages: Boolean,
    primaryTextColor: ColorProvider,
    secondaryTextColor: ColorProvider,
    cardAppearance: WidgetCardAppearance,
    resolvedCardAppearance: ResolvedWidgetCardAppearance,
    cardSlabFillColor: ColorProvider?,
    cardSlabColorSource: WidgetColorProviderSource,
    cardPrimaryTextColor: ColorProvider,
    cardSecondaryTextColor: ColorProvider,
    cardDividerColorSource: WidgetColorProviderSource,
    cardLayout: ResolvedWidgetCardLayout,
    imageBudgetPolicy: WidgetImageBudgetPolicy,
    listImageLayout: ResolvedWidgetListImageLayout,
    cardImageTargetPx: Int,
) {
    LazyColumn {
        if (!showHeader) {
            item { Spacer(modifier = GlanceModifier.height(Spacing.small)) }
        }

        items(count = feedItems.size) { index ->
            val feedItem = feedItems[index]
            when (feedLayout) {
                WidgetFeedLayout.LIST -> WidgetFeedItemList(
                    feedItem = feedItem,
                    browserManager = browserManager,
                    fontSizes = fontSizes,
                    hideImages = hideImages,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    imageBudgetPolicy = imageBudgetPolicy,
                    imageLayout = listImageLayout,
                )
                WidgetFeedLayout.CARD -> {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetFeedItemCard(
                            feedItem = feedItem,
                            browserManager = browserManager,
                            fontSizes = fontSizes,
                            hideImages = hideImages,
                            appearance = cardAppearance,
                            slabFillColor = cardSlabFillColor,
                            slabFillColorSource = cardSlabColorSource,
                            resolvedSlabFillColor = resolvedCardAppearance.slabFillColor,
                            primaryTextColor = cardPrimaryTextColor,
                            secondaryTextColor = cardSecondaryTextColor,
                            cardLayout = cardLayout,
                            imageBudgetPolicy = imageBudgetPolicy,
                            imageDisplayTargetPx = cardImageTargetPx,
                        )
                        resolveWidgetCardDividerLayout(
                            itemSeparation = cardAppearance.itemSeparation,
                            itemIndex = index,
                            itemCount = feedItems.size,
                        )?.let { dividerLayout ->
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(horizontal = dividerLayout.horizontalInsetDp.dp),
                            ) {
                                WidgetCardDivider(
                                    colorSource = cardDividerColorSource,
                                    resolvedColor = resolvedCardAppearance.dividerColor,
                                    opacityPercent = cardAppearance.dividerOpacityPercent,
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .height(dividerLayout.thicknessDp.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = GlanceModifier.height(Spacing.small)) }
    }
}

@Composable
private fun WidgetCardDivider(
    colorSource: WidgetColorProviderSource,
    resolvedColor: Color,
    opacityPercent: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    when (colorSource) {
        WidgetColorProviderSource.THEMED -> AndroidRemoteViews(
            remoteViews = createThemedWidgetCardDividerRemoteViews(
                context = LocalContext.current,
                opacityPercent = opacityPercent,
            ),
            modifier = modifier,
        )
        WidgetColorProviderSource.RESOLVED -> Spacer(
            modifier = modifier.background(ColorProvider(resolvedColor)),
        )
    }
}

internal fun createThemedWidgetCardDividerRemoteViews(
    context: Context,
    opacityPercent: Int,
): RemoteViews = RemoteViews(context.packageName, R.layout.widget_card_divider).apply {
    setInt(
        R.id.widget_card_divider,
        "setImageAlpha",
        widgetCardDividerImageAlpha(opacityPercent),
    )
}

internal fun widgetCardDividerImageAlpha(opacityPercent: Int): Int =
    (opacityPercent.coerceIn(0, FULL_OPACITY_PERCENT) * FULL_OPACITY_ALPHA.toFloat() / FULL_OPACITY_PERCENT)
        .roundToInt()

internal fun resolveWidgetCardDividerLayout(
    itemSeparation: WidgetCardItemSeparation,
    itemIndex: Int,
    itemCount: Int,
): WidgetCardDividerLayout? = if (
    itemSeparation == WidgetCardItemSeparation.DIVIDER &&
    itemIndex >= 0 &&
    itemIndex < itemCount - 1
) {
    WidgetCardDividerLayout(
        horizontalInsetDp = 16,
        thicknessDp = 1,
    )
} else {
    null
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

internal data class WidgetCardDividerLayout(
    val horizontalInsetDp: Int,
    val thicknessDp: Int,
)
