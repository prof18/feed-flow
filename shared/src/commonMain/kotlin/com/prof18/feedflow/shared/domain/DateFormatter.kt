package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents.Companion.Format
import kotlinx.datetime.format.DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
import kotlinx.datetime.format.DateTimeComponents.Formats.RFC_1123
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

class DateFormatter(
    val logger: Logger,
) {
    private val formats = listOf(
        ISO_DATE_TIME_OFFSET,
        RFC_1123,

        // Tue, 5 Sep 2017 09:58:38 +0000
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            dayOfMonth(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            chars(" ")
            alternativeParsing({
                chars("UT")
            }, {
                chars("Z")
            }) {
                optional("GMT") {
                    offset(UtcOffset.Formats.FOUR_DIGITS)
                }
            }
        },

        //  "Mon, 6 May 2024 22:00:00 +00:00"
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            dayOfMonth(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            chars(" ")
            offsetHours()
            char(':')
            offsetMinutesOfHour()
        },

        // Fri, 7 May 2021 10:44:02 EST
        //  Fri, 7 May 2021 10:44:02 EST
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            alternativeParsing({
                dayOfMonth()
            }) {
                dayOfMonth(Padding.NONE)
            }
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            chars(" ")
            optional {
                chars("EST")
            }
            optional {
                chars("EDT")
            }
            optional {
                chars("CDT")
            }
            optional {
                chars("PDT")
            }
            optional {
                chars("PST")
            }
        },

        // "2023-12-13 19:34:30  +0800"
        // "2023-12-20 18:35:26 +0800"
        // 2023-07-25T13:55:02
        Format {
            year()
            char('-')
            monthNumber()
            char('-')
            dayOfMonth()
            optional {
                char(' ')
            }
            optional {
                char('T')
            }
            hour()
            char(':')
            minute()
            char(':')
            second()
            optional {
                char('.')
                secondFraction(1, 9)
            }
            optional {
                char(' ')
            }
            optional {
                chars("  ")
            }
            optional {
                offset(UtcOffset.Formats.FOUR_DIGITS)
            }
        },

        // 01 Jan 2014
        // Thu, 22 Jun 2023
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            dayOfMonth()
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
        },

        // 2024-05-04
        Format {
            year()
            char('-')
            monthNumber()
            char('-')
            dayOfMonth()
        },

        // 2024-05-03 14:30
        Format {
            year()
            char('-')
            monthNumber()
            char('-')
            dayOfMonth()
            char(' ')
            hour()
            char(':')
            minute()
        },
    )

    fun getDateMillisFromString(dateString: String): Long? {
        var exception: Throwable? = null
        var errorMessage: String? = null

        val parsed = formats.firstNotNullOfOrNull { format ->
            try {
                format.parse(dateString)
            } catch (e: IllegalArgumentException) {
                exception = e
                errorMessage = "Error while trying to format the date with dateFormatter. Date: $dateString"
                null
            }
        }

        if (parsed == null) {
            exception?.printStackTrace()
            logger.e(exception) {
                errorMessage ?: "N/A"
            }
        }

        return try {
            parsed?.toInstantUsingOffset()?.toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            parsed?.apply {
                hour = 0
                minute = 0
            }?.toInstantUsingOffset()?.toEpochMilliseconds()
        }
    }

    fun formatDateForFeed(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val localDate = LocalDate(
            year = dateTime.year,
            monthNumber = dateTime.monthNumber,
            dayOfMonth = dateTime.dayOfMonth,
        )

        val isToday = today == localDate
        val dateFormat = if (isToday) {
            LocalDateTime.Format {
                hour()
                char(':')
                minute()
            }
        } else {
            LocalDateTime.Format {
                dayOfMonth()
                char('/')
                monthNumber()
                chars(" - ")
                hour()
                char(':')
                minute()
            }
        }

        return dateFormat.format(dateTime)
    }

    fun currentTimeMillis(): Long =
        Clock.System.now().toEpochMilliseconds()

    fun getCurrentDateForExport(): String {
        val instant = Clock.System.now()
        val dateTime: LocalDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.dayOfMonth}-${dateTime.monthNumber}-${dateTime.year}"
    }
}
