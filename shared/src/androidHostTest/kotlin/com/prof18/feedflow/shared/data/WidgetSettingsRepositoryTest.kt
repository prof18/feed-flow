package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetCardImageSizing
import com.prof18.feedflow.shared.domain.model.WidgetCardItemSeparation
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetSettingsRepositoryTest {

    @Test
    fun `absent card appearance settings use defaults`() {
        val repository = WidgetSettingsRepository(MapSettings())

        assertEquals(WidgetCardAppearance(), repository.getWidgetCardAppearance())
        assertEquals(WidgetCardAppearance(), repository.widgetCardAppearance.value)
    }

    @Test
    fun `card appearance round trips every field and emits the update`() = runTest {
        val repository = WidgetSettingsRepository(MapSettings())
        val appearance = WidgetCardAppearance(
            surfaceColor = 0xFF123456.toInt(),
            surfaceOpacityPercent = 70,
            cornerRadiusDp = 24,
            itemSeparation = WidgetCardItemSeparation.DIVIDER,
            dividerOpacityPercent = 45,
            imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
        )

        repository.widgetCardAppearance.test {
            assertEquals(WidgetCardAppearance(), awaitItem())

            repository.setWidgetCardAppearance(appearance)

            assertEquals(appearance, awaitItem())
            assertEquals(appearance, repository.getWidgetCardAppearance())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `null surface color removes the stored custom color`() {
        val settings = MapSettings()
        val repository = WidgetSettingsRepository(settings)
        repository.setWidgetCardAppearance(
            WidgetCardAppearance(surfaceColor = 0xFF123456.toInt()),
        )

        repository.setWidgetCardAppearance(WidgetCardAppearance(surfaceColor = null))

        assertNull(settings.getIntOrNull(WIDGET_CARD_SURFACE_COLOR_KEY))
        assertNull(repository.getWidgetCardAppearance().surfaceColor)
        assertNull(repository.widgetCardAppearance.value.surfaceColor)
    }

    @Test
    fun `custom surface color is stored as opaque RGB`() {
        val settings = MapSettings()
        val repository = WidgetSettingsRepository(settings)

        repository.setWidgetCardAppearance(
            WidgetCardAppearance(surfaceColor = 0x12123456),
        )

        assertEquals(0xFF123456.toInt(), settings.getIntOrNull(WIDGET_CARD_SURFACE_COLOR_KEY))
        assertEquals(0xFF123456.toInt(), repository.getWidgetCardAppearance().surfaceColor)
        assertEquals(0xFF123456.toInt(), repository.widgetCardAppearance.value.surfaceColor)
    }

    @Test
    fun `card appearance clamps opacities and rounds odd radius upward`() {
        val settings = MapSettings()
        val repository = WidgetSettingsRepository(settings)

        repository.setWidgetCardAppearance(
            WidgetCardAppearance(
                surfaceOpacityPercent = 101,
                cornerRadiusDp = 17,
                dividerOpacityPercent = -1,
            ),
        )

        val expected = WidgetCardAppearance(
            surfaceOpacityPercent = 100,
            cornerRadiusDp = 18,
            dividerOpacityPercent = 0,
        )
        assertEquals(100, settings.getInt(WIDGET_CARD_SURFACE_OPACITY_PERCENT_KEY, -1))
        assertEquals(18, settings.getInt(WIDGET_CARD_CORNER_RADIUS_DP_KEY, -1))
        assertEquals(0, settings.getInt(WIDGET_CARD_DIVIDER_OPACITY_PERCENT_KEY, -1))
        assertEquals(expected, repository.getWidgetCardAppearance())
        assertEquals(expected, repository.widgetCardAppearance.value)
    }

    @Test
    fun `card appearance clamps radius and opacity at both boundaries`() {
        val repository = WidgetSettingsRepository(MapSettings())

        repository.setWidgetCardAppearance(
            WidgetCardAppearance(
                surfaceOpacityPercent = -1,
                cornerRadiusDp = -1,
                dividerOpacityPercent = 101,
            ),
        )
        assertEquals(0, repository.getWidgetCardAppearance().surfaceOpacityPercent)
        assertEquals(0, repository.getWidgetCardAppearance().cornerRadiusDp)
        assertEquals(100, repository.getWidgetCardAppearance().dividerOpacityPercent)

        repository.setWidgetCardAppearance(WidgetCardAppearance(cornerRadiusDp = 33))
        assertEquals(32, repository.getWidgetCardAppearance().cornerRadiusDp)
    }

    @Test
    fun `preloaded card appearance values are normalized on read and initial emission`() {
        val normalizedRadiusByStoredValue = mapOf(
            17 to 18,
            -1 to 0,
            33 to 32,
        )

        normalizedRadiusByStoredValue.forEach { (storedRadius, normalizedRadius) ->
            val settings = MapSettings().apply {
                this[WIDGET_CARD_SURFACE_COLOR_KEY] = 0x12123456
                this[WIDGET_CARD_SURFACE_OPACITY_PERCENT_KEY] = 101
                this[WIDGET_CARD_CORNER_RADIUS_DP_KEY] = storedRadius
                this[WIDGET_CARD_DIVIDER_OPACITY_PERCENT_KEY] = -1
            }
            val repository = WidgetSettingsRepository(settings)
            val expected = WidgetCardAppearance(
                surfaceColor = 0xFF123456.toInt(),
                surfaceOpacityPercent = 100,
                cornerRadiusDp = normalizedRadius,
                dividerOpacityPercent = 0,
            )

            assertEquals(expected, repository.getWidgetCardAppearance())
            assertEquals(expected, repository.widgetCardAppearance.value)
        }
    }

    @Test
    fun `competing card appearance writes keep persisted and emitted snapshots coherent`() = runTest {
        val firstAppearance = WidgetCardAppearance(
            surfaceColor = 0xFF112233.toInt(),
            surfaceOpacityPercent = 20,
            cornerRadiusDp = 10,
            itemSeparation = WidgetCardItemSeparation.SPACING,
            dividerOpacityPercent = 30,
            imageSizing = WidgetCardImageSizing.THUMBNAIL,
        )
        val secondAppearance = WidgetCardAppearance(
            surfaceColor = 0xFF445566.toInt(),
            surfaceOpacityPercent = 80,
            cornerRadiusDp = 28,
            itemSeparation = WidgetCardItemSeparation.NONE,
            dividerOpacityPercent = 70,
            imageSizing = WidgetCardImageSizing.FILL_ROW_HEIGHT,
        )
        val settings = CoordinatedSettings(
            firstWriterColor = firstAppearance.surfaceColor,
            secondWriterImageSizing = secondAppearance.imageSizing.name,
        )
        val repository = WidgetSettingsRepository(settings)

        val firstWriter = launch(Dispatchers.Default) {
            repository.setWidgetCardAppearance(firstAppearance)
        }
        assertTrue(settings.awaitFirstWriterColor())

        val secondWriter = launch(Dispatchers.Default) {
            repository.setWidgetCardAppearance(secondAppearance)
        }
        val secondWriterInterleaved = settings.awaitSecondWriterFinalWrite(
            timeout = 1,
            unit = TimeUnit.SECONDS,
        )
        if (secondWriterInterleaved) {
            settings.releaseSecondWriter()
            secondWriter.join()
            settings.releaseFirstWriter()
        } else {
            settings.releaseFirstWriter()
            firstWriter.join()
            assertTrue(settings.awaitSecondWriterFinalWrite())
            settings.releaseSecondWriter()
        }
        firstWriter.join()
        secondWriter.join()

        assertEquals(repository.getWidgetCardAppearance(), repository.widgetCardAppearance.value)
    }

    @Test
    fun `malformed card appearance enums fall back to defaults`() {
        val settings = MapSettings().apply {
            this[WIDGET_CARD_ITEM_SEPARATION_KEY] = "malformed"
            this[WIDGET_CARD_IMAGE_SIZING_KEY] = "malformed"
        }

        val repository = WidgetSettingsRepository(settings)

        assertEquals(WidgetCardItemSeparation.SPACING, repository.getWidgetCardAppearance().itemSeparation)
        assertEquals(WidgetCardImageSizing.THUMBNAIL, repository.getWidgetCardAppearance().imageSizing)
    }

    private class CoordinatedSettings(
        private val firstWriterColor: Int?,
        private val secondWriterImageSizing: String,
        private val delegate: Settings = MapSettings(),
    ) : Settings by delegate {
        private val firstWriterColorStored = CountDownLatch(1)
        private val releaseFirstWriterLatch = CountDownLatch(1)
        private val secondWriterFinalWriteStored = CountDownLatch(1)
        private val releaseSecondWriterLatch = CountDownLatch(1)

        override fun putInt(key: String, value: Int) {
            delegate.putInt(key, value)
            if (key == WIDGET_CARD_SURFACE_COLOR_KEY && value == firstWriterColor) {
                firstWriterColorStored.countDown()
                check(releaseFirstWriterLatch.await(5, TimeUnit.SECONDS))
            }
        }

        override fun putString(key: String, value: String) {
            delegate.putString(key, value)
            if (key == WIDGET_CARD_IMAGE_SIZING_KEY && value == secondWriterImageSizing) {
                secondWriterFinalWriteStored.countDown()
                check(releaseSecondWriterLatch.await(5, TimeUnit.SECONDS))
            }
        }

        fun awaitFirstWriterColor(): Boolean =
            firstWriterColorStored.await(5, TimeUnit.SECONDS)

        fun awaitSecondWriterFinalWrite(
            timeout: Long = 5,
            unit: TimeUnit = TimeUnit.SECONDS,
        ): Boolean = secondWriterFinalWriteStored.await(timeout, unit)

        fun releaseFirstWriter() {
            releaseFirstWriterLatch.countDown()
        }

        fun releaseSecondWriter() {
            releaseSecondWriterLatch.countDown()
        }
    }

    private companion object {
        const val WIDGET_CARD_SURFACE_COLOR_KEY = "WIDGET_CARD_SURFACE_COLOR"
        const val WIDGET_CARD_SURFACE_OPACITY_PERCENT_KEY = "WIDGET_CARD_SURFACE_OPACITY_PERCENT"
        const val WIDGET_CARD_CORNER_RADIUS_DP_KEY = "WIDGET_CARD_CORNER_RADIUS_DP"
        const val WIDGET_CARD_ITEM_SEPARATION_KEY = "WIDGET_CARD_ITEM_SEPARATION"
        const val WIDGET_CARD_DIVIDER_OPACITY_PERCENT_KEY = "WIDGET_CARD_DIVIDER_OPACITY_PERCENT"
        const val WIDGET_CARD_IMAGE_SIZING_KEY = "WIDGET_CARD_IMAGE_SIZING"
    }
}
