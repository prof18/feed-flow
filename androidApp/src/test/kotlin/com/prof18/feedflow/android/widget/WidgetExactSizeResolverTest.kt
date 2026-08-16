package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.SizeF
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetExactSizeResolverTest {

    @Test
    fun `snapshot copies bundle values and explicit sizes immutably`() {
        val sourceSizes = arrayListOf(SizeF(100f, 200f))
        val bundle = legacyBundle(minWidth = 80, maxWidth = 160, minHeight = 60, maxHeight = 240).apply {
            putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sourceSizes)
        }

        val snapshot = WidgetOptionsSnapshot.fromBundle(bundle)
        sourceSizes += SizeF(300f, 400f)
        bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 999)

        assertEquals(listOf(WidgetExactSize(100f, 200f)), snapshot.explicitSizes)
        assertEquals(80, snapshot.minWidthDp)
    }

    @Test
    fun `api 31 uses usable explicit sizes and removes invalid entries and duplicates`() {
        val snapshot = WidgetOptionsSnapshot(
            explicitSizes = listOf(
                WidgetExactSize(100f, 200f),
                WidgetExactSize(0f, 200f),
                WidgetExactSize(100f, 200f),
                WidgetExactSize(300f, -1f),
                WidgetExactSize(240f, 120f),
            ),
            minWidthDp = 80,
            maxWidthDp = 160,
            minHeightDp = 60,
            maxHeightDp = 240,
        )

        val result = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 31)

        assertEquals(
            listOf(DpSize(100.dp, 200.dp), DpSize(240.dp, 120.dp)),
            result.sizes,
        )
        assertEquals(2, result.variantCount)
        assertEquals(4, result.payloadVariantCount)
    }

    @Test
    fun `payload variant count matches Glance 1_1_1 exact compositions across option shapes`() {
        val cases = listOf(
            GlanceCase(
                description = "api 31 normal explicit sizes",
                snapshot = options(explicitSizes = listOf(size(100, 200), size(240, 120))),
                sdkInt = 31,
            ),
            GlanceCase(
                description = "api 31 mixed valid malformed and duplicate explicit sizes",
                snapshot = options(
                    explicitSizes = listOf(
                        size(100, 200),
                        size(0, 200),
                        size(100, 200),
                        size(-40, 200),
                    ),
                ),
                sdkInt = 35,
            ),
            GlanceCase(
                description = "api 31 empty explicit sizes use complete legacy fields",
                snapshot = options(
                    explicitSizes = emptyList(),
                    minWidthDp = 80,
                    maxWidthDp = 160,
                    minHeightDp = 60,
                    maxHeightDp = 240,
                ),
                sdkInt = 31,
            ),
            GlanceCase(
                description = "api 31 absent explicit sizes deduplicate equal legacy pairs",
                snapshot = options(
                    minWidthDp = 100,
                    maxWidthDp = 100,
                    minHeightDp = 200,
                    maxHeightDp = 200,
                ),
                sdkInt = 31,
            ),
            GlanceCase(
                description = "api 31 missing legacy field uses current size",
                snapshot = options(minWidthDp = 80, maxHeightDp = 240),
                sdkInt = 31,
            ),
            GlanceCase(
                description = "api 31 zero legacy field uses current size",
                snapshot = options(
                    explicitSizes = emptyList(),
                    minWidthDp = 0,
                    maxWidthDp = 160,
                    minHeightDp = 60,
                    maxHeightDp = 240,
                ),
                sdkInt = 31,
            ),
            GlanceCase(
                description = "api 31 negative nonzero legacy fields still create variants",
                snapshot = options(
                    minWidthDp = -80,
                    maxWidthDp = -160,
                    minHeightDp = -60,
                    maxHeightDp = -240,
                ),
                sdkInt = 31,
            ),
            GlanceCase(
                description = "api 26 complete legacy pairs",
                snapshot = options(
                    minWidthDp = 80,
                    maxWidthDp = 160,
                    minHeightDp = 60,
                    maxHeightDp = 240,
                ),
                sdkInt = 30,
            ),
            GlanceCase(
                description = "api 26 partial legacy fields create one orientation",
                snapshot = options(minWidthDp = 80, maxHeightDp = 240),
                sdkInt = 26,
            ),
            GlanceCase(
                description = "api 26 zero and missing legacy fields use current size",
                snapshot = options(minWidthDp = 0, maxHeightDp = 240),
                sdkInt = 30,
            ),
            GlanceCase(
                description = "api 26 negative nonzero legacy pair still creates a variant",
                snapshot = options(maxWidthDp = -160, minHeightDp = -60),
                sdkInt = 26,
            ),
            GlanceCase(
                description = "api 26 equal orientation pairs are deduplicated",
                snapshot = options(
                    minWidthDp = 100,
                    maxWidthDp = 100,
                    minHeightDp = 200,
                    maxHeightDp = 200,
                ),
                sdkInt = 30,
            ),
        )

        cases.forEach { case ->
            val glanceVariantCount = glance111ExactVariantCount(
                snapshot = case.snapshot,
                currentSize = CURRENT_SIZE,
                sdkInt = case.sdkInt,
            )
            val result = resolveExactSizes(case.snapshot, CURRENT_SIZE, case.sdkInt)

            assertEquals(case.description, glanceVariantCount, result.payloadVariantCount)
        }
    }

    @Test
    fun `non-finite explicit sizes are conservatively counted and payload count never reaches zero`() {
        val malformed = resolveExactSizes(
            snapshot = options(
                explicitSizes = listOf(
                    WidgetExactSize(Float.NaN, 200f),
                    WidgetExactSize(Float.POSITIVE_INFINITY, 100f),
                    WidgetExactSize(Float.NaN, 200f),
                ),
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 31,
        )
        val missing = resolveExactSizes(
            snapshot = options(),
            currentSize = CURRENT_SIZE,
            sdkInt = 31,
        )

        assertEquals(listOf(CURRENT_SIZE), malformed.sizes)
        assertEquals(2, malformed.payloadVariantCount)
        assertEquals(1, missing.payloadVariantCount)
    }

    @Test
    fun `api 31 derives portrait and landscape only when all four legacy fields are positive`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = 80,
                maxWidthDp = 160,
                minHeightDp = 60,
                maxHeightDp = 240,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 31,
        )

        assertEquals(
            listOf(DpSize(80.dp, 240.dp), DpSize(160.dp, 60.dp)),
            result.sizes,
        )
    }

    @Test
    fun `api 31 empty explicit list with only one legacy pair falls back to current size`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = emptyList(),
                minWidthDp = 80,
                maxWidthDp = null,
                minHeightDp = null,
                maxHeightDp = 240,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 35,
        )

        assertEquals(listOf(CURRENT_SIZE), result.sizes)
    }

    @Test
    fun `api 26 through 30 accepts a valid portrait pair independently`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = listOf(WidgetExactSize(999f, 999f)),
                minWidthDp = 80,
                maxWidthDp = null,
                minHeightDp = null,
                maxHeightDp = 240,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 30,
        )

        assertEquals(listOf(DpSize(80.dp, 240.dp)), result.sizes)
    }

    @Test
    fun `api 26 through 30 accepts a valid landscape pair independently`() {
        val result = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = null,
                maxWidthDp = 160,
                minHeightDp = 60,
                maxHeightDp = null,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 26,
        )

        assertEquals(listOf(DpSize(160.dp, 60.dp)), result.sizes)
    }

    @Test
    fun `legacy duplicate candidates are filtered and current size is used when neither pair is valid`() {
        val duplicatePairs = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = 100,
                maxWidthDp = 100,
                minHeightDp = 200,
                maxHeightDp = 200,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 30,
        )
        val noPairs = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = null,
                minWidthDp = 0,
                maxWidthDp = -1,
                minHeightDp = 60,
                maxHeightDp = null,
            ),
            currentSize = CURRENT_SIZE,
            sdkInt = 30,
        )

        assertEquals(listOf(DpSize(100.dp, 200.dp)), duplicatePairs.sizes)
        assertEquals(listOf(CURRENT_SIZE), noPairs.sizes)
    }

    @Test
    fun `stable key and variant count are deterministic from one snapshot current size and sdk branch`() {
        val snapshot = WidgetOptionsSnapshot(
            explicitSizes = listOf(WidgetExactSize(100f, 200f), WidgetExactSize(240f, 120f)),
            minWidthDp = 80,
            maxWidthDp = 160,
            minHeightDp = 60,
            maxHeightDp = 240,
        )

        val first = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 31)
        val second = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 31)
        val legacyBranch = resolveExactSizes(snapshot, CURRENT_SIZE, sdkInt = 30)

        assertEquals(first.stableKey, second.stableKey)
        assertEquals(first.variantCount, second.variantCount)
        assertNotEquals(first.stableKey, legacyBranch.stableKey)
    }

    private fun glance111ExactVariantCount(
        snapshot: WidgetOptionsSnapshot,
        currentSize: DpSize,
        sdkInt: Int,
    ): Int {
        val appWidgetUtils = Class.forName("androidx.glance.appwidget.AppWidgetUtilsKt")
        val extractedSizes = if (sdkInt >= 31) {
            val extractAllSizes = appWidgetUtils.getMethod(
                "extractAllSizes",
                Bundle::class.java,
                Function0::class.java,
            )
            @Suppress("UNCHECKED_CAST")
            extractAllSizes.invoke(
                null,
                snapshot.toBundle(),
                { currentSize },
            ) as List<DpSize>
        } else {
            val extractOrientationSizes = appWidgetUtils.getMethod(
                "extractOrientationSizes",
                Bundle::class.java,
            )
            @Suppress("UNCHECKED_CAST")
            (extractOrientationSizes.invoke(null, snapshot.toBundle()) as List<DpSize>)
                .ifEmpty { listOf(currentSize) }
        }
        return extractedSizes.distinct().size
    }

    private fun WidgetOptionsSnapshot.toBundle(): Bundle = Bundle().apply {
        explicitSizes?.let { sizes ->
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                ArrayList(sizes.map { size -> SizeF(size.widthDp, size.heightDp) }),
            )
        }
        minWidthDp?.let { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, it) }
        maxWidthDp?.let { putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, it) }
        minHeightDp?.let { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, it) }
        maxHeightDp?.let { putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, it) }
    }

    private fun options(
        explicitSizes: List<WidgetExactSize>? = null,
        minWidthDp: Int? = null,
        maxWidthDp: Int? = null,
        minHeightDp: Int? = null,
        maxHeightDp: Int? = null,
    ): WidgetOptionsSnapshot = WidgetOptionsSnapshot(
        explicitSizes = explicitSizes,
        minWidthDp = minWidthDp,
        maxWidthDp = maxWidthDp,
        minHeightDp = minHeightDp,
        maxHeightDp = maxHeightDp,
    )

    private fun size(widthDp: Int, heightDp: Int): WidgetExactSize =
        WidgetExactSize(widthDp.toFloat(), heightDp.toFloat())

    private data class GlanceCase(
        val description: String,
        val snapshot: WidgetOptionsSnapshot,
        val sdkInt: Int,
    )

    private fun legacyBundle(
        minWidth: Int,
        maxWidth: Int,
        minHeight: Int,
        maxHeight: Int,
    ): Bundle = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)
    }

    private companion object {
        val CURRENT_SIZE = DpSize(120.dp, 180.dp)
    }
}
