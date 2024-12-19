package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.shared.testLogger
import kotlin.test.Test
import kotlin.test.assertNotNull

class DateFormatterTest {

    private val dateFormatter = DateFormatter(testLogger)

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
    )

    @Test
    fun `getDateMillisFromString returns correct values`() {
        for (input in testInputs) {
            println(input)
            assertNotNull(dateFormatter.getDateMillisFromString(input))
        }
    }
}
