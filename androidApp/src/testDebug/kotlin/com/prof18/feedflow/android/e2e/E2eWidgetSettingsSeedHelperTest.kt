@file:Suppress("MagicNumber")

package com.prof18.feedflow.android.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.russhwolf.settings.SharedPreferencesSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class E2eWidgetSettingsSeedHelperTest {

    @Test
    fun `Android widget profile becomes fallback for widgets placed later`() {
        val sharedPreferences = testSharedPreferences()
        val settings = SharedPreferencesSettings(sharedPreferences)
        val repository = WidgetSettingsRepository(settings)

        E2eWidgetSettingsSeedHelper.applyAndroidWidgetLegacyFallbacks(sharedPreferences)

        val configuration = repository.getWidgetConfiguration(NEW_WIDGET_ID)
        assertEquals(WidgetContentFilter.Timeline, configuration.contentFilter)
        assertEquals(WidgetFeedLayout.CARD, configuration.feedLayout)
        assertTrue(configuration.showHeader)
        assertEquals(2, configuration.fontScale)
        assertEquals(0xFF1E3A5F.toInt(), configuration.backgroundColor)
        assertEquals(85, configuration.backgroundOpacityPercent)
        assertEquals(WidgetTextColorMode.LIGHT, configuration.textColorMode)
        assertFalse(configuration.hideImages)
        assertEquals(0xFF1E3A5F.toInt(), settings.getInt("WIDGET_BACKGROUND_COLOR", 0))
    }

    @Test
    fun `reset restores legacy defaults and leaves instance overrides for reset loop`() {
        val sharedPreferences = testSharedPreferences()
        val settings = SharedPreferencesSettings(sharedPreferences)
        val repository = WidgetSettingsRepository(settings)
        E2eWidgetSettingsSeedHelper.applyAndroidWidgetLegacyFallbacks(sharedPreferences)
        repository.setWidgetFeedLayout(OVERRIDDEN_WIDGET_ID, WidgetFeedLayout.CARD)
        repository.setWidgetBackgroundColor(OVERRIDDEN_WIDGET_ID, 0xFF112233.toInt())

        E2eWidgetSettingsSeedHelper.resetLegacyFallbacks(sharedPreferences)

        val fallbackConfiguration = repository.getWidgetConfiguration(NEW_WIDGET_ID)
        assertEquals(WidgetFeedLayout.LIST, fallbackConfiguration.feedLayout)
        assertTrue(fallbackConfiguration.showHeader)
        assertEquals(0, fallbackConfiguration.fontScale)
        assertNull(fallbackConfiguration.backgroundColor)
        assertEquals(100, fallbackConfiguration.backgroundOpacityPercent)
        assertEquals(WidgetTextColorMode.AUTOMATIC, fallbackConfiguration.textColorMode)
        assertFalse(fallbackConfiguration.hideImages)
        assertFalse(sharedPreferences.contains("WIDGET_BACKGROUND_COLOR"))

        val overriddenConfiguration = repository.getWidgetConfiguration(OVERRIDDEN_WIDGET_ID)
        assertEquals(WidgetFeedLayout.CARD, overriddenConfiguration.feedLayout)
        assertEquals(0xFF112233.toInt(), overriddenConfiguration.backgroundColor)

        repository.clearWidgetConfiguration(OVERRIDDEN_WIDGET_ID)

        val resetConfiguration = repository.getWidgetConfiguration(OVERRIDDEN_WIDGET_ID)
        assertEquals(WidgetFeedLayout.LIST, resetConfiguration.feedLayout)
        assertNull(resetConfiguration.backgroundColor)
    }

    private companion object {
        const val NEW_WIDGET_ID = 42
        const val OVERRIDDEN_WIDGET_ID = 84
    }

    private fun testSharedPreferences() =
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("e2e_widget_seed_test", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
}
