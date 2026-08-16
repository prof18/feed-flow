package com.prof18.feedflow.android.widget.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import com.prof18.feedflow.android.widget.WidgetExactSizeResolution
import com.prof18.feedflow.android.widget.WidgetImageRequestIdentity
import com.prof18.feedflow.android.widget.resolveWidgetImageBudget
import com.prof18.feedflow.shared.domain.feed.MAX_WIDGET_FEED_ITEMS
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WidgetBitmapValidatorTest {

    @Test
    fun `validated allocations for every fixed slot stay within aggregate image budget`() {
        listOf(1, 2, 3, 5).forEach { variantCount ->
            val budget = resolveWidgetImageBudget(
                screenWidthPx = 480,
                screenHeightPx = 800,
                exactSizes = WidgetExactSizeResolution(
                    stableKey = "sizes-$variantCount",
                    sizes = List(variantCount) { index ->
                        DpSize((100 + index).dp, (200 + index).dp)
                    },
                    payloadVariantCount = variantCount,
                ),
                feedItemCount = MAX_WIDGET_FEED_ITEMS,
            )
            val slotCount = budget.payloadCount.toInt()
            val allocationByteCounts = List(slotCount) { slotIndex ->
                val request = requireNotNull(
                    budget.resolveRequest(
                        imageUrl = "https://example.com/image-$slotIndex",
                        displayTargetPx = 1_000,
                    ),
                )
                assertEquals(budget.budgetEdgePx, request.edgePx)

                val decodedBitmap = Bitmap.createBitmap(
                    request.edgePx,
                    request.edgePx,
                    Bitmap.Config.ARGB_8888,
                )
                val validatedBitmap = requireNotNull(
                    WidgetBitmapValidator.validate(
                        bitmap = decodedBitmap,
                        requestEdgePx = request.edgePx,
                        payloadBudgetBytes = request.identity.payloadBudgetBytes,
                    ),
                )
                val allocationByteCount = validatedBitmap.allocationByteCount.toLong()

                assertTrue(validatedBitmap.width <= request.edgePx)
                assertTrue(validatedBitmap.height <= request.edgePx)
                assertTrue(allocationByteCount <= request.identity.payloadBudgetBytes)
                validatedBitmap.recycle()
                allocationByteCount
            }

            assertEquals(budget.payloadCount, allocationByteCounts.size.toLong())
            assertTrue(allocationByteCounts.sum() <= budget.effectiveArticleBudgetBytes)
        }
    }

    @Test
    fun `software ARGB 8888 within dimensions and budget is accepted`() {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888)

        val result = WidgetBitmapValidator.validate(
            bitmap = bitmap,
            requestEdgePx = 40,
            payloadBudgetBytes = bitmap.allocationByteCount.toLong(),
        )

        assertSame(bitmap, result)
    }

    @Test
    fun `RGBA F16 is converted to software ARGB 8888`() {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.RGBA_F16)

        val result = WidgetBitmapValidator.validate(
            bitmap = bitmap,
            requestEdgePx = 20,
            payloadBudgetBytes = 20L * 20L * 4L,
        )

        requireNotNull(result)
        assertNotSame(bitmap, result)
        assertEquals(Bitmap.Config.ARGB_8888, result.config)
        assertFalse(result.config == Bitmap.Config.HARDWARE)
        assertEquals(20, result.width)
        assertEquals(20, result.height)
    }

    @Test
    fun `dimensions larger than the request are rerasterized within the request edge`() {
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)

        val result = WidgetBitmapValidator.validate(
            bitmap = bitmap,
            requestEdgePx = 40,
            payloadBudgetBytes = 40L * 40L * 4L,
        )

        requireNotNull(result)
        assertNotSame(bitmap, result)
        assertEquals(Bitmap.Config.ARGB_8888, result.config)
        assertTrue(result.width <= 40)
        assertTrue(result.height <= 40)
    }

    @Test
    fun `allocation over budget is reduced once from the actual allocation`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.RGBA_F16)
        val payloadBudgetBytes = 200L
        assertTrue(bitmap.allocationByteCount.toLong() > bitmap.width.toLong() * bitmap.height * 4L)

        val result = WidgetBitmapValidator.validate(
            bitmap = bitmap,
            requestEdgePx = 10,
            payloadBudgetBytes = payloadBudgetBytes,
        )

        requireNotNull(result)
        assertNotSame(bitmap, result)
        assertEquals(7, result.width)
        assertEquals(7, result.height)
        assertTrue(result.allocationByteCount.toLong() <= payloadBudgetBytes)
    }

    @Test
    fun `impossible budget omits the bitmap`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val result = WidgetBitmapValidator.validate(
            bitmap = bitmap,
            requestEdgePx = 1,
            payloadBudgetBytes = 3L,
        )

        assertNull(result)
    }

    @Test
    fun `hardware result is converted to compliant software or omitted`() {
        val hardwareBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
            .copy(Bitmap.Config.HARDWARE, false)
        assertEquals(Bitmap.Config.HARDWARE, hardwareBitmap.config)

        val result = WidgetBitmapValidator.validate(
            bitmap = hardwareBitmap,
            requestEdgePx = 20,
            payloadBudgetBytes = 20L * 20L * 4L,
        )

        assertTrue(
            result == null ||
                result.config == Bitmap.Config.ARGB_8888 &&
                result.config != Bitmap.Config.HARDWARE &&
                result.allocationByteCount.toLong() <= 20L * 20L * 4L,
        )
    }

    @Test
    fun `conversion failure omits the bitmap`() {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.RGBA_F16)
        bitmap.recycle()

        val result = WidgetBitmapValidator.validate(
            bitmap = bitmap,
            requestEdgePx = 20,
            payloadBudgetBytes = 20L * 20L * 4L,
        )

        assertNull(result)
    }

    @Test
    fun `Coil request uses exact square software ARGB 8888 decoding`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val key = requestIdentity(edgePx = 42, payloadBudgetBytes = 7_056L)

        val request = buildWidgetArticleImageRequest(context, key)

        assertEquals("https://example.com/article.png", request.data)
        assertEquals(Size(42, 42), request.sizeResolver.size())
        assertEquals(Precision.EXACT, request.precision)
        assertEquals(Scale.FILL, request.scale)
        assertFalse(request.allowHardware)
        assertEquals(Bitmap.Config.ARGB_8888, request.bitmapConfig)
    }

    @Test
    fun `full key changes when budget changes at the same bounded edge`() {
        val first = requestIdentity(edgePx = 42, payloadBudgetBytes = 7_056L)
        val second = requestIdentity(edgePx = 42, payloadBudgetBytes = 6_000L)

        assertEquals(first.imageUrl, second.imageUrl)
        assertEquals(first.edgePx, second.edgePx)
        assertTrue(first != second)
    }

    private fun requestIdentity(
        edgePx: Int,
        payloadBudgetBytes: Long,
    ) = WidgetImageRequestIdentity(
        imageUrl = "https://example.com/article.png",
        edgePx = edgePx,
        exactSizeKey = "same-exact-size",
        payloadBudgetBytes = payloadBudgetBytes,
    )
}
