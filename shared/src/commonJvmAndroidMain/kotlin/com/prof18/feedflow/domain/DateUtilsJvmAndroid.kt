package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal actual fun getDateMillisFromString(dateString: String, logger: Logger?): Long? {
    var exception: Throwable? = null
    var message: String? = null

    var date = try {
        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault())
        dateFormat.parse(dateString)?.time
    } catch (e: ParseException) {
        exception = e
        message = "Error while trying to format the date with dateFormatter. Date: $dateString"
        null
    }

    if (date == null) {
        date = try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            dateFormat.parse(dateString)?.time
        } catch (e: ParseException) {
            exception = e
            message = "Error while trying to format the date with dateFormatter. Date: $dateString"
            null
        }
    }

    if (date == null) {
        date = try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            dateFormat.parse(dateString)?.time
        } catch (e: ParseException) {
            exception = e
            message = "Error while trying to format the date with dateFormatter. Date: $dateString"
            null
        }
    }

    if (date == null && exception != null && message != null) {
        logger?.e(exception) {
            message
        }
    }

    return date
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
