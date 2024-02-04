package com.prof18.feedflow.shared.domain

internal interface DateFormatter {
    fun getDateMillisFromString(dateString: String): Long?
    fun formatDateForFeed(millis: Long): String
    fun currentTimeMillis(): Long
    fun getCurrentDateForExport(): String
}
