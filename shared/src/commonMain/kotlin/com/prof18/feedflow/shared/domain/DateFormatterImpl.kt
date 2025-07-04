package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DateFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents.Companion.Format
import kotlinx.datetime.format.DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
import kotlinx.datetime.format.DateTimeComponents.Formats.RFC_1123
import kotlinx.datetime.format.DateTimeFormatBuilder
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

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
            day(Padding.NONE)
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
            alternativeParsing({
                hour(padding = Padding.NONE)
            }) {
                hour()
            }
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
        // Tue, 17 Jun 2025 12:00:00 +03
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                optional {
                    char(',')
                }
                chars(" ")
            }

            day(Padding.ZERO)
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
                offset(UtcOffset.Formats.FOUR_DIGITS)
            }, {
                offset(UtcOffset.Formats.ISO_BASIC)
            }) {
                offset(UtcOffset.Formats.ISO)
            }
        },

        // Mon, Jun 16, 2025 06:28:45 +0200
        Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            chars(", ")
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            chars(" ")
            day(Padding.ZERO)
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
            day(Padding.NONE)
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
                day()
            }) {
                day(Padding.NONE)
            }
            char(' ')
            alternativeParsing({
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }) {
                monthName(MonthNames.ENGLISH_FULL)
            }
            char(' ')
            year()
            char(' ')
            alternativeParsing({
                hour()
            }) {
                hour(padding = Padding.NONE)
            }
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            chars(" ")
            timeZones()
        },

        // "2023-12-13 19:34:30  +0800"
        // "2023-12-20 18:35:26 +0800"
        // 2023-07-25T13:55:02
        Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
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
            timeZones()
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
            day()
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
            day()
        },

        Format {
            year()
            char('/')
            monthNumber()
            char('/')
            day()
        },

        // 2024-05-03 14:30
        Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
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
            day()
            char(',')
            char(' ')
            year()
        },

        // 30 August 2024
        Format {
            day()
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
                day()
            }) {
                day(Padding.NONE)
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
                day()
            }) {
                day(Padding.NONE)
            }
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            char(' ')
            alternativeParsing(
                {
                    hour(padding = Padding.NONE)
                },
            ) { hour() }
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
                day()
            }) {
                day(Padding.NONE)
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
                day()
            }) {
                day(Padding.NONE)
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
            timeZones()
        },

        // Feb 19, 2025 00:49 GMT
        Format {
            alternativeParsing({
                monthName(MonthNames(MonthNames.ENGLISH_ABBREVIATED.names.map { it.lowercase() }))
            }) {
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }
            char(' ')
            day()
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
            timeZones()
        },

        // 02/18/25 20:39:28
        Format {
            monthNumber()
            char('/')
            day()
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
            day()
            char('.')
            monthNumber()
            char('.')
            year()
        },

        // 30-Apr-2025 07:00:00
        Format {
            day()
            char('-')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char('-')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        },

        // 03.06.2025 14:12:00
        Format {
            day()
            char('.')
            monthNumber()
            char('.')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        },

        // Oct 26, 2020 9:47am
        // Aug 22, 2022 11:35am
        Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            day()
            chars(", ")
            year()
            char(' ')
            alternativeParsing(
                {
                    hour(padding = Padding.NONE)
                },
            ) {
                hour()
            }
            char(':')
            minute()
            amPmMarker(am = "am", pm = "pm")
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
            // Last resort
            try {
                parsed?.apply {
                    hour = 0
                    minute = 0
                }?.toInstantUsingOffset()?.toEpochMilliseconds()
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun formatDateForFeed(millis: Long, dateFormat: DateFormat): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val localDate = LocalDate(
            year = dateTime.year,
            month = dateTime.month.number,
            day = dateTime.day,
        )

        val isToday = today == localDate
        val isThisYear = today.year == localDate.year

        val dateFormat = when {
            isToday -> {
                LocalDateTime.Format {
                    hour()
                    char(':')
                    minute()
                }
            }

            isThisYear -> {
                LocalDateTime.Format {
                    dayAndMonth(dateFormat)
                    chars(" - ")
                    hour()
                    char(':')
                    minute()
                }
            }

            else -> {
                LocalDateTime.Format {
                    dayAndMonth(dateFormat)
                    char('/')
                    year()
                    chars(" - ")
                    hour()
                    char(':')
                    minute()
                }
            }
        }

        return dateFormat.format(dateTime)
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
        }
    }

    private fun DateTimeFormatBuilder.WithDateTimeComponents.timeZones() {
        alternativeParsing(
            { chars("EST") },
            { chars("GMT") },
            { chars("EDT") },
            { chars("CDT") },
            { chars("PDT") },
            { chars("PST") },
            { chars("UTC") },
            { chars("CET") },
            { chars("CST") },
            { chars("EEST") },
            { chars("CEST") },
        ) {}
    }

    override fun formatDateForLastRefresh(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val dateFormat = LocalDateTime.Format {
            day()
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
        return "${dateTime.day}-${dateTime.month.number}-${dateTime.year}"
    }
}
