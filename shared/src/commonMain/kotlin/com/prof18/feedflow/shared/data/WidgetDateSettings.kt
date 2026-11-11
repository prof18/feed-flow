package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat

/**
 * Copies the date and time formats to where the iOS widget can read them. The other platforms'
 * widgets read the app settings directly, so there it does nothing.
 */
internal expect fun saveWidgetDateSettings(dateFormat: DateFormat, timeFormat: TimeFormat)
