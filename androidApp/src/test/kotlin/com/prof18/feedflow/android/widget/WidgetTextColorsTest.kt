package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTextColorsTest {

    @Test
    fun `widgetTextColorsForBackground uses dark text on light backgrounds`() {
        val colors = widgetTextColorsForBackground(Color(0xFFF5F5F5))

        assertEquals(Color.Black, colors.primary)
        assertEquals(Color.Black.copy(alpha = 0.72f), colors.secondary)
    }

    @Test
    fun `widgetTextColorsForBackground uses light text on dark backgrounds`() {
        val colors = widgetTextColorsForBackground(Color(0xFF111111))

        assertEquals(Color.White, colors.primary)
        assertEquals(Color.White.copy(alpha = 0.72f), colors.secondary)
    }

    @Test
    fun `widgetEffectiveBackgroundColor lightens dark color over light underlay`() {
        val effectiveBackground = widgetEffectiveBackgroundColor(
            backgroundColor = Color(0xFF000000),
            backgroundOpacity = 0.2f,
            underlayColor = Color(0xFFFFFFFF),
        )

        val colors = widgetTextColorsForBackground(effectiveBackground)

        assertEquals(Color.Black, colors.primary)
    }

    @Test
    fun `widgetTextColorsForMode returns light override colors`() {
        val colors = widgetTextColorsForMode(
            textColorMode = WidgetTextColorMode.LIGHT,
            backgroundColor = Color(0xFFF5F5F5),
        )

        assertEquals(Color.White, colors.primary)
        assertEquals(Color.White.copy(alpha = 0.72f), colors.secondary)
    }

    @Test
    fun `widgetTextColorsForMode returns dark override colors`() {
        val colors = widgetTextColorsForMode(
            textColorMode = WidgetTextColorMode.DARK,
            backgroundColor = Color(0xFF111111),
        )

        assertEquals(Color.Black, colors.primary)
        assertEquals(Color.Black.copy(alpha = 0.72f), colors.secondary)
    }
}
