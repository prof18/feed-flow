package com.prof18.feedflow.domain

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


internal actual fun getDateMillisFromString(dateString: String): Long? {
    val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault())
    return try {
        dateFormat.parse(dateString)?.time
    } catch (e: ParseException) {
        null
    }
}

internal actual fun formatDate(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val today = LocalDateTime.now(ZoneId.systemDefault())
    val isToday = dateTime.toLocalDate() == today.toLocalDate()

    val pattern = if (isToday) {
        "HH:mm"
    } else {
        "dd/MM - HH:mm"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return formatter.format(dateTime)
}

internal actual fun currentTimeMillis(): Long =
    System.currentTimeMillis()