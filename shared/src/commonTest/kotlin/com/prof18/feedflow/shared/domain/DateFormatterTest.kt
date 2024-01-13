package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.shared.DateFormatterFactory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DateFormatterTest {

    private val dateFormatter = DateFormatterFactory.createDateFormatter()

    private val testInputs = listOf(
        "Wed, 15 May 2019 20:48:02 +0000",
        "Fri, 7 May 2021 10:44:02 EST",
        "2023-07-28T15:01:10+02:00",
        "Fri, 28 Jul 2023 12:37:25 +0300",
        "2023-05-18T15:00:00.000Z",
        "Mon, 31 Jul 2023 19:26:12 -0400",
        "2023-07-09T18:42:00.004-07:00",
        "2023-07-25T13:55:02",
        "Tue, 08 Aug 2023 12:58:00 Z",
        "Tue, 5 Sep 2017 09:58:38 +0000",
        "Wed, 13 Sep 2017 09:59:21 +0000",
        "Thu, 31 Aug 2023 00:00:00 GMT",
        "29 Aug 2023 00:00:29 +0200",
        "Mon, 21 Aug 2023 13:56 EDT",
        "Tue, 09 May 2023 19:32:59 +0000",
        "Thu, 06 Jul 2023 18:28:00 +0800",
        "2023-12-13 19:34:30  +0800",
        "2023-12-20 18:35:26 +0800",
        "2023-12-19 16:02:26",
    )

    @Test
    fun `getDateMillisFromString returns correct values`() {
        for (input in testInputs) {
            println(input)
            assertNotNull(dateFormatter.getDateMillisFromString(input))
        }
    }

    @Test
    fun `getDateMillisFromString returns null if the format is not supported`() {
        assertNull(
            dateFormatter.getDateMillisFromString(
                "Fri, 7 May 2021 10:44:0",
            ),
        )
    }
}
