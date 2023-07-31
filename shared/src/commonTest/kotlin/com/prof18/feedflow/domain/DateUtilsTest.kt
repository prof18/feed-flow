package com.prof18.feedflow.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateUtilsTest {
    @Test
    fun `getDateMillisFromString returns correct values`() {
        val dateString = "Wed, 15 May 2019 20:48:02 +0000"

        val millis = getDateMillisFromString(dateString)
        println(millis)
        assertEquals(1557953282000, millis)
    }

    @Test
    fun `getDateMillisFromString returns correct values with timezone`() {
        val dateString = "Fri, 7 May 2021 10:44:02 EST"

        val millis = getDateMillisFromString(dateString)
        assertEquals(1620402242000, millis)
    }

    @Test
    fun `getDateMillisFromString returns null with a wrong value`() {
        val dateString = "Fri, 7 May 2021 10:44:0"

        val millis = getDateMillisFromString(dateString)
        assertNull(millis)
    }

    @Test
    fun `getDateMillisFromString returns a value with a strange format`() {
        val dateString = "2023-07-28T15:01:10+02:00"

        val millis = getDateMillisFromString(dateString)
        assertEquals(1690549270000, millis)
    }

    @Test
    fun `getDateMillisFromString returns a value with different time zone`() {
        val dateString = "Fri, 28 Jul 2023 12:37:25 +0300"

        val millis = getDateMillisFromString(dateString)
        assertEquals(1690537045000, millis)
    }

    @Test
    fun `getDateMillisFromString returns a value with another format`() {
        val dateString = "2023-05-18T15:00:00.000Z"

        val millis = getDateMillisFromString(dateString)
        assertEquals(1684414800000, millis)
    }
}
