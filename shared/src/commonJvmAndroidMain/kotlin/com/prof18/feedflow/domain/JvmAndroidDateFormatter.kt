package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


internal class JvmAndroidDateFormatter(
    private val logger: Logger,
) : DateFormatter {

    private fun parseDateString(dateString: String): DateParsingResult {
        val dateTimeFormatterBuilder = DateTimeFormatterBuilder()
            .append(
                DateTimeFormatter.ofPattern(
                    "[EEE, d MMM yyyy HH:mm:ss z]" +
                            "[EEE, d MMM yyyy HH:mm:ss Z]" +
                            "[EEE, dd MMM yyyy HH:mm:ss 'Z']" +
                            "[EEE, dd MMM yyyy HH:mm z]" +
                            "[yyyy-MM-dd'T'HH:mm:ss.SSSXXX]" +
                            "[yyyy-MM-dd'T'HH:mm:ss.SSS'Z']" +
                            "[yyyy-MM-dd'T'HH:mm:ssXXX]" +
                            "[yyyy-MM-dd'T'HH:mm:ss'Z']" +
                            "[yyyy-MM-dd'T'HH:mm:ss]" +
                            "[yyyy-MM-dd'T'HH:mm:ss.SSSZ]" +
                            "[dd MMM yyyy HH:mm:ss Z]"
                )
            )
        val dateTimeFormatter = dateTimeFormatterBuilder.toFormatter()
        return try {
            LocalDateTime.parse(dateString, dateTimeFormatter)
            DateParsingResult.Parsed(
                date = LocalDateTime.parse(dateString, dateTimeFormatter),
            )
        } catch (e: Throwable) {
            DateParsingResult.ParsingError(
                exception = e,
                message = "Error while trying to format the date with dateFormatter. Date: $dateString",
            )
        }
    }

    override fun getDateMillisFromString(dateString: String): Long? {
        val parseResult = parseDateString(dateString)
        return if (parseResult is DateParsingResult.Parsed) {
            parseResult.date.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        } else {
            val exception = (parseResult as DateParsingResult.ParsingError).exception
            logger.e(exception) {
                parseResult.message
            }
            null
        }
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
            val date: LocalDateTime,
        ) : DateParsingResult()

        data class ParsingError(
            val exception: Throwable,
            val message: String,
        ) : DateParsingResult()
    }
}
