package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.test.FakeClock
import com.prof18.feedflow.shared.test.testLogger
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class DateFormatterTest {

    private val dateFormatter = DateFormatterImpl(
        logger = testLogger,
        clock = FakeClock.DEFAULT,
    )

    private val testInputs = listOf(
        "Wed, 15 May 2019 20:48:02 +0000",
        "29 Aug 2023 00:00:29 +0200",
        "Tue, 08 Aug 2023 12:58:00 Z",
        "Thu, 06 Jul 2023 18:28:00 +0800",
        "Mon, 31 Jul 2023 19:26:12 -0400",
        "Fri, 28 Jul 2023 12:37:25 +0300",
        "Tue, 5 Sep 2017 09:58:38 +0000",
        "Thu, 31 Aug 2023 00:00:00 GMT",
        "Fri, 7 May 2021 10:44:02 EST",
        "2023-07-28T15:01:10+02:00",
        "2023-05-18T15:00:00.000Z",
        "2023-07-09T18:42:00.004-07:00",
        "2023-07-25T13:55:02",
        "Mon, 21 Aug 2023 13:56 EDT",
        "2023-12-13 19:34:30  +0800",
        "2023-12-20 18:35:26 +0800",
        "2023-12-19 16:02:26",
        "01 Jan 2014",
        "2024-04-08 07:09:09 +0800",
        "Thu, 22 Jun 2023",
        "Tue, 19 Mar 2024 06:00 -0400",
        "Sat, 13 Apr 2024 06:00:00 PDT",
        "Sat, 13 Apr 2024 06:00:00 PST",
        "2024-05-04",
        "2024-05-03 14:30",
        "Mon, 6 May 2024 22:00:00 +00:00",
        "Thu, 22 Jun 2023",
        "Thu 02 May 2024 11:37:00 +0200",
        "May 21, 2024",
        "30 August 2024",
        "Sat, 19 Oct 24 15:49:00 +0200",
        "January 1, 2017",
        "January 03, 2017",
        "January 10, 2017",
        "2023-12-15 00:00:00 UTC",
        "Wed, 18 Dec 2024 17:46:49",
        "Thu 18/01/2024 - 13:01",
        "thu 18/01/2024 - 13:01",
        "thu, 18 jan 2024 13:46:12 GMT",
        "thu, 18 jan 2024 13:46:12 CET",
        "thu 18/01/2024 - 10:14",
        "Feb 19, 2025 00:49 GMT",
        "2 February 2025 23:01:17 GMT",
        "02/18/25 20:39:28",
        "2022/09/22",
        "14.03.2025",
        "Thu, 13 Mar 2025 21:45:09 CET",
        "Wed, 26 Mar 2025 9:46:58 PDT",
        "Sat, 29 March 2025 06:00:00 CDT",
        "Mon, 05 Aug 2024 16:14:10 CST",
        "30-Apr-2025 07:00:00",
        "Mon, 12 May 2025 04:07:26 EEST",
        "Sat, 9 Nov 2024 1:02:00 +0100",
        "03.06.2025 14:12:00",
        "Aug 22, 2022 11:35am",
        "Oct 26, 2020 9:47am",
        "Sat, 16 Mar 2013 8:37:00",
        "Mon, 16 Jun 2025 13:39:25 CEST",
        "Mon, Jun 16, 2025 06:28:45 +0200",
        "Tue, 17 Jun 2025 12:00:00 +03",
        "Mon., 16 Jun 2025 15:08:44 +0200",
        "Fri, 15 Oct 2021, 16:15:00 -0600",
        "Oct 1, 2025 8:30am",
        "Sunday, 10 Aug 2025 19:00:01 GMT",
        "Fri, 18 Sept 2020 10:30:00 -0600",
        "21:02 | 22-11-2025",
        "Thu, 11 Dec 2025 10:23:40  Z",
        "Tue, 09 Dec 2025 13:46:55 +0000 2025-12-09 13:46:55",
        "Wed,28 Jan 2026 22:22:12 -0000",
        "2026-02-04-T08:00:00+02:00",
        // ISO date with T separator but no seconds (JAVA-B3)
        "2017-10-12T12:32",
        "2018-05-17T13:27",
        // Uppercase RFC-822 style dates (JAVA-B3)
        "TUE, 20 JAN 2026 09:17:26 -0400",
        "WED, 18 MAR 2026 09:16:34 -0400",
        // Non-English day-of-week with English month (JAVA-BB)
        "Mer, 08 Apr 2026 12:31:28 +0200",
        // Non-English day-of-week with English month (JAVA-6J)
        "lun, 16 Mar 2026 17:08:21 +0100",
        // Greek locale day-of-week and month (JAVA-G6)
        "Σάβ, 21 Μαρ 2026 18:00:00 +0200",
        // French locale
        "ven, 14 fév 2025 10:00:00 +0100",
        // Italian locale
        "mar, 09 dic 2025 13:46:55 +0000",
        // Spanish locale
        "lun, 10 ene 2026 08:30:00 +0100",
        // German locale
        "Mi, 05 Mär 2025 12:00:00 +0100",
        // Full weekday and full month date only (JAVA-G6)
        "Tuesday, 07 April 2026",
        "Wednesday, 08 April 2026",
        // JavaScript-style date string (JAVA-G6)
        "Sun Feb 05 2012 10:48:29",
        // Month-first timestamp without a day comma (JAVA-B3 follow-up in same Sentry bucket)
        "Fri, Jun 14 2024 10:08:25 +0000",
    )

    @Test
    fun `getDateMillisFromString parses various date formats correctly`() {
        for (input in testInputs) {
            println(input)
            assertNotNull(dateFormatter.getDateMillisFromString(input))
        }
    }

    @Test
    fun `getDateMillisFromString returns null for invalid input`() {
        val invalidInputs = listOf(
            "Date:",
            "2026-04",
            "Invalid Date",
        )

        for (input in invalidInputs) {
            val result = dateFormatter.getDateMillisFromString(input)
            assertNull(result, input)
        }
    }

    @Test
    fun `formatDateForFeed uses ISO format for today`() {
        val millis = FakeClock.DEFAULT.now().toEpochMilliseconds()
        val expectedDateTime = Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        val formatted = dateFormatter.formatDateForFeed(
            millis = millis,
            dateFormat = DateFormat.ISO,
            timeFormat = TimeFormat.HOURS_24,
        )

        val expected = "${expectedDateTime.hour.padded()}:${expectedDateTime.minute.padded()}"

        assertEquals(expected, formatted)
    }

    @Test
    fun `formatDateForFeed uses ISO format for an article from the same year`() {
        val timezone = TimeZone.currentSystemDefault()
        val millis = LocalDateTime(
            year = 2025,
            monthNumber = 2,
            dayOfMonth = 3,
            hour = 14,
            minute = 30,
        ).toInstant(timezone).toEpochMilliseconds()

        val formatted = dateFormatter.formatDateForFeed(
            millis = millis,
            dateFormat = DateFormat.ISO,
            timeFormat = TimeFormat.HOURS_24,
        )

        assertEquals("2025-02-03 - 14:30", formatted)
    }

    @Test
    fun `formatDateForFeed uses ISO format with 12 hour clock`() {
        val timezone = TimeZone.currentSystemDefault()
        val millis = LocalDateTime(
            year = 2024,
            monthNumber = 12,
            dayOfMonth = 25,
            hour = 14,
            minute = 30,
        ).toInstant(timezone).toEpochMilliseconds()

        val formatted = dateFormatter.formatDateForFeed(
            millis = millis,
            dateFormat = DateFormat.ISO,
            timeFormat = TimeFormat.HOURS_12,
        )

        assertEquals("2024-12-25 - 2:30 PM", formatted)
    }

    private fun Int.padded(): String = toString().padStart(2, '0')
}
