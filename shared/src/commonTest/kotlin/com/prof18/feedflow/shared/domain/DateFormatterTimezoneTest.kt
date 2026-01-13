package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.shared.test.testLogger
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class DateFormatterTimezoneTest {

    private val dateFormatter = DateFormatterImpl(testLogger)

    @Test
    fun `EDT timezone should be correctly interpreted`() {
        // "Fri, 7 May 2021 10:44:02 EDT"
        // EDT is UTC-4, so 10:44:02 EDT = 14:44:02 UTC
        val dateString = "Fri, 7 May 2021 10:44:02 EDT"
        val millis = dateFormatter.getDateMillisFromString(dateString)

        assertNotNull(millis)

        // Convert to UTC to see what time it actually represents
        val instant = Instant.fromEpochMilliseconds(millis)
        val utcDateTime = instant.toLocalDateTime(TimeZone.UTC)

        println("Input: $dateString")
        println("Parsed epoch millis: $millis")
        println("UTC time: ${utcDateTime.hour}:${utcDateTime.minute}:${utcDateTime.second}")
        println("Expected UTC time: 14:44:02 (10:44 EDT + 4 hours)")

        // If EDT is correctly parsed, 10:44 EDT should be 14:44 UTC
        assertEquals(14, utcDateTime.hour, "Hour should be 14 in UTC (10 + 4 for EDT offset)")
        assertEquals(44, utcDateTime.minute)
    }

    @Test
    fun `EST timezone should be correctly interpreted`() {
        // "Fri, 7 May 2021 10:44:02 EST"
        // EST is UTC-5, so 10:44:02 EST = 15:44:02 UTC
        val dateString = "Fri, 7 May 2021 10:44:02 EST"
        val millis = dateFormatter.getDateMillisFromString(dateString)

        assertNotNull(millis)

        val instant = Instant.fromEpochMilliseconds(millis)
        val utcDateTime = instant.toLocalDateTime(TimeZone.UTC)

        println("Input: $dateString")
        println("UTC time: ${utcDateTime.hour}:${utcDateTime.minute}:${utcDateTime.second}")
        println("Expected UTC time: 15:44:02 (10:44 EST + 5 hours)")

        // If EST is correctly parsed, 10:44 EST should be 15:44 UTC
        assertEquals(15, utcDateTime.hour, "Hour should be 15 in UTC (10 + 5 for EST offset)")
        assertEquals(44, utcDateTime.minute)
    }

    @Test
    fun `offset format should be correctly interpreted`() {
        // "Fri, 7 May 2021 10:44:02 -0400" (same as EDT)
        val dateString = "Fri, 7 May 2021 10:44:02 -0400"
        val millis = dateFormatter.getDateMillisFromString(dateString)

        assertNotNull(millis)

        val instant = Instant.fromEpochMilliseconds(millis)
        val utcDateTime = instant.toLocalDateTime(TimeZone.UTC)

        println("Input: $dateString")
        println("UTC time: ${utcDateTime.hour}:${utcDateTime.minute}:${utcDateTime.second}")

        // Offset -0400 means 4 hours behind UTC
        // So 10:44 -0400 should be 14:44 UTC
        assertEquals(14, utcDateTime.hour, "Hour should be 14 in UTC")
        assertEquals(44, utcDateTime.minute)
    }
}
