package com.prof18.feedflow.shared.domain.mappers

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat

data class FeedItemMappingSettings(
    val removeTitleFromDescription: Boolean = false,
    val hideDescription: Boolean = false,
    val hideImages: Boolean = false,
    val hideDate: Boolean = false,
    val dateFormat: DateFormat = DateFormat.NORMAL,
    val timeFormat: TimeFormat = TimeFormat.HOURS_24,
)
