package com.prof18.feedflow.android.widget

import com.prof18.feedflow.shared.domain.feed.MAX_WIDGET_FEED_ITEMS
import kotlin.math.sqrt

private const val REMOTE_VIEWS_BYTES_PER_PIXEL_WITH_HEADROOM = 6L
private const val ARTICLE_BUDGET_NUMERATOR = 3L
private const val ARTICLE_BUDGET_DENOMINATOR = 4L
private const val MAX_ARTICLE_BUDGET_BYTES = 6L * 1024L * 1024L
private const val ARGB_8888_BYTES_PER_PIXEL = 4.0
private const val MAX_IMAGE_EDGE_PX = 512

internal fun resolveWidgetImageBudget(
    screenWidthPx: Int,
    screenHeightPx: Int,
    exactSizes: WidgetExactSizeResolution,
): WidgetImageBudgetPolicy {
    require(exactSizes.payloadVariantCount > 0)

    val screenPixelCount = saturatedMultiply(
        screenWidthPx.coerceAtLeast(0).toLong(),
        screenHeightPx.coerceAtLeast(0).toLong(),
    )
    val remoteViewsLimitBytes = saturatedMultiply(
        screenPixelCount,
        REMOTE_VIEWS_BYTES_PER_PIXEL_WITH_HEADROOM,
    )
    val deviceArticleBudgetBytes = multiplyDivideFloor(
        value = remoteViewsLimitBytes,
        numerator = ARTICLE_BUDGET_NUMERATOR,
        denominator = ARTICLE_BUDGET_DENOMINATOR,
    )
    val effectiveArticleBudgetBytes = minOf(
        MAX_ARTICLE_BUDGET_BYTES,
        deviceArticleBudgetBytes,
    )
    val payloadCount = saturatedMultiply(
        MAX_WIDGET_FEED_ITEMS.toLong(),
        exactSizes.payloadVariantCount.toLong(),
    )
    val payloadBudgetBytes = effectiveArticleBudgetBytes / payloadCount
    val budgetEdgePx = sqrt(payloadBudgetBytes.toDouble() / ARGB_8888_BYTES_PER_PIXEL).toInt()

    return WidgetImageBudgetPolicy(
        exactSizeKey = exactSizes.stableKey,
        payloadCount = payloadCount,
        remoteViewsLimitBytes = remoteViewsLimitBytes,
        effectiveArticleBudgetBytes = effectiveArticleBudgetBytes,
        payloadBudgetBytes = payloadBudgetBytes,
        budgetEdgePx = budgetEdgePx,
    )
}

private fun saturatedMultiply(first: Long, second: Long): Long {
    if (first == 0L || second == 0L) {
        return 0L
    }
    return if (first > Long.MAX_VALUE / second) Long.MAX_VALUE else first * second
}

private fun multiplyDivideFloor(
    value: Long,
    numerator: Long,
    denominator: Long,
): Long {
    val quotient = value / denominator
    val remainder = value % denominator
    return quotient * numerator + remainder * numerator / denominator
}

internal data class WidgetImageBudgetPolicy(
    val exactSizeKey: String,
    val payloadCount: Long,
    val remoteViewsLimitBytes: Long,
    val effectiveArticleBudgetBytes: Long,
    val payloadBudgetBytes: Long,
    val budgetEdgePx: Int,
) {
    val identity = WidgetImageBudgetIdentity(
        exactSizeKey = exactSizeKey,
        payloadBudgetBytes = payloadBudgetBytes,
    )

    fun resolveRequest(
        imageUrl: String,
        displayTargetPx: Int,
    ): WidgetImageRequestPolicy? {
        val edgePx = minOf(
            displayTargetPx.coerceAtLeast(0),
            MAX_IMAGE_EDGE_PX,
            budgetEdgePx,
        )
        if (edgePx < 1) {
            return null
        }
        return WidgetImageRequestPolicy(
            edgePx = edgePx,
            identity = WidgetImageRequestIdentity(
                imageUrl = imageUrl,
                edgePx = edgePx,
                exactSizeKey = exactSizeKey,
                payloadBudgetBytes = payloadBudgetBytes,
            ),
        )
    }
}

internal data class WidgetImageBudgetIdentity(
    val exactSizeKey: String,
    val payloadBudgetBytes: Long,
)

internal data class WidgetImageRequestPolicy(
    val edgePx: Int,
    val identity: WidgetImageRequestIdentity,
)

internal data class WidgetImageRequestIdentity(
    val imageUrl: String,
    val edgePx: Int,
    val exactSizeKey: String,
    val payloadBudgetBytes: Long,
    val softwareCornerRadiusDp: Int = 0,
    val softwareDisplayViewportDp: Int = 0,
)
