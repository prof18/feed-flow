package com.prof18.feedflow.shared.domain.widget

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.core.utils.IOS_APP_GROUP
import platform.Foundation.NSUserDefaults

/**
 * The app's settings live in the Keychain, which the widget extension can't read. The app copies
 * the date and time formats to the app-group defaults whenever they change, so the widget can format
 * dates like the app does.
 */
class WidgetDateSettingsStore(
    private val userDefaults: NSUserDefaults? = NSUserDefaults(suiteName = IOS_APP_GROUP),
) {
    /** False for installs that picked their formats before the copy existed. */
    fun hasSavedSettings(): Boolean = userDefaults?.stringForKey(DATE_FORMAT_KEY) != null

    fun save(settings: WidgetDateSettings) {
        val defaults = userDefaults ?: return
        defaults.setObject(settings.dateFormat.name, forKey = DATE_FORMAT_KEY)
        defaults.setObject(settings.timeFormat.name, forKey = TIME_FORMAT_KEY)
    }

    /** Falls back to the app's default formats until the app has saved its settings. */
    fun read(): WidgetDateSettings {
        val dateFormat = userDefaults?.stringForKey(DATE_FORMAT_KEY)
            ?.let { name -> DateFormat.entries.firstOrNull { it.name == name } }
            ?: DateFormat.NORMAL
        val timeFormat = userDefaults?.stringForKey(TIME_FORMAT_KEY)
            ?.let { name -> TimeFormat.entries.firstOrNull { it.name == name } }
            ?: TimeFormat.HOURS_24
        return WidgetDateSettings(dateFormat = dateFormat, timeFormat = timeFormat)
    }

    private companion object {
        const val DATE_FORMAT_KEY = "widget_date_format"
        const val TIME_FORMAT_KEY = "widget_time_format"
    }
}

data class WidgetDateSettings(
    val dateFormat: DateFormat,
    val timeFormat: TimeFormat,
)
