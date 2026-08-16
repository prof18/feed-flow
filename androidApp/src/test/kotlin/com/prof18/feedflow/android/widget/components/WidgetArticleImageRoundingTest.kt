package com.prof18.feedflow.android.widget.components

import android.graphics.Bitmap
import android.graphics.Color
import com.prof18.feedflow.android.widget.WidgetImageRequestIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetArticleImageRoundingTest {

    @Test
    fun `pre S Thumbnail identity and rendered bitmap use fixed 8dp corners`() {
        val key = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity(edgePx = 50, payloadBudgetBytes = 10_000L),
                renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
                sdkInt = 30,
                cornerRadiusDp = 8,
                displayViewportDp = 50,
            ),
        )
        val source = opaqueBitmap(width = 50, height = 50)

        val rendered = requireNotNull(validateAndRenderWidgetArticleBitmap(source, key))

        assertEquals(8, key.softwareCornerRadiusDp)
        assertEquals(50, key.softwareDisplayViewportDp)
        assertRoundedCorners(rendered)
        assertEquals(255, Color.alpha(rendered.getPixel(rendered.width / 2, rendered.height / 2)))
        assertEquals(50, rendered.width)
        assertEquals(50, rendered.height)
        assertEquals(Bitmap.Config.ARGB_8888, rendered.config)
        assertTrue(rendered.allocationByteCount.toLong() <= key.payloadBudgetBytes)
    }

    @Test
    fun `pre S List keeps legacy square bitmap and does not key on radius`() {
        val requestIdentity = requestIdentity(edgePx = 50, payloadBudgetBytes = 10_000L)
        val radius8Key = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity,
                renderPolicy = WidgetArticleImageRenderPolicy.GLANCE_ONLY,
                sdkInt = 30,
                cornerRadiusDp = 8,
                displayViewportDp = 50,
            ),
        )
        val radius32Key = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity,
                renderPolicy = WidgetArticleImageRenderPolicy.GLANCE_ONLY,
                sdkInt = 30,
                cornerRadiusDp = 32,
                displayViewportDp = 100,
            ),
        )
        val source = opaqueBitmap(width = 50, height = 50)

        val rendered = requireNotNull(validateAndRenderWidgetArticleBitmap(source, radius8Key))

        assertEquals(requestIdentity, radius8Key)
        assertEquals(requestIdentity, radius32Key)
        assertSame(source, rendered)
        assertSquareCorners(rendered)
    }

    @Test
    fun `pre S Fill bitmap center-crops and applies configured radius to all four corners`() {
        val key = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity(edgePx = 160, payloadBudgetBytes = 102_400L),
                renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
                sdkInt = 26,
                cornerRadiusDp = 32,
                displayViewportDp = 100,
            ),
        )
        val source = opaqueBitmap(width = 160, height = 100)

        val rendered = requireNotNull(validateAndRenderWidgetArticleBitmap(source, key))

        assertNotSame(source, rendered)
        assertEquals(160, rendered.width)
        assertEquals(160, rendered.height)
        assertEquals(Bitmap.Config.ARGB_8888, rendered.config)
        assertTrue(rendered.allocationByteCount.toLong() <= key.payloadBudgetBytes)
        assertRoundedCorners(rendered)
        assertEquals(Color.MAGENTA, rendered.getPixel(rendered.width / 2, rendered.height / 2))
    }

    @Test
    fun `zero radius remains square and does not copy validated bitmap`() {
        val key = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity(edgePx = 50, payloadBudgetBytes = 10_000L),
                renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
                sdkInt = 30,
                cornerRadiusDp = 0,
                displayViewportDp = 50,
            ),
        )
        val source = opaqueBitmap(width = 50, height = 50)

        val rendered = requireNotNull(validateAndRenderWidgetArticleBitmap(source, key))

        assertEquals(0, key.softwareCornerRadiusDp)
        assertEquals(0, key.softwareDisplayViewportDp)
        assertSame(source, rendered)
        assertSquareCorners(rendered)
    }

    @Test
    fun `S and newer preserve native Glance rounding without software render identity`() {
        val requestIdentity = requestIdentity(edgePx = 100, payloadBudgetBytes = 40_000L)
        val radius8 = resolveWidgetArticleImageIdentity(
            requestIdentity = requestIdentity,
            renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
            sdkInt = 31,
            cornerRadiusDp = 8,
            displayViewportDp = 50,
        )
        val radius32 = resolveWidgetArticleImageIdentity(
            requestIdentity = requestIdentity,
            renderPolicy = WidgetArticleImageRenderPolicy.GLANCE_ONLY,
            sdkInt = 35,
            cornerRadiusDp = 32,
            displayViewportDp = 100,
        )

        assertEquals(requestIdentity, radius8)
        assertEquals(requestIdentity, radius32)
    }

    @Test
    fun `rounded output is reduced to final payload budget before delivery`() {
        val key = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity(edgePx = 100, payloadBudgetBytes = 10_000L),
                renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
                sdkInt = 30,
                cornerRadiusDp = 24,
                displayViewportDp = 100,
            ),
        )
        val source = opaqueBitmap(width = 100, height = 60)

        val rendered = requireNotNull(validateAndRenderWidgetArticleBitmap(source, key))

        assertEquals(50, rendered.width)
        assertEquals(50, rendered.height)
        assertTrue(rendered.allocationByteCount.toLong() <= key.payloadBudgetBytes)
        assertRoundedCorners(rendered)
    }

    @Test
    fun `unsafe bitmap is still omitted before software rounding`() {
        val key = requireNotNull(
            resolveWidgetArticleImageIdentity(
                requestIdentity = requestIdentity(edgePx = 50, payloadBudgetBytes = 10_000L),
                renderPolicy = WidgetArticleImageRenderPolicy.CARD_COMPATIBLE,
                sdkInt = 30,
                cornerRadiusDp = 8,
                displayViewportDp = 50,
            ),
        )
        val recycled = opaqueBitmap(width = 50, height = 50).apply(Bitmap::recycle)

        val rendered = validateAndRenderWidgetArticleBitmap(recycled, key)

        assertNull(rendered)
    }

    private fun requestIdentity(
        edgePx: Int,
        payloadBudgetBytes: Long,
    ) = WidgetImageRequestIdentity(
        imageUrl = "https://example.com/article.png",
        edgePx = edgePx,
        exactSizeKey = "same-exact-sizes",
        payloadBudgetBytes = payloadBudgetBytes,
    )

    private fun opaqueBitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }

    private fun assertRoundedCorners(bitmap: Bitmap) {
        val lastX = bitmap.width - 1
        val lastY = bitmap.height - 1
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        assertEquals(0, Color.alpha(bitmap.getPixel(lastX, 0)))
        assertEquals(0, Color.alpha(bitmap.getPixel(0, lastY)))
        assertEquals(0, Color.alpha(bitmap.getPixel(lastX, lastY)))
    }

    private fun assertSquareCorners(bitmap: Bitmap) {
        val lastX = bitmap.width - 1
        val lastY = bitmap.height - 1
        assertEquals(255, Color.alpha(bitmap.getPixel(0, 0)))
        assertEquals(255, Color.alpha(bitmap.getPixel(lastX, 0)))
        assertEquals(255, Color.alpha(bitmap.getPixel(0, lastY)))
        assertEquals(255, Color.alpha(bitmap.getPixel(lastX, lastY)))
    }
}
