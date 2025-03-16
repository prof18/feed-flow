package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
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

class DateFormatterImpl(
    private val logger: Logger,
) : DateFormatter {
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
            alternativeParsing({
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }) {
                monthName(MonthNames.ENGLISH_FULL)
            }
            char(' ')
            alternativeParsing({
                yearTwoDigits(1970)
            }) {
                year()
            }
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

        //  "Thu 02 May 2024 11:37:00 +0200"
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(" ")
            }
            dayOfMonth(Padding.ZERO)
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
            offset(UtcOffset.Formats.FOUR_DIGITS)
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
            optional {
                chars("UTC")
            }
            optional {
                chars("CET")
            }
            optional {
                chars("GMT")
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
            optional {
                chars(" ")
            }
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
            optional {
                chars("UTC")
            }
            optional {
                chars("CET")
            }
            optional {
                chars("GMT")
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
        // 2022/09/22
        Format {
            year()
            alternativeParsing({
                char('-')
            }) {
                char('/')
            }
            monthNumber()
            alternativeParsing({
                char('-')
            }) {
                char('/')
            }
            dayOfMonth()
        },

        Format {
            year()
            char('/')
            monthNumber()
            char('/')
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

        // May 21, 2024
        Format {
            alternativeParsing(
                {
                    monthName(MonthNames.ENGLISH_FULL)
                },
            ) {
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }
            char(' ')
            dayOfMonth()
            char(',')
            char(' ')
            year()
        },

        // 30 August 2024
        Format {
            dayOfMonth()
            char(' ')
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            year()
        },

        // January 1, 2017
        Format {
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            alternativeParsing({
                dayOfMonth()
            }) {
                dayOfMonth(Padding.NONE)
            }
            char(',')
            char(' ')
            year()
        },

        // Wed, 18 Dec 2024 17:46:49
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
        },

        // Thu 18/01/2024 - 13:01
        Format {
            alternativeParsing({
                dayOfWeek(DayOfWeekNames(DayOfWeekNames.ENGLISH_ABBREVIATED.names.map { it.lowercase() }))
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            }
            char(' ')
            alternativeParsing({
                dayOfMonth()
            }) {
                dayOfMonth(Padding.NONE)
            }
            char('/')
            monthNumber()
            char('/')
            year()
            chars(" - ")
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
        },

        // mar, 14 gen 2025 13:46:12 GMT
        Format {
            dayOfWeek(DayOfWeekNames(DayOfWeekNames.ENGLISH_ABBREVIATED.names.map { it.lowercase() }))
            chars(", ")

            alternativeParsing({
                dayOfMonth()
            }) {
                dayOfMonth(Padding.NONE)
            }
            char(' ')
            alternativeParsing({
                monthName(MonthNames(MonthNames.ENGLISH_ABBREVIATED.names.map { it.lowercase() }))
            }) {
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }
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
                chars("GMT")
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
            optional {
                chars("UTC")
            }
            optional {
                chars("CET")
            }
        },

        // Feb 19, 2025 00:49 GMT
        Format {
            alternativeParsing({
                monthName(MonthNames(MonthNames.ENGLISH_ABBREVIATED.names.map { it.lowercase() }))
            }) {
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }
            char(' ')
            dayOfMonth()
            chars(", ")
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
                chars("GMT")
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
            optional {
                chars("UTC")
            }
            optional {
                chars("CET")
            }
        },

        // 02/18/25 20:39:28
        Format {
            monthNumber()
            char('/')
            dayOfMonth()
            char('/')
            yearTwoDigits(1990)
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        },

        // "14.03.2025
        Format {
            dayOfMonth()
            char('.')
            monthNumber()
            char('.')
            year()
        },
    )

    override fun getDateMillisFromString(dateString: String): Long? {
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

    override fun formatDateForFeed(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val localDate = LocalDate(
            year = dateTime.year,
            monthNumber = dateTime.monthNumber,
            dayOfMonth = dateTime.dayOfMonth,
        )

        val isToday = today == localDate
        val isThisYear = today.year == localDate.year

        val dateFormat = if (isToday) {
            LocalDateTime.Format {
                hour()
                char(':')
                minute()
            }
        } else if (isThisYear) {
            LocalDateTime.Format {
                dayOfMonth()
                char('/')
                monthNumber()
                chars(" - ")
                hour()
                char(':')
                minute()
            }
        } else {
            LocalDateTime.Format {
                dayOfMonth()
                char('/')
                monthNumber()
                char('/')
                year()
                chars(" - ")
                hour()
                char(':')
                minute()
            }
        }

        return dateFormat.format(dateTime)
    }

    override fun formatDateForLastRefresh(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val dateFormat = LocalDateTime.Format {
            dayOfMonth()
            char('/')
            monthNumber()
            char('/')
            year()
            chars(" - ")
            hour()
            char(':')
            minute()
            char(':')
            second()
        }

        return dateFormat.format(dateTime)
    }

    override fun currentTimeMillis(): Long =
        Clock.System.now().toEpochMilliseconds()

    override fun getCurrentDateForExport(): String {
        val instant = Clock.System.now()
        val dateTime: LocalDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.dayOfMonth}-${dateTime.monthNumber}-${dateTime.year}"
    }
}
