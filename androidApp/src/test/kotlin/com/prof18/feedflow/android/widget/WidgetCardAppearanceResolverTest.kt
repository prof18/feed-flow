package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetCardAppearanceResolverTest {

    @Test
    fun `default themed opaque automatic appearance preserves themed text roles`() {
        val result = resolveAppearance()

        assertEquals(THEMED_CARD_SURFACE, result.slabFillColor)
        assertEquals(THEMED_CARD_SURFACE, result.effectiveCardColor)
        assertEquals(THEMED_ON_SURFACE, result.textColors.primary)
        assertEquals(THEMED_ON_SURFACE, result.textColors.secondary)
    }

    @Test
    fun `default card selects theme aware Glance providers`() {
        val result = resolveWidgetCardColorProviderPolicy(
            appearance = WidgetCardAppearance(),
            textColorMode = WidgetTextColorMode.AUTOMATIC,
        )

        assertEquals(WidgetColorProviderSource.THEMED, result.surface)
        assertEquals(WidgetColorProviderSource.THEMED, result.primaryText)
        assertEquals(WidgetColorProviderSource.THEMED, result.secondaryText)
        assertEquals(WidgetColorProviderSource.THEMED, result.divider)
    }

    @Test
    fun `custom divider opacity keeps theme aware provider for compatible card colors`() {
        val result = resolveWidgetCardColorProviderPolicy(
            appearance = WidgetCardAppearance(dividerOpacityPercent = 35),
            textColorMode = WidgetTextColorMode.AUTOMATIC,
        )

        assertEquals(WidgetColorProviderSource.THEMED, result.divider)
    }

    @Test
    fun `custom card appearance selects fixed resolved providers`() {
        val customColorResult = resolveWidgetCardColorProviderPolicy(
            appearance = WidgetCardAppearance(surfaceColor = 0xFF123456.toInt()),
            textColorMode = WidgetTextColorMode.AUTOMATIC,
        )
        val customOpacityResult = resolveWidgetCardColorProviderPolicy(
            appearance = WidgetCardAppearance(surfaceOpacityPercent = 50),
            textColorMode = WidgetTextColorMode.AUTOMATIC,
        )

        assertEquals(WidgetColorProviderSource.RESOLVED, customColorResult.surface)
        assertEquals(WidgetColorProviderSource.RESOLVED, customColorResult.primaryText)
        assertEquals(WidgetColorProviderSource.RESOLVED, customColorResult.secondaryText)
        assertEquals(WidgetColorProviderSource.RESOLVED, customColorResult.divider)
        assertEquals(WidgetColorProviderSource.RESOLVED, customOpacityResult.surface)
        assertEquals(WidgetColorProviderSource.RESOLVED, customOpacityResult.primaryText)
        assertEquals(WidgetColorProviderSource.RESOLVED, customOpacityResult.secondaryText)
        assertEquals(WidgetColorProviderSource.RESOLVED, customOpacityResult.divider)
    }

    @Test
    fun `explicit text mode keeps themed surface and selects resolved text providers`() {
        val result = resolveWidgetCardColorProviderPolicy(
            appearance = WidgetCardAppearance(),
            textColorMode = WidgetTextColorMode.DARK,
        )

        assertEquals(WidgetColorProviderSource.THEMED, result.surface)
        assertEquals(WidgetColorProviderSource.RESOLVED, result.primaryText)
        assertEquals(WidgetColorProviderSource.RESOLVED, result.secondaryText)
        assertEquals(WidgetColorProviderSource.RESOLVED, result.divider)
    }

    @Test
    fun `zero opacity emits no slab fill and uses effective outer color`() {
        val result = resolveAppearance(
            appearance = WidgetCardAppearance(surfaceOpacityPercent = 0),
        )

        assertNull(result.slabFillColor)
        assertEquals(result.effectiveOuterColor, result.effectiveCardColor)
        assertEquals(Color.Black, result.textColors.primary)
    }

    @Test
    fun `automatic text uses custom translucent card composited over effective outer background`() {
        val result = resolveAppearance(
            appearance = WidgetCardAppearance(
                surfaceColor = 0xFF000000.toInt(),
                surfaceOpacityPercent = 20,
            ),
            outerSurfaceColor = Color.Black,
            outerSurfaceOpacityPercent = 20,
        )

        assertEquals(0.2f, result.slabFillColor?.alpha ?: -1f, 0.001f)
        assertEquals(0.64f, result.effectiveCardColor.red, 0.01f)
        assertEquals(Color.Black, result.textColors.primary)
    }

    @Test
    fun `light and dark modes override automatic card text`() {
        val light = resolveAppearance(textColorMode = WidgetTextColorMode.LIGHT)
        val dark = resolveAppearance(textColorMode = WidgetTextColorMode.DARK)

        assertEquals(Color.White, light.textColors.primary)
        assertEquals(Color.White.copy(alpha = 0.72f), light.textColors.secondary)
        assertEquals(Color.Black, dark.textColors.primary)
        assertEquals(Color.Black.copy(alpha = 0.72f), dark.textColors.secondary)
    }

    @Test
    fun `divider uses secondary RGB with configured percentage as final alpha`() {
        val result = resolveAppearance(
            appearance = WidgetCardAppearance(dividerOpacityPercent = 35),
            textColorMode = WidgetTextColorMode.LIGHT,
        )

        assertEquals(Color.White.copy(alpha = 0.35f), result.dividerColor)
    }

    private fun resolveAppearance(
        appearance: WidgetCardAppearance = WidgetCardAppearance(),
        textColorMode: WidgetTextColorMode = WidgetTextColorMode.AUTOMATIC,
        outerSurfaceColor: Color = Color.White,
        outerSurfaceOpacityPercent: Int = 100,
    ): ResolvedWidgetCardAppearance = resolveWidgetCardAppearance(
        appearance = appearance,
        textColorMode = textColorMode,
        outerSurfaceColor = outerSurfaceColor,
        outerSurfaceOpacityPercent = outerSurfaceOpacityPercent,
        themedWidgetUnderlayColor = Color.White,
        themedCardSurfaceColor = THEMED_CARD_SURFACE,
        themedOnSurfaceColor = THEMED_ON_SURFACE,
    )

    private companion object {
        val THEMED_CARD_SURFACE = Color(0xFF1D3A4A)
        val THEMED_ON_SURFACE = Color(0xFFE1F5FE)
    }
}
