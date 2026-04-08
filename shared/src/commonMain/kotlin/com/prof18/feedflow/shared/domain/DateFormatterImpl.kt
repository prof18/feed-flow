package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
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
    private val clock: Clock,
) : DateFormatter {
    private val formats = listOf(
        ISO_DATE_TIME_OFFSET,
        RFC_1123,

        // Sunday, 10 Aug 2025 19:00:01 GMT (GMT will be normalized to +0000)
        // Accept full day-of-week names
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                alternativeParsing({
                    dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
                }) {
                    // also accept lowercase full names
                    dayOfWeek(
                        DayOfWeekNames(
                            DayOfWeekNames.ENGLISH_FULL.names.map { it.lowercase() },
                        ),
                    )
                }
                chars(", ")
            }

            alternativeParsing({
                day(Padding.ZERO)
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
            // timezone already normalized (e.g., +0000)
            offset(UtcOffset.Formats.FOUR_DIGITS)
        },

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
                    char('.')
                }
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
                optional {
                    char('.')
                }
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

        // Fri, 7 May 2021 10:44:02 EST (will be normalized to -0500)
        // Fri, 7 May 2021 10:44:02 -0500
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
            offset(UtcOffset.Formats.FOUR_DIGITS)
        },

        // "2023-12-13 19:34:30  +0800"
        // "2023-12-20 18:35:26 +0800"
        // 2023-07-25T13:55:02
        // "2023-12-15 00:00:00 UTC" (will be normalized to +0000)
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
            optional {
                char(':')
                second()
                optional {
                    char('.')
                    secondFraction(1, 9)
                }
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

        // Tuesday, 07 April 2026
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                alternativeParsing({
                    dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
                }) {
                    dayOfWeek(
                        DayOfWeekNames(
                            DayOfWeekNames.ENGLISH_FULL.names.map { it.lowercase() },
                        ),
                    )
                }
                chars(", ")
            }
            alternativeParsing({
                day(Padding.ZERO)
            }) {
                day(Padding.NONE)
            }
            char(' ')
            monthName(MonthNames.ENGLISH_FULL)
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

        // mar, 14 gen 2025 13:46:12 GMT (will be normalized to +0000)
        // thu, 18 jan 2024 13:46:12 CET (will be normalized to +0100)
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
            offset(UtcOffset.Formats.FOUR_DIGITS)
        },

        // Feb 19, 2025 00:49 GMT (will be normalized to +0000)
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
            offset(UtcOffset.Formats.FOUR_DIGITS)
        },

        // Fri, Jun 14 2024 10:08:25 +0000
        // Sun Feb 05 2012 10:48:29
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                alternativeParsing({
                    dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                }) {
                    dayOfWeek(
                        DayOfWeekNames(
                            DayOfWeekNames.ENGLISH_ABBREVIATED.names.map { it.lowercase() },
                        ),
                    )
                }
                alternativeParsing({
                    chars(", ")
                }) {
                    char(' ')
                }
            }
            alternativeParsing({
                monthName(MonthNames.ENGLISH_ABBREVIATED)
            }) {
                monthName(MonthNames.ENGLISH_FULL)
            }
            char(' ')
            alternativeParsing({
                day(Padding.ZERO)
            }) {
                day(Padding.NONE)
            }
            char(' ')
            year()
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
            optional {
                chars(" ")
                alternativeParsing({
                    offset(UtcOffset.Formats.FOUR_DIGITS)
                }, {
                    offset(UtcOffset.Formats.ISO_BASIC)
                }) {
                    offset(UtcOffset.Formats.ISO)
                }
            }
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

        // Fri, 15 Oct 2021, 16:15:00 -0600
        Format {
            alternativeParsing({
                // the day of week may be missing
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            alternativeParsing({
                day(Padding.NONE)
            }) {
                day()
            }
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
            chars(", ")
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

        // Oct 26, 2020 9:47am
        // Aug 22, 2022 11:35am
        // Oct 1, 2025 8:30am
        Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            alternativeParsing({
                day(Padding.NONE)
            }) {
                day()
            }
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

        // 21:02 | 22-11-2025
        Format {
            alternativeParsing({
                hour(padding = Padding.NONE)
            }) {
                hour()
            }
            char(':')
            minute()
            chars(" | ")
            alternativeParsing({
                day(Padding.NONE)
            }) {
                day()
            }
            char('-')
            monthNumber()
            char('-')
            year()
        },
    )

    override fun getDateMillisFromString(dateString: String): Long? {
        if (dateString == "Date:") {
            return null
        }

        val basicNormalized = normalizeBasic(dateString)

        // First pass: try all formats with cheap normalization only
        val parsed = tryParse(basicNormalized)
            // Second pass (fallback): apply expensive locale normalization for non-English dates
            ?: tryParse(normalizeLocale(basicNormalized))

        if (parsed == null) {
            logger.e {
                "Error while trying to format the date with dateFormatter. Date: $dateString"
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

    override fun formatDateForFeed(millis: Long, dateFormat: DateFormat, timeFormat: TimeFormat): String {
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
                        char('/')
                        year()
                    }
                    chars(" - ")
                    hourAndMinute(timeFormat)
                }
            }
        }

        return dateFormatBuilder.format(dateTime)
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
        clock.now().toEpochMilliseconds()

    override fun getCurrentDateForExport(): String {
        val instant = clock.now()
        val dateTime: LocalDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.day}-${dateTime.month.number}-${dateTime.year}"
    }

    private fun normalizeBasic(dateString: String): String {
        var normalized = dateString
        // Replace timezone abbreviations with numeric offsets
        timezoneReplacements.forEach { (abbrev, offset) ->
            val regex = Regex("\\b$abbrev\\b")
            normalized = regex.replace(normalized, offset)
        }

        uppercaseEnglishDateTokenReplacements.forEach { (uppercaseToken, normalizedToken) ->
            val regex = Regex("(?<![A-Za-z])$uppercaseToken(?![A-Za-z])")
            normalized = regex.replace(normalized, normalizedToken)
        }

        // Normalize non-standard month abbreviation "Sept" -> "Sep" (case-insensitive)
        // This helps parse inputs like: "Fri, 18 Sept 2020 10:30:00 -0600"
        normalized = Regex("\\bsept\\b", RegexOption.IGNORE_CASE)
            .replace(normalized, "Sep")

        // Handle double spaces
        normalized = normalized.replace("  ", " ")

        // Handle missing space after day-of-week comma (e.g., "Wed,28" -> "Wed, 28")
        normalized = Regex("([A-Za-z]{3}),(\\d)")
            .replace(normalized) { matchResult ->
                "${matchResult.groupValues[1]}, ${matchResult.groupValues[2]}"
            }

        // Handle extra hyphen before T in ISO format (e.g., "2026-02-04-T08:00:00" -> "2026-02-04T08:00:00")
        normalized = normalized.replace("-T", "T")

        // Remove appended timestamp (garbage)
        // e.g. "Mar, 09 Dic 2025 13:46:55 +0000 2025-12-09 13:46:55"
        normalized = normalized.replace(Regex("\\s\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$"), "")

        return normalized
    }

    /**
     * Expensive fallback normalization for non-English locale dates.
     * Strips non-English day-of-week prefixes and replaces non-English month
     * abbreviations with English equivalents.
     * Only called when [normalizeBasic] + all formats fail to parse.
     */
    private fun normalizeLocale(dateString: String): String {
        var normalized = dateString

        // Strip non-English day-of-week prefix from RFC-822 style dates.
        // e.g. "lun, 16 Mar 2026 ..." -> "16 Mar 2026 ..."
        // e.g. "Σάβ, 21 Μαρ 2026 ..." -> "21 Μαρ 2026 ..."
        val dayOfWeekMatch = Regex("^(\\p{L}+),\\s+").find(normalized)
        if (dayOfWeekMatch != null) {
            val dayName = dayOfWeekMatch.groupValues[1]
            if (dayName !in englishDayAbbreviations) {
                normalized = normalized.removePrefix(dayOfWeekMatch.value)
            }
        }

        // Replace non-English month abbreviations with English equivalents.
        // e.g. "21 Μαρ 2026" -> "21 Mar 2026", "09 Dic 2025" -> "09 Dec 2025"
        // Uses Unicode-aware boundaries (\p{L}, \d) instead of \b which is ASCII-only in Java.
        monthReplacements.forEach { (localized, english) ->
            val regex = Regex(
                "(?<![\\p{L}\\d])${Regex.escape(localized)}(?![\\p{L}\\d])",
                RegexOption.IGNORE_CASE,
            )
            normalized = regex.replace(normalized, english)
        }

        return normalized
    }

    private fun tryParse(dateString: String) = formats.firstNotNullOfOrNull { format ->
        try {
            format.parse(dateString)
        } catch (_: Throwable) {
            null
        }
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

    // Non-English month abbreviations mapped to English equivalents.
    // Covers: French, Italian, Spanish, Portuguese, German, Dutch, Greek, Turkish, Polish, Romanian
    private val monthReplacements = mapOf(
        // January
        "janv" to "Jan", "jan" to "Jan", "gen" to "Jan", "ene" to "Jan",
        "Ιαν" to "Jan", "oca" to "Jan", "sty" to "Jan", "ian" to "Jan",
        // February
        "fév" to "Feb", "févr" to "Feb", "feb" to "Feb",
        "fev" to "Feb", "Φεβ" to "Feb", "şub" to "Feb", "lut" to "Feb",
        // March
        "mars" to "Mar", "mar" to "Mar", "mär" to "Mar",
        "mrt" to "Mar", "Μαρ" to "Mar", "Μάρ" to "Mar",
        // April
        "avr" to "Apr", "avri" to "Apr", "apr" to "Apr", "abr" to "Apr",
        "Απρ" to "Apr", "nis" to "Apr", "kwi" to "Apr",
        // May
        "mai" to "May", "mag" to "May", "may" to "May",
        "mei" to "May", "Μαΐ" to "May", "Μάι" to "May",
        "Μαϊ" to "May", "mayıs" to "May", "maj" to "May",
        // June
        "juin" to "Jun", "giu" to "Jun", "jun" to "Jun",
        "Ιουν" to "Jun", "haz" to "Jun", "cze" to "Jun", "iun" to "Jun",
        // July
        "juil" to "Jul", "juill" to "Jul", "lug" to "Jul", "jul" to "Jul",
        "Ιουλ" to "Jul", "tem" to "Jul", "lip" to "Jul", "iul" to "Jul",
        // August
        "août" to "Aug", "aou" to "Aug", "ago" to "Aug", "aug" to "Aug",
        "Αυγ" to "Aug", "ağu" to "Aug", "sie" to "Aug",
        // September
        "sep" to "Sep", "set" to "Sep",
        "Σεπ" to "Sep", "eyl" to "Sep", "wrz" to "Sep",
        // October
        "oct" to "Oct", "okt" to "Oct", "ott" to "Oct", "out" to "Oct",
        "Οκτ" to "Oct", "eki" to "Oct", "paź" to "Oct",
        // November
        "nov" to "Nov", "Νοε" to "Nov", "kas" to "Nov", "lis" to "Nov",
        "noi" to "Nov",
        // December
        "déc" to "Dec", "dic" to "Dec", "dez" to "Dec", "dec" to "Dec",
        "Δεκ" to "Dec", "ara" to "Dec", "gru" to "Dec",
    )

    private val englishDayAbbreviations = setOf(
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun",
        "mon", "tue", "wed", "thu", "fri", "sat", "sun",
    )

    private val uppercaseEnglishDateTokenReplacements = mapOf(
        "MON" to "Mon",
        "TUE" to "Tue",
        "WED" to "Wed",
        "THU" to "Thu",
        "FRI" to "Fri",
        "SAT" to "Sat",
        "SUN" to "Sun",
        "JAN" to "Jan",
        "FEB" to "Feb",
        "MAR" to "Mar",
        "APR" to "Apr",
        "MAY" to "May",
        "JUN" to "Jun",
        "JUL" to "Jul",
        "AUG" to "Aug",
        "SEP" to "Sep",
        "OCT" to "Oct",
        "NOV" to "Nov",
        "DEC" to "Dec",
    )

    private val timezoneReplacements = mapOf(
        "EST" to "-0500", // Eastern Standard Time
        "EDT" to "-0400", // Eastern Daylight Time
        "CST" to "-0600", // Central Standard Time (US)
        "CDT" to "-0500", // Central Daylight Time
        "PST" to "-0800", // Pacific Standard Time
        "PDT" to "-0700", // Pacific Daylight Time
        "GMT" to "+0000", // Greenwich Mean Time
        "UTC" to "+0000", // Coordinated Universal Time
        "CET" to "+0100", // Central European Time
        "CEST" to "+0200", // Central European Summer Time
        "EEST" to "+0300", // Eastern European Summer Time
    )
}
