package com.prof18.feedflow.android.widget

import androidx.glance.appwidget.SizeMode
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import sun.misc.Unsafe

class FeedFlowWidgetTest {

    @Test
    fun `widget uses exact size mode`() {
        val widget = FeedFlowWidget(
            repository = instanceWithoutConstructor(FeedWidgetRepository::class.java),
            widgetSettingsRepository = instanceWithoutConstructor(WidgetSettingsRepository::class.java),
            browserManager = instanceWithoutConstructor(BrowserManager::class.java),
        )

        assertEquals(SizeMode.Exact, widget.sizeMode)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> instanceWithoutConstructor(type: Class<T>): T =
        unsafe.allocateInstance(type) as T

    private companion object {
        val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as Unsafe
        }
    }
}
