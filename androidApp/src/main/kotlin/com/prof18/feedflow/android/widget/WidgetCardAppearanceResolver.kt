package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.domain.model.normalized

private const val MAX_PERCENT = 100
private const val PERCENT_DIVISOR = 100f

internal fun resolveWidgetCardAppearance(
    appearance: WidgetCardAppearance,
    textColorMode: WidgetTextColorMode,
    outerSurfaceColor: Color,
    outerSurfaceOpacityPercent: Int,
    themedWidgetUnderlayColor: Color,
    themedCardSurfaceColor: Color,
    themedOnSurfaceColor: Color,
): ResolvedWidgetCardAppearance {
    val normalizedAppearance = appearance.normalized()
    val outerOpacity = outerSurfaceOpacityPercent.coerceIn(0, MAX_PERCENT) / PERCENT_DIVISOR
    val effectiveOuterColor = outerSurfaceColor
        .copy(alpha = outerOpacity)
        .compositeOver(themedWidgetUnderlayColor.copy(alpha = 1f))
    val cardSurfaceColor = normalizedAppearance.surfaceColor
        ?.let(::widgetColorFromArgb)
        ?: themedCardSurfaceColor.copy(alpha = 1f)
    val cardOpacity = normalizedAppearance.surfaceOpacityPercent / PERCENT_DIVISOR
    val slabFillColor = cardSurfaceColor
        .copy(alpha = cardOpacity)
        .takeUnless { cardOpacity == 0f }
    val effectiveCardColor = slabFillColor?.compositeOver(effectiveOuterColor) ?: effectiveOuterColor
    val providerPolicy = resolveWidgetCardColorProviderPolicy(
        appearance = normalizedAppearance,
        textColorMode = textColorMode,
    )
    val textColors = if (providerPolicy.primaryText == WidgetColorProviderSource.THEMED) {
        WidgetTextColors(
            primary = themedOnSurfaceColor,
            secondary = themedOnSurfaceColor,
        )
    } else {
        widgetTextColorsForMode(
            textColorMode = textColorMode,
            backgroundColor = effectiveCardColor,
        )
    }
    val dividerColor = textColors.secondary.copy(
        alpha = normalizedAppearance.dividerOpacityPercent / PERCENT_DIVISOR,
    )

    return ResolvedWidgetCardAppearance(
        slabFillColor = slabFillColor,
        effectiveOuterColor = effectiveOuterColor,
        effectiveCardColor = effectiveCardColor,
        textColors = textColors,
        dividerColor = dividerColor,
    )
}

internal fun resolveWidgetCardColorProviderPolicy(
    appearance: WidgetCardAppearance,
    textColorMode: WidgetTextColorMode,
): WidgetCardColorProviderPolicy {
    val normalizedAppearance = appearance.normalized()
    val surfaceSource = if (
        normalizedAppearance.surfaceColor == null &&
        normalizedAppearance.surfaceOpacityPercent == MAX_PERCENT
    ) {
        WidgetColorProviderSource.THEMED
    } else {
        WidgetColorProviderSource.RESOLVED
    }
    val textSource = if (
        surfaceSource == WidgetColorProviderSource.THEMED &&
        textColorMode == WidgetTextColorMode.AUTOMATIC
    ) {
        WidgetColorProviderSource.THEMED
    } else {
        WidgetColorProviderSource.RESOLVED
    }
    return WidgetCardColorProviderPolicy(
        surface = surfaceSource,
        primaryText = textSource,
        secondaryText = textSource,
        divider = textSource,
    )
}

internal data class WidgetCardColorProviderPolicy(
    val surface: WidgetColorProviderSource,
    val primaryText: WidgetColorProviderSource,
    val secondaryText: WidgetColorProviderSource,
    val divider: WidgetColorProviderSource,
)

internal enum class WidgetColorProviderSource {
    THEMED,
    RESOLVED,
}
