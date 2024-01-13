package com.prof18.feedflow.shared.domain

internal interface DateFormatter {
    fun getDateMillisFromString(dateString: String): Long?
    fun formatDate(millis: Long): String
    fun currentTimeMillis(): Long
}
