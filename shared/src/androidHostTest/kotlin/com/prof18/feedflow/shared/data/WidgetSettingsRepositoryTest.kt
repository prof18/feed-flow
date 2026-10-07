package com.prof18.feedflow.shared.data

import app.cash.turbine.test
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetSettingsRepositoryTest : KoinTestBase() {

    @Test
    fun `per-widget values take precedence over legacy defaults`() {
        val settings = MapSettings().apply {
            this["FEED_WIDGET_LAYOUT"] = WidgetFeedLayout.LIST.name
        }
        val repository = WidgetSettingsRepository(settings)

        repository.setWidgetFeedLayout(WIDGET_ID, WidgetFeedLayout.CARD)

        assertEquals(WidgetFeedLayout.CARD, repository.getWidgetConfiguration(WIDGET_ID).feedLayout)
    }

    @Test
    fun `missing per-widget values fall back to legacy defaults`() {
        val settings = MapSettings().apply {
            this["FEED_WIDGET_LAYOUT"] = WidgetFeedLayout.CARD.name
            this["WIDGET_TEXT_COLOR_MODE"] = WidgetTextColorMode.DARK.name
            this["WIDGET_SHOW_REFRESH_BUTTON"] = true
        }

        val configuration = WidgetSettingsRepository(settings).getWidgetConfiguration(WIDGET_ID)

        assertEquals(WidgetFeedLayout.CARD, configuration.feedLayout)
        assertEquals(WidgetTextColorMode.DARK, configuration.textColorMode)
        assertEquals(true, configuration.showRefreshButton)
    }

    @Test
    fun `refresh button is disabled by default and per-widget setting overrides legacy value`() {
        val settings = MapSettings()
        val repository = WidgetSettingsRepository(settings)

        assertEquals(false, repository.getWidgetConfiguration(WIDGET_ID).showRefreshButton)

        settings["WIDGET_SHOW_REFRESH_BUTTON"] = true
        assertEquals(true, repository.getWidgetConfiguration(WIDGET_ID).showRefreshButton)

        repository.setWidgetShowRefreshButton(WIDGET_ID, false)

        assertEquals(false, repository.getWidgetConfiguration(WIDGET_ID).showRefreshButton)
        assertEquals(true, repository.getWidgetConfiguration(OTHER_WIDGET_ID).showRefreshButton)
    }

    @Test
    fun `per-widget default background clears legacy color`() {
        val settings = MapSettings().apply {
            this["WIDGET_BACKGROUND_COLOR"] = 123456
        }
        val repository = WidgetSettingsRepository(settings)

        repository.setWidgetBackgroundColor(WIDGET_ID, null)

        assertNull(repository.getWidgetConfiguration(WIDGET_ID).backgroundColor)
    }

    @Test
    fun `clearing widget configuration restores legacy defaults`() {
        val settings = MapSettings().apply {
            this["FEED_WIDGET_LAYOUT"] = WidgetFeedLayout.LIST.name
        }
        val repository = WidgetSettingsRepository(settings)
        repository.setWidgetFeedLayout(WIDGET_ID, WidgetFeedLayout.CARD)

        repository.clearWidgetConfiguration(WIDGET_ID)

        assertEquals(WidgetFeedLayout.LIST, repository.getWidgetConfiguration(WIDGET_ID).feedLayout)
    }

    @Test
    fun `restoring widget configurations snapshots overlapping IDs and preserves unrelated widgets`() {
        val settings = MapSettings().apply {
            this["FEED_WIDGET_LAYOUT"] = WidgetFeedLayout.LIST.name
        }
        val repository = WidgetSettingsRepository(settings)
        val firstConfiguration = WidgetConfiguration(
            contentFilter = WidgetContentFilter.Category("technology"),
            feedLayout = WidgetFeedLayout.CARD,
            showHeader = false,
            showRefreshButton = true,
            fontScale = 3,
            backgroundColor = 0xFF112233.toInt(),
            backgroundOpacityPercent = 65,
            textColorMode = WidgetTextColorMode.LIGHT,
            hideImages = true,
        )
        val secondConfiguration = WidgetConfiguration(
            contentFilter = WidgetContentFilter.Bookmarks,
            feedLayout = WidgetFeedLayout.LIST,
            showHeader = true,
            showRefreshButton = false,
            fontScale = 1,
            backgroundColor = null,
            backgroundOpacityPercent = 90,
            textColorMode = WidgetTextColorMode.DARK,
            hideImages = false,
        )
        val unrelatedConfiguration = secondConfiguration.copy(feedLayout = WidgetFeedLayout.CARD)
        setConfiguration(repository, OLD_WIDGET_ID, firstConfiguration)
        setConfiguration(repository, OVERLAPPING_WIDGET_ID, secondConfiguration)
        setConfiguration(repository, UNRELATED_WIDGET_ID, unrelatedConfiguration)

        repository.restoreWidgetConfigurations(
            oldWidgetIds = intArrayOf(OLD_WIDGET_ID, OVERLAPPING_WIDGET_ID),
            newWidgetIds = intArrayOf(OVERLAPPING_WIDGET_ID, NEW_WIDGET_ID),
        )

        assertEquals(firstConfiguration, repository.getWidgetConfiguration(OVERLAPPING_WIDGET_ID))
        assertEquals(secondConfiguration, repository.getWidgetConfiguration(NEW_WIDGET_ID))
        assertEquals(unrelatedConfiguration, repository.getWidgetConfiguration(UNRELATED_WIDGET_ID))
        assertNull(settings.getStringOrNull("FEED_WIDGET_LAYOUT_$OLD_WIDGET_ID"))
        assertNull(settings.getStringOrNull("WIDGET_CONTENT_FILTER_$OLD_WIDGET_ID"))
        assertEquals(WidgetFeedLayout.LIST, repository.getWidgetConfiguration(OLD_WIDGET_ID).feedLayout)
    }

    @Test
    fun `restoring widget configuration snapshots legacy fallback values`() {
        val settings = MapSettings().apply {
            this["FEED_WIDGET_LAYOUT"] = WidgetFeedLayout.CARD.name
            this["WIDGET_SHOW_HEADER"] = false
            this["WIDGET_SHOW_REFRESH_BUTTON"] = true
            this["WIDGET_FONT_SCALE_FACTOR"] = 4
            this["WIDGET_BACKGROUND_COLOR"] = 0xFF345678.toInt()
            this["WIDGET_BACKGROUND_OPACITY_PERCENT"] = 72
            this["WIDGET_TEXT_COLOR_MODE"] = WidgetTextColorMode.LIGHT.name
            this["WIDGET_HIDE_IMAGES"] = true
        }
        val repository = WidgetSettingsRepository(settings)
        val expectedConfiguration = repository.getWidgetConfiguration(OLD_WIDGET_ID)

        repository.restoreWidgetConfigurations(
            oldWidgetIds = intArrayOf(OLD_WIDGET_ID),
            newWidgetIds = intArrayOf(NEW_WIDGET_ID),
        )

        settings["FEED_WIDGET_LAYOUT"] = WidgetFeedLayout.LIST.name
        settings["WIDGET_SHOW_HEADER"] = true
        settings["WIDGET_SHOW_REFRESH_BUTTON"] = false
        settings["WIDGET_FONT_SCALE_FACTOR"] = 0
        settings.remove("WIDGET_BACKGROUND_COLOR")
        settings["WIDGET_BACKGROUND_OPACITY_PERCENT"] = 100
        settings["WIDGET_TEXT_COLOR_MODE"] = WidgetTextColorMode.AUTOMATIC.name
        settings["WIDGET_HIDE_IMAGES"] = false

        assertEquals(expectedConfiguration, repository.getWidgetConfiguration(NEW_WIDGET_ID))
    }

    @Test
    fun `configuration observer emits the complete restored configuration`() = runTest(testDispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())
        val configuration = WidgetConfiguration(
            contentFilter = WidgetContentFilter.Bookmarks,
            feedLayout = WidgetFeedLayout.CARD,
            showHeader = false,
            fontScale = 2,
            backgroundColor = 0xFFABCDEF.toInt(),
            backgroundOpacityPercent = 80,
            textColorMode = WidgetTextColorMode.LIGHT,
            hideImages = true,
        )
        setConfiguration(repository, OLD_WIDGET_ID, configuration)

        repository.observeWidgetConfiguration(NEW_WIDGET_ID).test {
            awaitItem()

            repository.restoreWidgetConfigurations(
                oldWidgetIds = intArrayOf(OLD_WIDGET_ID),
                newWidgetIds = intArrayOf(NEW_WIDGET_ID),
            )

            assertEquals(configuration, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `restoring empty and unchanged widget ID mappings is safe`() {
        val repository = WidgetSettingsRepository(MapSettings())
        repository.setWidgetFeedLayout(WIDGET_ID, WidgetFeedLayout.CARD)

        repository.restoreWidgetConfigurations(intArrayOf(), intArrayOf())
        repository.restoreWidgetConfigurations(intArrayOf(WIDGET_ID), intArrayOf(WIDGET_ID))

        assertEquals(WidgetFeedLayout.CARD, repository.getWidgetConfiguration(WIDGET_ID).feedLayout)
    }

    @Test
    fun `content filter round trips through repository`() {
        val repository = WidgetSettingsRepository(MapSettings())
        val filter = WidgetContentFilter.Category("technology")

        repository.setWidgetContentFilter(WIDGET_ID, filter)

        assertEquals(filter, repository.getWidgetConfiguration(WIDGET_ID).contentFilter)
    }

    @Test
    fun `widget configurations remain isolated when one is cleared`() {
        val settings = MapSettings().apply {
            this["FEED_WIDGET_LAYOUT"] = WidgetFeedLayout.LIST.name
        }
        val repository = WidgetSettingsRepository(settings)
        repository.setWidgetFeedLayout(WIDGET_ID, WidgetFeedLayout.CARD)
        repository.setWidgetContentFilter(WIDGET_ID, WidgetContentFilter.Category("technology"))
        repository.setWidgetFeedLayout(OTHER_WIDGET_ID, WidgetFeedLayout.LIST)
        repository.setWidgetContentFilter(OTHER_WIDGET_ID, WidgetContentFilter.Bookmarks)
        repository.setWidgetHideImages(OTHER_WIDGET_ID, true)

        assertEquals(WidgetFeedLayout.CARD, repository.getWidgetConfiguration(WIDGET_ID).feedLayout)
        assertEquals(
            WidgetContentFilter.Category("technology"),
            repository.getWidgetConfiguration(WIDGET_ID).contentFilter,
        )
        assertEquals(
            WidgetContentFilter.Bookmarks,
            repository.getWidgetConfiguration(OTHER_WIDGET_ID).contentFilter,
        )

        repository.clearWidgetConfiguration(WIDGET_ID)

        assertEquals(WidgetFeedLayout.LIST, repository.getWidgetConfiguration(WIDGET_ID).feedLayout)
        assertEquals(WidgetContentFilter.Timeline, repository.getWidgetConfiguration(WIDGET_ID).contentFilter)
        assertEquals(
            WidgetContentFilter.Bookmarks,
            repository.getWidgetConfiguration(OTHER_WIDGET_ID).contentFilter,
        )
        assertEquals(true, repository.getWidgetConfiguration(OTHER_WIDGET_ID).hideImages)
    }

    @Test
    fun `configuration observer emits initial state and every setting update`() = runTest(testDispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())

        repository.observeWidgetConfiguration(WIDGET_ID).test {
            assertEquals(repository.getWidgetConfiguration(WIDGET_ID), awaitItem())

            repository.setWidgetContentFilter(WIDGET_ID, WidgetContentFilter.Bookmarks)
            assertEquals(WidgetContentFilter.Bookmarks, awaitItem().contentFilter)

            repository.setWidgetFeedLayout(WIDGET_ID, WidgetFeedLayout.CARD)
            assertEquals(WidgetFeedLayout.CARD, awaitItem().feedLayout)

            repository.setWidgetShowHeader(WIDGET_ID, false)
            assertEquals(false, awaitItem().showHeader)

            repository.setWidgetShowRefreshButton(WIDGET_ID, true)
            assertEquals(true, awaitItem().showRefreshButton)

            repository.setWidgetFontScaleFactor(WIDGET_ID, 2)
            assertEquals(2, awaitItem().fontScale)

            repository.setWidgetBackgroundColor(WIDGET_ID, 123456)
            assertEquals(123456, awaitItem().backgroundColor)

            repository.setWidgetBackgroundOpacityPercent(WIDGET_ID, 70)
            assertEquals(70, awaitItem().backgroundOpacityPercent)

            repository.setWidgetTextColorMode(WIDGET_ID, WidgetTextColorMode.DARK)
            assertEquals(WidgetTextColorMode.DARK, awaitItem().textColorMode)

            repository.setWidgetHideImages(WIDGET_ID, true)
            assertEquals(true, awaitItem().hideImages)

            repository.clearWidgetConfiguration(WIDGET_ID)
            val clearedConfiguration = awaitItem()
            assertEquals(WidgetContentFilter.Timeline, clearedConfiguration.contentFilter)
            assertEquals(WidgetFeedLayout.LIST, clearedConfiguration.feedLayout)
            assertEquals(true, clearedConfiguration.showHeader)
            assertEquals(false, clearedConfiguration.showRefreshButton)
            assertEquals(0, clearedConfiguration.fontScale)
            assertNull(clearedConfiguration.backgroundColor)
            assertEquals(100, clearedConfiguration.backgroundOpacityPercent)
            assertEquals(WidgetTextColorMode.AUTOMATIC, clearedConfiguration.textColorMode)
            assertEquals(false, clearedConfiguration.hideImages)
        }
    }

    @Test
    fun `configuration observer ignores updates for other widgets`() = runTest(testDispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())

        repository.observeWidgetConfiguration(WIDGET_ID).test {
            awaitItem()

            repository.setWidgetHideImages(OTHER_WIDGET_ID, true)

            expectNoEvents()
        }
    }

    @Test
    fun `configuration observer reads latest persisted state when collection starts`() = runTest(testDispatcher) {
        val repository = WidgetSettingsRepository(MapSettings())
        repository.setWidgetFeedLayout(WIDGET_ID, WidgetFeedLayout.CARD)

        repository.observeWidgetConfiguration(WIDGET_ID).test {
            assertEquals(WidgetFeedLayout.CARD, awaitItem().feedLayout)
        }
    }

    private companion object {
        const val WIDGET_ID = 42
        const val OTHER_WIDGET_ID = 84
        const val OLD_WIDGET_ID = 100
        const val OVERLAPPING_WIDGET_ID = 101
        const val NEW_WIDGET_ID = 102
        const val UNRELATED_WIDGET_ID = 103
    }

    private fun setConfiguration(
        repository: WidgetSettingsRepository,
        widgetId: Int,
        configuration: WidgetConfiguration,
    ) {
        repository.setWidgetContentFilter(widgetId, configuration.contentFilter)
        repository.setWidgetFeedLayout(widgetId, configuration.feedLayout)
        repository.setWidgetShowHeader(widgetId, configuration.showHeader)
        repository.setWidgetShowRefreshButton(widgetId, configuration.showRefreshButton)
        repository.setWidgetFontScaleFactor(widgetId, configuration.fontScale)
        repository.setWidgetBackgroundColor(widgetId, configuration.backgroundColor)
        repository.setWidgetBackgroundOpacityPercent(widgetId, configuration.backgroundOpacityPercent)
        repository.setWidgetTextColorMode(widgetId, configuration.textColorMode)
        repository.setWidgetHideImages(widgetId, configuration.hideImages)
    }
}
