package com.prof18.feedflow.core.domain

import com.prof18.feedflow.core.model.DateFormat

interface DateFormatter {
    fun getDateMillisFromString(dateString: String): Long?
    fun formatDateForFeed(millis: Long, dateFormat: DateFormat): String
    fun formatDateForLastRefresh(millis: Long): String
    fun currentTimeMillis(): Long
    fun getCurrentDateForExport(): String
}
