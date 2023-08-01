package com.prof18.feedflow.domain

import com.prof18.feedflow.DateFormatterFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateFormatterTest {

    private val dateFormatter = DateFormatterFactory.createDateFormatter()

    private val testInputs = listOf(
        "Wed, 15 May 2019 20:48:02 +0000" to 1557953282000,
        "Fri, 7 May 2021 10:44:02 EST" to 1620402242000,
        "Fri, 7 May 2021 10:44:0" to null,
        "2023-07-28T15:01:10+02:00" to 1690549270000,
        "Fri, 28 Jul 2023 12:37:25 +0300" to 1690537045000,
        "2023-05-18T15:00:00.000Z" to 1684414800000,
        "Mon, 31 Jul 2023 19:26:12 -0400" to 1690845972000,
        "2023-07-09T18:42:00.004-07:00" to 1688953320004,
        "2022/09/22" to 1663797600000,
        "2023-07-25T13:55:02" to 1690286102000,
    )

    @Test
    fun `getDateMillisFromString returns correct values`() {
        for (input in testInputs) {
            assertEquals(input.second, dateFormatter.getDateMillisFromString(input.first))
        }
    }

    @Test
    fun fakeTest() {
        assertTrue(true)
    }
}
