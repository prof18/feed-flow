package com.prof18.feedflow.shared.domain.widget

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetDateSettingsStoreTest {

    @Test
    fun `default formats are used until the app saves its settings`() {
        assertEquals(DEFAULT_SETTINGS, WidgetDateSettingsStore(testDefaults()).read())
    }

    @Test
    fun `settings count as saved only after a save`() {
        val store = WidgetDateSettingsStore(testDefaults())
        assertFalse(store.hasSavedSettings())

        store.save(WidgetDateSettings(dateFormat = DateFormat.ISO, timeFormat = TimeFormat.HOURS_12))

        assertTrue(store.hasSavedSettings())
        assertEquals(WidgetDateSettings(dateFormat = DateFormat.ISO, timeFormat = TimeFormat.HOURS_12), store.read())
    }

    @Test
    fun `saved settings are read back`() {
        val store = WidgetDateSettingsStore(testDefaults())
        val settings = WidgetDateSettings(
            dateFormat = DateFormat.ISO,
            timeFormat = TimeFormat.HOURS_12,
        )

        store.save(settings)

        assertEquals(settings, store.read())
    }

    @Test
    fun `an unknown saved format falls back to the default`() {
        val defaults = testDefaults()
        WidgetDateSettingsStore(defaults).save(
            WidgetDateSettings(dateFormat = DateFormat.ISO, timeFormat = TimeFormat.HOURS_12),
        )
        defaults.setObject("SOMETHING_NEW", forKey = "widget_date_format")

        assertEquals(
            WidgetDateSettings(dateFormat = DateFormat.NORMAL, timeFormat = TimeFormat.HOURS_12),
            WidgetDateSettingsStore(defaults).read(),
        )
    }

    private companion object {
        val DEFAULT_SETTINGS = WidgetDateSettings(dateFormat = DateFormat.NORMAL, timeFormat = TimeFormat.HOURS_24)
    }

    private fun testDefaults(): NSUserDefaults =
        NSUserDefaults(suiteName = "widget-date-test-${NSUUID().UUIDString}")
}
