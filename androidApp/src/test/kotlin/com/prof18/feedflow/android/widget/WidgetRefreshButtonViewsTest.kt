package com.prof18.feedflow.android.widget

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.android.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WidgetRefreshButtonViewsTest {
    @Test
    fun `partial feedback swaps the button to loading and back without a full widget render`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = widgetRefreshButtonViews(context, Color.WHITE, "Refresh feeds", isRefreshing = false)
        val view = views.apply(context, FrameLayout(context))
        val icon = view.findViewById<ImageView>(R.id.widget_refresh_icon)
        val progress = view.findViewById<ProgressBar>(R.id.widget_refresh_progress)
        assertEquals(View.VISIBLE, icon.visibility)
        assertEquals(View.GONE, progress.visibility)

        widgetRefreshButtonFeedback(context, isRefreshing = true).reapply(context, view)

        assertEquals(View.GONE, icon.visibility)
        assertEquals(View.VISIBLE, progress.visibility)
        assertEquals(Color.WHITE, progress.indeterminateTintList?.defaultColor)

        widgetRefreshButtonFeedback(context, isRefreshing = false).reapply(context, view)

        assertEquals(View.VISIBLE, icon.visibility)
        assertEquals(View.GONE, progress.visibility)
    }
}
