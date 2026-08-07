package com.prof18.feedflow.shared.domain.model

private const val DEFAULT_SURFACE_OPACITY_PERCENT = 100
private const val DEFAULT_CORNER_RADIUS_DP = 16
private const val DEFAULT_DIVIDER_OPACITY_PERCENT = 20
private const val MIN_PERCENT = 0
private const val MAX_PERCENT = 100
private const val MIN_CORNER_RADIUS_DP = 0
private const val MAX_CORNER_RADIUS_DP = 32
private const val OPAQUE_ALPHA_MASK = -0x1000000

data class WidgetCardAppearance(
    val surfaceColor: Int? = null,
    val surfaceOpacityPercent: Int = DEFAULT_SURFACE_OPACITY_PERCENT,
    val cornerRadiusDp: Int = DEFAULT_CORNER_RADIUS_DP,
    val itemSeparation: WidgetCardItemSeparation = WidgetCardItemSeparation.SPACING,
    val dividerOpacityPercent: Int = DEFAULT_DIVIDER_OPACITY_PERCENT,
    val imageSizing: WidgetCardImageSizing = WidgetCardImageSizing.THUMBNAIL,
)

enum class WidgetCardItemSeparation {
    SPACING,
    DIVIDER,
    NONE,
}

enum class WidgetCardImageSizing {
    THUMBNAIL,
    FILL_ROW_HEIGHT,
}

fun WidgetCardAppearance.normalized(): WidgetCardAppearance {
    val clampedCornerRadiusDp = cornerRadiusDp.coerceIn(
        minimumValue = MIN_CORNER_RADIUS_DP,
        maximumValue = MAX_CORNER_RADIUS_DP,
    )
    return copy(
        surfaceColor = surfaceColor?.or(OPAQUE_ALPHA_MASK),
        surfaceOpacityPercent = surfaceOpacityPercent.coerceIn(MIN_PERCENT, MAX_PERCENT),
        cornerRadiusDp = clampedCornerRadiusDp + clampedCornerRadiusDp.mod(2),
        dividerOpacityPercent = dividerOpacityPercent.coerceIn(MIN_PERCENT, MAX_PERCENT),
    )
}
