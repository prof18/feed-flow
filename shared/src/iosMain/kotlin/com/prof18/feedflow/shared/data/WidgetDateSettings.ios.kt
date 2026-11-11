package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.domain.widget.WidgetDateSettings
import com.prof18.feedflow.shared.domain.widget.WidgetDateSettingsStore

internal actual fun saveWidgetDateSettings(dateFormat: DateFormat, timeFormat: TimeFormat) {
    WidgetDateSettingsStore().save(WidgetDateSettings(dateFormat = dateFormat, timeFormat = timeFormat))
}
