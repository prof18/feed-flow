package com.prof18.feedflow.core.domain

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat

interface DateFormatter {
    fun getDateMillisFromString(dateString: String): Long?
    fun formatDateForFeed(millis: Long, dateFormat: DateFormat, timeFormat: TimeFormat): String
    fun formatDateForLastRefresh(millis: Long): String
    fun currentTimeMillis(): Long
    fun getCurrentDateForExport(): String
}
