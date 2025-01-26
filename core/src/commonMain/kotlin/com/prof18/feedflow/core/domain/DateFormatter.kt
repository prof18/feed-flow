package com.prof18.feedflow.core.domain

interface DateFormatter {
    fun getDateMillisFromString(dateString: String): Long?
    fun formatDateForFeed(millis: Long): String
    fun formatDateForLastRefresh(millis: Long): String
    fun currentTimeMillis(): Long
    fun getCurrentDateForExport(): String
}
