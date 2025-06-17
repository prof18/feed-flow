package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.shared.testLogger
import kotlin.test.Test
import kotlin.test.assertNotNull

class DateFormatterTest {

    private val dateFormatter = DateFormatterImpl(testLogger)

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
    )

    @Test
    fun `getDateMillisFromString returns correct values`() {
        for (input in testInputs) {
            println(input)
            assertNotNull(dateFormatter.getDateMillisFromString(input))
        }
    }
}
