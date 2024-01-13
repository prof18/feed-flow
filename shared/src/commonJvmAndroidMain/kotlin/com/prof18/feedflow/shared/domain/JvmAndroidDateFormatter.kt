package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

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
                        "[yyyy-MM-dd HH:mm:ss  Z]" +
                        "[yyyy-MM-dd HH:mm:ss Z]" +
                        "[yyyy-MM-dd HH:mm:ss]" +
                        "[dd MMM yyyy HH:mm:ss Z]",
                ),
            )

        val dateTimeFormatter = dateTimeFormatterBuilder.toFormatter(Locale.ENGLISH)

        return try {
            val temporalAccessor = dateTimeFormatter.parse(dateString)
            DateParsingResult.Parsed(
                date = ZonedDateTime.from(temporalAccessor).withZoneSameInstant(ZoneId.systemDefault()),
            )
        } catch (e: DateTimeException) {
            try {
                val localDateTime = LocalDateTime.parse(dateString, dateTimeFormatter)
                DateParsingResult.Parsed(
                    date = localDateTime.atZone(ZoneId.systemDefault()),
                )
            } catch (e: Throwable) {
                DateParsingResult.ParsingError(
                    exception = e,
                    message = "Error while trying to format the date with dateFormatter. Date: $dateString",
                )
            }
        } catch (e: Throwable) {
            DateParsingResult.ParsingError(
                exception = e,
                message = "Error while trying to format the date with dateFormatter. Date: $dateString",
            )
        }
    }

    @Suppress("MagicNumber")
    override fun getDateMillisFromString(dateString: String): Long? {
        val parseResult = parseDateString(dateString)
        return if (parseResult is DateParsingResult.Parsed) {
            parseResult.date.toEpochSecond() * 1000
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
        val userTimeZone = ZoneId.systemDefault()
        val zonedDateTime = instant.atZone(userTimeZone)

        val today = ZonedDateTime.now()
        val isToday = zonedDateTime.toLocalDate() == today.toLocalDate()
        val pattern = if (isToday) {
            "HH:mm"
        } else {
            "dd/MM - HH:mm"
        }

        val formatter = DateTimeFormatter
            .ofPattern(pattern)
            .withZone(zonedDateTime.zone)
        return formatter.format(zonedDateTime)
    }

    override fun currentTimeMillis(): Long =
        System.currentTimeMillis()

    private sealed class DateParsingResult {
        data class Parsed(
            val date: ZonedDateTime,
        ) : DateParsingResult()

        data class ParsingError(
            val exception: Throwable,
            val message: String,
        ) : DateParsingResult()
    }
}
