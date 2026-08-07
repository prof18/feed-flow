package com.prof18.feedflow.android.widget

import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 31])
class WidgetCardDividerRemoteViewsTest {

    @Test
    fun `themed divider keeps resource aware tint and configured final opacity`() {
        val originalQualifiers = RuntimeEnvironment.getQualifiers()
        val dayDivider: ImageView
        val nightDivider: ImageView
        try {
            RuntimeEnvironment.setQualifiers("+notnight")
            dayDivider = inflateDivider(opacityPercent = 35)
            RuntimeEnvironment.setQualifiers("+night")
            nightDivider = inflateDivider(opacityPercent = 35)
        } finally {
            RuntimeEnvironment.setQualifiers(originalQualifiers)
        }
        val dayTint = dayDivider.imageTintList?.defaultColor
        val nightTint = nightDivider.imageTintList?.defaultColor

        assertNotNull(dayTint)
        assertNotNull(nightTint)
        assertNotEquals(dayTint, nightTint)
        assertEquals(255, Color.alpha(requireNotNull(dayTint)))
        assertEquals(255, Color.alpha(requireNotNull(nightTint)))
        assertEquals(89, dayDivider.imageAlpha)
        assertEquals(89, nightDivider.imageAlpha)
    }

    @Test
    fun `divider image alpha clamps normalized percentage endpoints`() {
        assertEquals(0, widgetCardDividerImageAlpha(-1))
        assertEquals(51, widgetCardDividerImageAlpha(20))
        assertEquals(255, widgetCardDividerImageAlpha(101))
    }

    private fun inflateDivider(opacityPercent: Int): ImageView {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = createThemedWidgetCardDividerRemoteViews(
            context = context,
            opacityPercent = opacityPercent,
        ).apply(context, FrameLayout(context))

        return requireNotNull(root.findViewById(R.id.widget_card_divider))
    }
}
