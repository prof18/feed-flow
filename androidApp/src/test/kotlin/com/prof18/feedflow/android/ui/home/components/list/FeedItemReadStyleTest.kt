package com.prof18.feedflow.android.ui.home.components.list

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.prof18.feedflow.android.ui.preview.feedItemsForPreview
import com.prof18.feedflow.android.ui.style.DarkColorScheme
import com.prof18.feedflow.android.ui.style.LightColorScheme
import com.prof18.feedflow.android.ui.style.OledColorScheme
import com.prof18.feedflow.core.model.FeedFilter
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedItemReadStyleTest {

    private val schemes = mapOf(
        "light" to LightColorScheme,
        "dark" to DarkColorScheme,
        "oled" to OledColorScheme,
    )

    // LIST rows sit on `surface`, CARD / BIG_IMAGE / GRID rows on `surfaceContainerLowest` cards.
    private fun ColorScheme.feedBackgrounds(): Map<String, Color> = mapOf(
        "surface" to surface,
        "surfaceContainerLowest" to surfaceContainerLowest,
    )

    @Test
    fun `read text keeps WCAG AA contrast on every feed background`() {
        for ((schemeName, scheme) in schemes) {
            for ((backgroundName, background) in scheme.feedBackgrounds()) {
                val readText = readTextColor(scheme.onSurface).compositeOver(background)
                val contrast = contrastRatio(readText, background)
                assertTrue(
                    contrast >= MIN_TEXT_CONTRAST,
                    "$schemeName read text on $backgroundName has contrast $contrast",
                )
            }
        }
    }

    @Test
    fun `read title is clearly fainter than unread title`() {
        for ((schemeName, scheme) in schemes) {
            for ((backgroundName, background) in scheme.feedBackgrounds()) {
                val unreadContrast = contrastRatio(scheme.onSurface, background)
                val readContrast = contrastRatio(
                    readTextColor(scheme.onSurface).compositeOver(background),
                    background,
                )
                assertTrue(
                    readContrast <= unreadContrast * MAX_READ_TO_UNREAD_CONTRAST,
                    "$schemeName on $backgroundName: read $readContrast vs unread $unreadContrast",
                )
            }
        }
    }

    @Test
    fun `read items are dimmed in mixed feeds`() {
        val readItem = feedItemsForPreview.first().copy(isRead = true)

        assertTrue(readItem.isDimmedAsRead(FeedFilter.Timeline))
        assertEquals(ReadImageAlpha, readItem.imageAlpha(FeedFilter.Timeline))
        assertEquals(ReadImageSaturation, readItem.imageSaturation(FeedFilter.Timeline))
    }

    @Test
    fun `read items are not dimmed in the Read and Bookmarks views`() {
        val readItem = feedItemsForPreview.first().copy(isRead = true)

        for (filter in listOf(FeedFilter.Read, FeedFilter.Bookmarks)) {
            assertFalse(readItem.isDimmedAsRead(filter))
            assertEquals(1f, readItem.imageAlpha(filter))
            assertEquals(1f, readItem.imageSaturation(filter))
        }
    }

    @Test
    fun `unread items are never dimmed`() {
        val unreadItem = feedItemsForPreview.first().copy(isRead = false)

        assertFalse(unreadItem.isDimmedAsRead(FeedFilter.Timeline))
        assertEquals(1f, unreadItem.imageAlpha(FeedFilter.Timeline))
        assertEquals(1f, unreadItem.imageSaturation(FeedFilter.Timeline))
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val first = foreground.luminance()
        val second = background.luminance()
        return (max(first, second) + 0.05f) / (min(first, second) + 0.05f)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        const val MAX_READ_TO_UNREAD_CONTRAST = 0.6f
    }
}
