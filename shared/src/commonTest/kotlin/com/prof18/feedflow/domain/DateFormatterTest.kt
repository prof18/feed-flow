package com.prof18.feedflow.domain

import com.prof18.feedflow.DateFormatterFactory
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
        "2022/09/22",
        "2023-07-25T13:55:02",
    )

    @Test
    fun `getDateMillisFromString returns correct values`() {
        for (input in testInputs) {
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
