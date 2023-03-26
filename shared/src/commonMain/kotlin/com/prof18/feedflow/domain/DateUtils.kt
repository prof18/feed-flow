package com.prof18.feedflow.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

internal expect fun getDateMillisFromString(dateString: String): Long?

internal fun formatDate(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val timeZone = TimeZone.currentSystemDefault()
    val dateTime: LocalDateTime = instant.toLocalDateTime(timeZone)
    val today: LocalDate = Clock.System.todayIn(timeZone)

    return if (dateTime.dayOfMonth == today.dayOfMonth) {
        "${dateTime.hour}:${dateTime.minute}"
    } else {
        "${dateTime.dayOfMonth}/${dateTime.monthNumber} - ${dateTime.hour}:${dateTime.minute}"
    }
}