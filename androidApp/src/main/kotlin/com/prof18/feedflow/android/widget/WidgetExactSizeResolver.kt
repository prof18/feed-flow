package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.SizeF
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

private const val ANDROID_12_SDK = 31

internal fun resolveExactSizes(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
    sdkInt: Int,
): WidgetExactSizeResolution {
    val sizes = if (sdkInt >= ANDROID_12_SDK) {
        resolveApi31Sizes(snapshot, currentSize)
    } else {
        resolveLegacySizes(snapshot, currentSize)
    }
    val payloadVariantCount = resolvePayloadVariantCount(snapshot, sdkInt)
    val stableKey = buildStableExactSizeKey(
        snapshot = snapshot,
        currentSize = currentSize,
        sdkBranch = if (sdkInt >= ANDROID_12_SDK) "api31+" else "api26-30",
        resolvedSizes = sizes,
        payloadVariantCount = payloadVariantCount,
    )
    return WidgetExactSizeResolution(
        stableKey = stableKey,
        sizes = sizes,
        payloadVariantCount = payloadVariantCount,
    )
}

private fun resolveApi31Sizes(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
): List<DpSize> {
    val explicitSizes = snapshot.explicitSizes
        .orEmpty()
        .filter(WidgetExactSize::isUsable)
        .distinct()
        .map(WidgetExactSize::toDpSize)
    if (explicitSizes.isNotEmpty()) {
        return explicitSizes
    }

    val portraitSize = legacySize(
        widthDp = snapshot.minWidthDp,
        heightDp = snapshot.maxHeightDp,
    )
    val landscapeSize = legacySize(
        widthDp = snapshot.maxWidthDp,
        heightDp = snapshot.minHeightDp,
    )
    if (portraitSize != null && landscapeSize != null) {
        return listOf(portraitSize, landscapeSize).distinct()
    }

    return listOf(currentSize)
}

private fun resolveLegacySizes(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
): List<DpSize> {
    val candidates = listOfNotNull(
        legacySize(
            widthDp = snapshot.minWidthDp,
            heightDp = snapshot.maxHeightDp,
        ),
        legacySize(
            widthDp = snapshot.maxWidthDp,
            heightDp = snapshot.minHeightDp,
        ),
    ).distinct()
    return candidates.ifEmpty { listOf(currentSize) }
}

private fun resolvePayloadVariantCount(
    snapshot: WidgetOptionsSnapshot,
    sdkInt: Int,
): Int = if (sdkInt >= ANDROID_12_SDK) {
    resolveApi31PayloadVariantCount(snapshot)
} else {
    resolveLegacyPayloadVariantCount(snapshot)
}

private fun resolveApi31PayloadVariantCount(snapshot: WidgetOptionsSnapshot): Int {
    val explicitSizes = snapshot.explicitSizes
    if (!explicitSizes.isNullOrEmpty()) {
        return explicitSizes
            .map(WidgetExactSize::toDpSize)
            .distinct()
            .size
            .coerceAtLeast(1)
    }

    val minWidthDp = snapshot.minWidthDp ?: return 1
    val maxWidthDp = snapshot.maxWidthDp ?: return 1
    val minHeightDp = snapshot.minHeightDp ?: return 1
    val maxHeightDp = snapshot.maxHeightDp ?: return 1
    if (minWidthDp == 0 || maxWidthDp == 0 || minHeightDp == 0 || maxHeightDp == 0) {
        return 1
    }

    return listOf(
        DpSize(minWidthDp.dp, maxHeightDp.dp),
        DpSize(maxWidthDp.dp, minHeightDp.dp),
    ).distinct().size
}

private fun resolveLegacyPayloadVariantCount(snapshot: WidgetOptionsSnapshot): Int =
    listOfNotNull(
        glanceLegacySizeOrNull(snapshot.maxWidthDp, snapshot.minHeightDp),
        glanceLegacySizeOrNull(snapshot.minWidthDp, snapshot.maxHeightDp),
    ).distinct().size.coerceAtLeast(1)

private fun glanceLegacySizeOrNull(widthDp: Int?, heightDp: Int?): DpSize? {
    if (widthDp == null || widthDp == 0 || heightDp == null || heightDp == 0) {
        return null
    }
    return DpSize(widthDp.dp, heightDp.dp)
}

private fun buildStableExactSizeKey(
    snapshot: WidgetOptionsSnapshot,
    currentSize: DpSize,
    sdkBranch: String,
    resolvedSizes: List<DpSize>,
    payloadVariantCount: Int,
): String = buildString {
    append("widget-exact-sizes-v2|")
    append(sdkBranch)
    append("|explicit=")
    val explicitSizes = snapshot.explicitSizes
    if (explicitSizes == null) {
        append("absent")
    } else {
        explicitSizes.joinTo(this, separator = ",") { size -> size.stableValue() }
    }
    append("|legacy=")
    append(snapshot.minWidthDp)
    append(',')
    append(snapshot.maxWidthDp)
    append(',')
    append(snapshot.minHeightDp)
    append(',')
    append(snapshot.maxHeightDp)
    append("|current=")
    append(currentSize.stableValue())
    append("|resolved=")
    resolvedSizes.joinTo(this, separator = ",") { size -> size.stableValue() }
    append("|payloadVariants=")
    append(payloadVariantCount)
}

private fun legacySize(widthDp: Int?, heightDp: Int?): DpSize? {
    if (widthDp == null || widthDp <= 0 || heightDp == null || heightDp <= 0) {
        return null
    }
    return DpSize(widthDp.dp, heightDp.dp)
}

private fun WidgetExactSize.isUsable(): Boolean =
    widthDp.isFinite() && widthDp > 0f && heightDp.isFinite() && heightDp > 0f

private fun WidgetExactSize.toDpSize(): DpSize = DpSize(widthDp.dp, heightDp.dp)

private fun WidgetExactSize.stableValue(): String =
    "${widthDp.toRawBits()}:${heightDp.toRawBits()}"

private fun DpSize.stableValue(): String =
    "${width.value.toRawBits()}:${height.value.toRawBits()}"

internal data class WidgetExactSizeResolution(
    val stableKey: String,
    val sizes: List<DpSize>,
    val payloadVariantCount: Int,
) {
    init {
        require(sizes.isNotEmpty())
        require(payloadVariantCount > 0)
    }

    val variantCount: Int
        get() = sizes.size
}

internal data class WidgetOptionsSnapshot(
    val explicitSizes: List<WidgetExactSize>?,
    val minWidthDp: Int?,
    val maxWidthDp: Int?,
    val minHeightDp: Int?,
    val maxHeightDp: Int?,
) {
    companion object {
        @Suppress("DEPRECATION")
        fun fromBundle(bundle: Bundle): WidgetOptionsSnapshot {
            val explicitSizes = if (bundle.containsKey(AppWidgetManager.OPTION_APPWIDGET_SIZES)) {
                bundle.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                    .orEmpty()
                    .map { size -> WidgetExactSize(size.width, size.height) }
            } else {
                null
            }
            return WidgetOptionsSnapshot(
                explicitSizes = explicitSizes,
                minWidthDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                maxWidthDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                minHeightDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                maxHeightDp = bundle.optionalInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
            )
        }
    }
}

internal data class WidgetExactSize(
    val widthDp: Float,
    val heightDp: Float,
)

private fun Bundle.optionalInt(key: String): Int? = if (containsKey(key)) getInt(key) else null
