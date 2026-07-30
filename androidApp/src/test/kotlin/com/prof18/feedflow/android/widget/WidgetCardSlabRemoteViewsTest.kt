package com.prof18.feedflow.android.widget

import android.content.Context
import android.graphics.Outline
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.unit.ColorProvider
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.R
import com.prof18.feedflow.android.widget.components.WidgetFeedItemCard
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import sun.misc.Unsafe

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 30])
class WidgetCardSlabRemoteViewsTest {

    @Test
    fun `pre S slab layouts declare whole-root outline clipping`() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources

        listOf(
            R.layout.widget_card_slab_themed,
            R.layout.widget_card_slab_resolved,
        ).forEach { layoutResource ->
            resources.getLayout(layoutResource).use { parser ->
                while (parser.next() != XmlPullParser.START_TAG) {
                    // Advance to the root element.
                }
                assertTrue(
                    parser.getAttributeBooleanValue(
                        "http://schemas.android.com/apk/res/android",
                        "clipToOutline",
                        false,
                    ),
                )
            }
        }
    }

    @Test
    fun `mapped outline clips the complete pre S slab at every normalized radius`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        (0..32 step 2).forEach { radiusDp ->
            val slab = inflateSlab(
                context = context,
                radiusDp = radiusDp,
                colorSource = WidgetColorProviderSource.THEMED,
                resolvedSlabFillColor = null,
            )
            slab.measure(exactly(1_000), exactly(1_000))
            slab.layout(0, 0, 1_000, 1_000)
            // Robolectric does not hydrate View's clipToOutline XML attribute on these SDKs.
            slab.clipToOutline = true
            val outline = Outline()
            slab.outlineProvider.getOutline(slab, outline)

            assertTrue(slab.clipToOutline)
            assertTrue(outline.canClip())
            val expectedRadiusPx = radiusDp * context.resources.displayMetrics.density
            assertEquals(
                expectedRadiusPx,
                (slab.background as GradientDrawable).cornerRadius,
                0.01f,
            )
            assertSame(slab, slab.findViewById<View>(R.id.widget_card_slab_content).parent)
        }
    }

    @Test
    fun `themed pre S slab keeps a resource-aware day-night surface`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val originalQualifiers = RuntimeEnvironment.getQualifiers()
        val dayTint: Int?
        val nightTint: Int?
        try {
            RuntimeEnvironment.setQualifiers("+notnight")
            dayTint = inflateSlabBackground(
                context = context,
                colorSource = WidgetColorProviderSource.THEMED,
                resolvedSlabFillColor = null,
            ).imageTintList?.defaultColor
            RuntimeEnvironment.setQualifiers("+night")
            nightTint = inflateSlabBackground(
                context = context,
                colorSource = WidgetColorProviderSource.THEMED,
                resolvedSlabFillColor = null,
            ).imageTintList?.defaultColor
        } finally {
            RuntimeEnvironment.setQualifiers(originalQualifiers)
        }

        assertNotNull(dayTint)
        assertNotNull(nightTint)
        assertNotEquals(dayTint, nightTint)
    }

    @Test
    fun `resolved pre S slab keeps configured surface opacity`() {
        val background = inflateSlabBackground(
            context = ApplicationProvider.getApplicationContext(),
            colorSource = WidgetColorProviderSource.RESOLVED,
            resolvedSlabFillColor = Color(0x59445566),
        )

        assertEquals(89, background.imageAlpha)
        val colorFilter = background.colorFilter as PorterDuffColorFilter
        assertEquals(0xFF445566.toInt(), shadowOf(colorFilter).color)
        assertNull(background.imageTintList)
    }

    @Test
    @Config(sdk = [31])
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    fun `direct S slab reapply clears nontransparent background at zero opacity`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val renderer = GlanceRemoteViews()
        val nontransparentColor = Color(0x82445566)
        val nontransparent = composeDirectCard(
            renderer = renderer,
            context = context,
            appearance = WidgetCardAppearance(
                surfaceOpacityPercent = 51,
                itemSeparation = WidgetCardItemSeparation.NONE,
            ),
            slabFillColor = nontransparentColor,
        )
        val transparent = composeDirectCard(
            renderer = renderer,
            context = context,
            appearance = WidgetCardAppearance(
                surfaceOpacityPercent = 0,
                itemSeparation = WidgetCardItemSeparation.NONE,
            ),
            slabFillColor = null,
        )

        assertEquals(nontransparent.layoutId, transparent.layoutId)
        val appliedRoot = nontransparent.apply(context, FrameLayout(context))
        val slab = requireNotNull(appliedRoot.findViewWithBackgroundColor(nontransparentColor.toArgb()))

        transparent.reapply(context, appliedRoot)

        assertEquals(Color.Transparent.toArgb(), (slab.background as ColorDrawable).color)
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private suspend fun composeDirectCard(
        renderer: GlanceRemoteViews,
        context: Context,
        appearance: WidgetCardAppearance,
        slabFillColor: Color?,
    ): RemoteViews = renderer.compose(
        context = context,
        size = DpSize(width = 320.dp, height = 100.dp),
    ) {
        WidgetFeedItemCard(
            feedItem = testFeedItem(),
            browserManager = instanceWithoutConstructor(BrowserManager::class.java),
            fontSizes = widgetFontSizes(scaleFactor = 0),
            hideImages = true,
            appearance = appearance,
            slabFillColor = slabFillColor?.let { ColorProvider(it) },
            slabFillColorSource = WidgetColorProviderSource.RESOLVED,
            resolvedSlabFillColor = slabFillColor,
            primaryTextColor = ColorProvider(Color.White),
            secondaryTextColor = ColorProvider(Color.LightGray),
            cardLayout = ResolvedWidgetCardLayout(
                imageSizing = WidgetCardImageSizing.THUMBNAIL,
                fixedRowHeightDp = null,
                imageViewportDp = WIDGET_THUMBNAIL_VIEWPORT_DP,
                displayTargetDp = WIDGET_THUMBNAIL_VIEWPORT_DP,
            ),
            imageBudgetPolicy = WidgetImageBudgetPolicy(
                exactSizeKey = "test-size",
                payloadCount = 1,
                remoteViewsLimitBytes = 1,
                effectiveArticleBudgetBytes = 1,
                payloadBudgetBytes = 1,
                budgetEdgePx = 1,
            ),
            imageDisplayTargetPx = 1,
        )
    }.remoteViews

    private fun testFeedItem(): FeedItem = FeedItem(
        id = "test-item",
        url = "",
        title = "Test title",
        subtitle = null,
        content = null,
        imageUrl = null,
        feedSource = FeedSource(
            id = "test-feed",
            url = "https://example.com/feed.xml",
            title = "Test feed",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        ),
        pubDateMillis = null,
        isRead = false,
        dateString = null,
        commentsUrl = null,
        isBookmarked = false,
    )

    private fun View.findViewWithBackgroundColor(color: Int): View? {
        if ((background as? ColorDrawable)?.color == color) {
            return this
        }
        if (this is ViewGroup) {
            repeat(childCount) { index ->
                getChildAt(index).findViewWithBackgroundColor(color)?.let { return it }
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> instanceWithoutConstructor(type: Class<T>): T =
        unsafe.allocateInstance(type) as T

    private fun inflateSlab(
        context: Context,
        radiusDp: Int,
        colorSource: WidgetColorProviderSource,
        resolvedSlabFillColor: Color?,
    ): FrameLayout {
        val root = createPreSWidgetCardSlabRemoteViews(
            context = context,
            cornerRadiusDp = radiusDp,
            colorSource = colorSource,
            resolvedSlabFillColor = resolvedSlabFillColor,
        ).apply(context, FrameLayout(context))

        return requireNotNull(root.findViewById(R.id.widget_card_slab_root))
    }

    private fun inflateSlabBackground(
        context: Context,
        colorSource: WidgetColorProviderSource,
        resolvedSlabFillColor: Color?,
    ): ImageView = requireNotNull(
        inflateSlab(
            context = context,
            radiusDp = 16,
            colorSource = colorSource,
            resolvedSlabFillColor = resolvedSlabFillColor,
        ).findViewById(R.id.widget_card_slab_background),
    )

    private fun exactly(size: Int): Int = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private companion object {
        val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as Unsafe
        }
    }
}
