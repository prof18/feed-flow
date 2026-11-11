package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormatBuilder
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Formats article dates for display. Kept apart from [DateFormatterImpl], whose parsing formats
 * are built eagerly and cost too much memory for the iOS widget extension.
 */
class FeedDateFormatter(
    private val clock: Clock,
) {
    fun format(millis: Long, dateFormat: DateFormat, timeFormat: TimeFormat): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val today: LocalDate = clock.todayIn(TimeZone.currentSystemDefault())

        val localDate = LocalDate(
            year = dateTime.year,
            month = dateTime.month.number,
            day = dateTime.day,
        )

        val isToday = today == localDate
        val isThisYear = today.year == localDate.year

        val dateFormatBuilder = when {
            isToday -> {
                LocalDateTime.Format {
                    hourAndMinute(timeFormat)
                }
            }

            isThisYear -> {
                LocalDateTime.Format {
                    dayAndMonth(dateFormat)
                    chars(" - ")
                    hourAndMinute(timeFormat)
                }
            }

            else -> {
                LocalDateTime.Format {
                    if (dateFormat == DateFormat.ISO) {
                        yearMonthDay()
                    } else {
                        dayAndMonth(dateFormat)
                        char(if (dateFormat == DateFormat.DAY_MONTH_DOT) '.' else '/')
                        year()
                    }
                    chars(" - ")
                    hourAndMinute(timeFormat)
                }
            }
        }

        return dateFormatBuilder.format(dateTime)
    }

    private fun DateTimeFormatBuilder.WithDateTime.dayAndMonth(dateFormat: DateFormat) {
        when (dateFormat) {
            DateFormat.NORMAL -> {
                day()
                char('/')
                monthNumber()
            }

            DateFormat.AMERICAN -> {
                monthNumber()
                char('/')
                day()
            }

            DateFormat.DAY_MONTH_DOT -> {
                day()
                char('.')
                monthNumber()
            }

            DateFormat.ISO -> {
                yearMonthDay()
            }
        }
    }

    private fun DateTimeFormatBuilder.WithDateTime.yearMonthDay() {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
    }

    private fun DateTimeFormatBuilder.WithDateTime.hourAndMinute(timeFormat: TimeFormat) {
        when (timeFormat) {
            TimeFormat.HOURS_24 -> {
                hour()
                char(':')
                minute()
            }

            TimeFormat.HOURS_12 -> {
                amPmHour(padding = Padding.NONE)
                char(':')
                minute()
                char(' ')
                amPmMarker("AM", "PM")
            }
        }
    }
}
