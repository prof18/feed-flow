package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

internal class JvmAndroidDateFormatter(
    private val logger: Logger,
) : DateFormatter {

    private val formatters = listOf(
        SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
    )

    @Suppress("TooGenericExceptionCaught")
    private fun SimpleDateFormat.parseDateString(dateString: String): DateParsingResult =
        try {
            DateParsingResult.Parsed(
                date = requireNotNull(
                    this.parse(dateString),
                ),
            )
        } catch (e: Throwable) {
            DateParsingResult.ParsingError(
                exception = e,
                message = "Error while trying to format the date with dateFormatter. Date: $dateString",
            )
        }

    override fun getDateMillisFromString(dateString: String): Long? {
        var exception: Throwable? = null
        var message: String? = null

        for (formatter in formatters) {
            val parseResult = formatter.parseDateString(dateString)
            if (parseResult is DateParsingResult.Parsed) {
                return parseResult.date.time
            } else {
                exception = (parseResult as DateParsingResult.ParsingError).exception
                message = parseResult.message
            }
        }

        val logMessage = message
        if (exception != null && logMessage != null) {
            logger.e(exception) {
                logMessage
            }
        }
        return null
    }

    override fun formatDate(millis: Long): String {
        val instant = Instant.ofEpochMilli(millis)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val today = LocalDateTime.now(ZoneId.systemDefault())
        val isToday = dateTime.toLocalDate() == today.toLocalDate()

        val pattern = if (isToday) {
            "HH:mm"
        } else {
            "dd/MM - HH:mm"
        }
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return formatter.format(dateTime)
    }

    override fun currentTimeMillis(): Long =
        System.currentTimeMillis()

    private sealed class DateParsingResult {
        data class Parsed(
            val date: Date,
        ) : DateParsingResult()

        data class ParsingError(
            val exception: Throwable,
            val message: String,
        ) : DateParsingResult()
    }
}
