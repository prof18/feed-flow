@file:Suppress("MagicNumber")

package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

internal class IosDateFormatter(
    val logger: Logger,
) : DateFormatter {

    private val formatter = NSDateFormatter().apply {
        setLocale(NSLocale("en_US_POSIX"))
    }

    private val patterns = listOf(
        "E, dd MMM yyyy HH:mm:ss Z",
        "E, dd MMM yyyy HH:mm:ss z",
        "E, dd MMM yyyy HH:mm:ss zzz",
        "E, dd MMM yyyy HH:mm zzz",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ",
        "yyyy-MM-dd'T'HH:mm:ssZZZZZ",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy/MM/dd",
        "dd MMM yyyy HH:mm:ss Z",
    )

    private val articleDateFormatter = NSDateFormatter().apply {
        setLocale(NSLocale.currentLocale)
    }

    private fun NSDateFormatter.parseDateString(dateString: String): DateParsingResult {
        val formattedDate = this.dateFromString(dateString.trim())
            ?: return DateParsingResult.ParsingError(
                exception = NullPointerException(),
                message = "The formatted date is null. Date: $dateString",
            )
        return try {
            DateParsingResult.Parsed(
                date = formattedDate,
            )
        } catch (e: Throwable) {
            DateParsingResult.ParsingError(
                exception = e,
                message = "Error while trying to format the date with dateFormatter. Date: $dateString",
            )
        }
    }

    override fun getDateMillisFromString(dateString: String): Long? {
        var exception: Throwable? = null
        var message: String? = null

        for (pattern in patterns) {
            formatter.setDateFormat(pattern)
            val parseResult = formatter.parseDateString(dateString)
            if (parseResult is DateParsingResult.Parsed) {
                return (parseResult.date.timeIntervalSince1970 * 1000).toLong()
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
        val date = NSDate.dateWithTimeIntervalSince1970((millis.toDouble() / 1000.0))

        val calendar = NSCalendar.currentCalendar
        val isToday = calendar.isDateInToday(date)

        val pattern = if (isToday) {
            "HH:mm"
        } else {
            "dd/MM - HH:mm"
        }
        articleDateFormatter.setDateFormat(pattern)
        return articleDateFormatter.stringFromDate(date)
    }

    override fun currentTimeMillis(): Long =
        (NSDate().timeIntervalSince1970 * 1000).toLong()

    private sealed class DateParsingResult {
        data class Parsed(
            val date: NSDate,
        ) : DateParsingResult()

        data class ParsingError(
            val exception: Throwable,
            val message: String,
        ) : DateParsingResult()
    }
}
