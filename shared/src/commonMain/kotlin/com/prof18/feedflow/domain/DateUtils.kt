package com.prof18.feedflow.domain

internal expect fun getDateMillisFromString(dateString: String): Long?

internal expect fun formatDate(millis: Long): String

internal expect fun currentTimeMillis(): Long
