package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPreviewAppearanceResolverTest {

    @Test
    fun `automatic preview contrast composites over themed underlay`() {
        val result = resolveWidgetPreviewAppearance(
            settingsState = WidgetSettingsState(
                backgroundColor = 0xFF000000.toInt(),
                backgroundOpacityPercent = 20,
                textColorMode = WidgetTextColorMode.AUTOMATIC,
            ),
            themedWidgetUnderlayColor = Color.White,
            themedCardSurfaceColor = THEMED_CARD_SURFACE,
            themedOnSurfaceColor = THEMED_ON_SURFACE,
        )

        assertEquals(0.8f, result.effectiveOuterColor.red, 0.01f)
        assertEquals(Color.Black, result.outerTextColors.primary)
    }

    @Test
    fun `compatibility card preview uses themed surface and on-surface for both text roles`() {
        val result = resolveWidgetPreviewAppearance(
            settingsState = WidgetSettingsState(
                feedLayout = WidgetFeedLayout.CARD,
                cardAppearance = WidgetCardAppearance(),
            ),
            themedWidgetUnderlayColor = Color.White,
            themedCardSurfaceColor = THEMED_CARD_SURFACE,
            themedOnSurfaceColor = THEMED_ON_SURFACE,
        )

        assertEquals(THEMED_CARD_SURFACE, result.card.slabFillColor)
        assertEquals(THEMED_ON_SURFACE, result.card.textColors.primary)
        assertEquals(THEMED_ON_SURFACE, result.card.textColors.secondary)
    }

    @Test
    fun `preview normalizes configured card geometry and appearance`() {
        val result = resolveWidgetPreviewAppearance(
            settingsState = WidgetSettingsState(
                feedLayout = WidgetFeedLayout.CARD,
                cardAppearance = WidgetCardAppearance(
                    surfaceColor = 0x00112233,
                    surfaceOpacityPercent = 130,
                    cornerRadiusDp = 25,
                    itemSeparation = WidgetCardItemSeparation.DIVIDER,
                    dividerOpacityPercent = 45,
                    imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
                ),
            ),
            themedWidgetUnderlayColor = Color.White,
            themedCardSurfaceColor = THEMED_CARD_SURFACE,
            themedOnSurfaceColor = THEMED_ON_SURFACE,
        )

        assertEquals(
            WidgetCardAppearance(
                surfaceColor = 0xFF112233.toInt(),
                surfaceOpacityPercent = 100,
                cornerRadiusDp = 26,
                itemSeparation = WidgetCardItemSeparation.DIVIDER,
                dividerOpacityPercent = 45,
                imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            ),
            result.cardAppearance,
        )
    }

    @Test
    fun `preview uses widget scaffold width at fill row fallback boundary`() {
        val previewWidthDp = 240f
        val fullWidthLayout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = previewWidthDp,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )
        val availableSlabWidthDp = calculateWidgetAvailableSlabWidthDp(previewWidthDp)
        val adjustedWidthLayout = resolveWidgetCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            availableSlabWidthDp = availableSlabWidthDp,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )
        val previewLayout = resolveWidgetPreviewCardLayout(
            requestedImageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
            previewWidthDp = previewWidthDp,
            fontSizes = widgetFontSizes(0),
            systemFontScale = 1f,
        )

        assertEquals(WidgetCardImageSizing.FILL_ROW_HEIGHT, fullWidthLayout.imageSizing)
        assertEquals(216f, availableSlabWidthDp, 0f)
        assertEquals(WidgetCardImageSizing.THUMBNAIL, adjustedWidthLayout.imageSizing)
        assertEquals(adjustedWidthLayout, previewLayout)
    }

    private companion object {
        val THEMED_CARD_SURFACE = Color(0xFF1D3A4A)
        val THEMED_ON_SURFACE = Color(0xFFE1F5FE)
    }
}
