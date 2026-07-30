package com.prof18.feedflow.android.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.domain.feed.MAX_WIDGET_FEED_ITEMS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetImageBudgetTest {

    @Test
    fun `payload count always reserves maximum feed items for every exact-size variant`() {
        val oneVariant = resolveBudget(variantCount = 1)
        val threeVariants = resolveBudget(variantCount = 3)

        assertEquals(MAX_WIDGET_FEED_ITEMS.toLong(), oneVariant.payloadCount)
        assertEquals(MAX_WIDGET_FEED_ITEMS.toLong() * 3L, threeVariants.payloadCount)
        assertEquals(oneVariant.payloadBudgetBytes / 3L, threeVariants.payloadBudgetBytes)
    }

    @Test
    fun `budget reserves 15 payloads for every Glance variant including unusable layout sizes`() {
        val exactSizes = resolveExactSizes(
            snapshot = WidgetOptionsSnapshot(
                explicitSizes = listOf(
                    WidgetExactSize(100f, 200f),
                    WidgetExactSize(0f, 200f),
                    WidgetExactSize(100f, 200f),
                    WidgetExactSize(-40f, 200f),
                ),
                minWidthDp = null,
                maxWidthDp = null,
                minHeightDp = null,
                maxHeightDp = null,
            ),
            currentSize = DpSize(120.dp, 180.dp),
            sdkInt = 31,
        )

        val budget = resolveWidgetImageBudget(
            screenWidthPx = 480,
            screenHeightPx = 800,
            exactSizes = exactSizes,
        )

        assertEquals(1, exactSizes.variantCount)
        assertEquals(3, exactSizes.payloadVariantCount)
        assertEquals(MAX_WIDGET_FEED_ITEMS.toLong() * 3L, budget.payloadCount)
        assertTrue(budget.exactSizeKey.contains("payloadVariants=3"))
    }

    @Test
    fun `list policy keeps 50dp viewport and fixed 15 by variant shared budget`() {
        val variantCount = 3
        val imageLayout = resolveWidgetListImageLayout(displayDensity = 2f)
        val budget = resolveBudget(variantCount = variantCount)
        val requests = List(MAX_WIDGET_FEED_ITEMS * variantCount) { index ->
            requireNotNull(
                budget.resolveRequest(
                    imageUrl = "https://example.com/list-image-$index",
                    displayTargetPx = imageLayout.displayTargetPx,
                ),
            )
        }

        assertEquals(50, imageLayout.displayViewportDp)
        assertEquals(100, imageLayout.displayTargetPx)
        assertEquals((MAX_WIDGET_FEED_ITEMS * variantCount).toLong(), budget.payloadCount)
        assertTrue(requests.all { it.identity.payloadBudgetBytes == budget.payloadBudgetBytes })
        assertTrue(requests.all { it.identity.exactSizeKey == budget.exactSizeKey })
    }

    @Test
    fun `480 by 800 display uses Android ceiling with 25 percent reserve`() {
        val budget = resolveBudget(
            screenWidthPx = 480,
            screenHeightPx = 800,
            variantCount = 1,
        )

        assertEquals(2_304_000L, budget.remoteViewsLimitBytes)
        assertEquals(1_728_000L, budget.effectiveArticleBudgetBytes)
        assertEquals(115_200L, budget.payloadBudgetBytes)
        assertEquals(169, budget.budgetEdgePx)
    }

    @Test
    fun `large displays cap the effective article budget at 6 MiB`() {
        val budget = resolveBudget(
            screenWidthPx = 4_000,
            screenHeightPx = 4_000,
            variantCount = 1,
        )

        assertEquals(6L * 1024L * 1024L, budget.effectiveArticleBudgetBytes)
        assertEquals(323, budget.budgetEdgePx)
    }

    @Test
    fun `maximum integer dimensions do not overflow into a negative budget`() {
        val budget = resolveBudget(
            screenWidthPx = Int.MAX_VALUE,
            screenHeightPx = Int.MAX_VALUE,
            variantCount = 2,
        )

        assertTrue(budget.remoteViewsLimitBytes > 0L)
        assertEquals(6L * 1024L * 1024L, budget.effectiveArticleBudgetBytes)
        assertTrue(budget.payloadBudgetBytes > 0L)
    }

    @Test
    fun `request edge is capped by display target and byte budget`() {
        val budget = resolveBudget(
            screenWidthPx = 480,
            screenHeightPx = 800,
            variantCount = 1,
        )

        assertEquals(50, budget.resolveRequest("https://example.com/image", displayTargetPx = 50)?.edgePx)
        assertEquals(169, budget.resolveRequest("https://example.com/image", displayTargetPx = 500)?.edgePx)
        assertNull(budget.resolveRequest("https://example.com/image", displayTargetPx = 0))
    }

    @Test
    fun `feed emission count cannot change fixed-capacity budget or otherwise identical request identity`() {
        val budget = resolveBudget(variantCount = 2)
        val oneImageEmission = listOf("https://example.com/image")
        val fifteenImageEmission = List(MAX_WIDGET_FEED_ITEMS) { index ->
            if (index == 0) "https://example.com/image" else "https://example.com/image-$index"
        }

        val oneItemRequest = budget.resolveRequest(oneImageEmission.first(), displayTargetPx = 50)
        val fifteenItemRequest = budget.resolveRequest(fifteenImageEmission.first(), displayTargetPx = 50)

        assertEquals(MAX_WIDGET_FEED_ITEMS.toLong() * 2L, budget.payloadCount)
        assertEquals(oneItemRequest, fifteenItemRequest)
    }

    @Test
    fun `smaller budget changes policy and request identity when bounded edge is unchanged`() {
        val largerBudget = resolveBudget(variantCount = 1, exactSizeKey = "one-size")
        val smallerBudget = resolveBudget(variantCount = 2, exactSizeKey = "two-sizes")
        val largerRequest = largerBudget.resolveRequest("https://example.com/image", displayTargetPx = 50)
        val smallerRequest = smallerBudget.resolveRequest("https://example.com/image", displayTargetPx = 50)

        assertTrue(smallerBudget.payloadBudgetBytes < largerBudget.payloadBudgetBytes)
        assertEquals(largerRequest?.edgePx, smallerRequest?.edgePx)
        assertNotEquals(largerBudget.identity, smallerBudget.identity)
        assertNotEquals(largerRequest?.identity, smallerRequest?.identity)
        assertEquals(smallerBudget.payloadBudgetBytes, smallerRequest?.identity?.payloadBudgetBytes)
        assertEquals("two-sizes", smallerRequest?.identity?.exactSizeKey)
    }

    @Test
    fun `exact-size key participates in policy and request identity even at the same budget`() {
        val first = resolveBudget(variantCount = 1, exactSizeKey = "first")
        val second = resolveBudget(variantCount = 1, exactSizeKey = "second")

        assertEquals(first.payloadBudgetBytes, second.payloadBudgetBytes)
        assertNotEquals(first.identity, second.identity)
        assertNotEquals(
            first.resolveRequest("https://example.com/image", 50)?.identity,
            second.resolveRequest("https://example.com/image", 50)?.identity,
        )
    }

    private fun resolveBudget(
        screenWidthPx: Int = 480,
        screenHeightPx: Int = 800,
        variantCount: Int,
        exactSizeKey: String = "sizes-$variantCount",
    ): WidgetImageBudgetPolicy = resolveWidgetImageBudget(
        screenWidthPx = screenWidthPx,
        screenHeightPx = screenHeightPx,
        exactSizes = WidgetExactSizeResolution(
            stableKey = exactSizeKey,
            sizes = List(variantCount) { index ->
                DpSize((100 + index).dp, (200 + index).dp)
            },
            payloadVariantCount = variantCount,
        ),
    )
}
