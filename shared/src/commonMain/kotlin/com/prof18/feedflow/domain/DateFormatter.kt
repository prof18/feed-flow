package com.prof18.feedflow.domain

internal interface DateFormatter {
    fun getDateMillisFromString(dateString: String): Long?
    fun formatDate(millis: Long): String
    fun currentTimeMillis(): Long
}
