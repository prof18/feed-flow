package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.testLogger
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatterRelativeTimeTest {

    private val dateFormatter = DateFormatterImpl(testLogger)
    private val dateFormat = DateFormat.NORMAL
    private val timeFormat = TimeFormat.HOURS_24

    @Test
    fun `returns now for timestamps less than 1 minute ago`() {
        val now = dateFormatter.currentTimeMillis()
        val thirtySecondsAgo = now - 30_000L

        val result = dateFormatter.formatDateForFeed(thirtySecondsAgo, dateFormat, timeFormat)
        assertEquals("now", result)
    }

    @Test
    fun `returns now for timestamps exactly now`() {
        val now = dateFormatter.currentTimeMillis()

        val result = dateFormatter.formatDateForFeed(now, dateFormat, timeFormat)
        assertEquals("now", result)
    }

    @Test
    fun `returns minutes for timestamps between 1 and 59 minutes ago`() {
        val now = dateFormatter.currentTimeMillis()
        val fiveMinutesAgo = now - (5 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(fiveMinutesAgo, dateFormat, timeFormat)
        assertEquals("5m", result)
    }

    @Test
    fun `returns 59m for timestamp 59 minutes ago`() {
        val now = dateFormatter.currentTimeMillis()
        val fiftyNineMinutesAgo = now - (59 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(fiftyNineMinutesAgo, dateFormat, timeFormat)
        assertEquals("59m", result)
    }

    @Test
    fun `returns hours for timestamps between 1 and 23 hours ago`() {
        val now = dateFormatter.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(twoHoursAgo, dateFormat, timeFormat)
        assertEquals("2h", result)
    }

    @Test
    fun `returns 23h for timestamp 23 hours ago`() {
        val now = dateFormatter.currentTimeMillis()
        val twentyThreeHoursAgo = now - (23 * 60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(twentyThreeHoursAgo, dateFormat, timeFormat)
        assertEquals("23h", result)
    }

    @Test
    fun `returns days for timestamps between 1 and 29 days ago`() {
        val now = dateFormatter.currentTimeMillis()
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(threeDaysAgo, dateFormat, timeFormat)
        assertEquals("3d", result)
    }

    @Test
    fun `returns 29d for timestamp 29 days ago`() {
        val now = dateFormatter.currentTimeMillis()
        val twentyNineDaysAgo = now - (29 * 24 * 60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(twentyNineDaysAgo, dateFormat, timeFormat)
        assertEquals("29d", result)
    }

    @Test
    fun `returns months for timestamps between 1 and 11 months ago`() {
        val now = dateFormatter.currentTimeMillis()
        val twoMonthsAgo = now - (2 * 30 * 24 * 60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(twoMonthsAgo, dateFormat, timeFormat)
        assertEquals("2mo", result)
    }

    @Test
    fun `returns 11mo for timestamp 11 months ago`() {
        val now = dateFormatter.currentTimeMillis()
        val elevenMonthsAgo = now - (11 * 30 * 24 * 60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(elevenMonthsAgo, dateFormat, timeFormat)
        assertEquals("11mo", result)
    }

    @Test
    fun `returns full date for timestamps 12 months or older`() {
        val now = dateFormatter.currentTimeMillis()
        val twoYearsAgo = now - (2 * 365 * 24 * 60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(twoYearsAgo, dateFormat, timeFormat)
        // Should contain a "/" which indicates date format, not relative time
        assert(result.contains("/")) { "Expected full date format with '/', but got: $result" }
    }

    @Test
    fun `handles future dates gracefully`() {
        val now = dateFormatter.currentTimeMillis()
        val oneHourFromNow = now + (60 * 60 * 1000L)

        val result = dateFormatter.formatDateForFeed(oneHourFromNow, dateFormat, timeFormat)
        assertEquals("now", result)
    }
}
