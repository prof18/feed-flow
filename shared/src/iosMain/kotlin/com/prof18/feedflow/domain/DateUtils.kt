package com.prof18.feedflow.domain

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

internal actual fun getDateMillisFromString(dateString: String): Long? {
    val dateFormatter = NSDateFormatter().apply {
        setDateFormat("EEE, d MMM yyyy HH:mm:ss Z")
        setLocale(NSLocale.currentLocale)
    }
    val date = dateFormatter.dateFromString(dateString) ?: return null
    return (date.timeIntervalSince1970 * 1000).toLong()
}

internal actual fun formatDate(millis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970((millis.toDouble() / 1000.0))

    val calendar = NSCalendar.currentCalendar
    val isToday = calendar.isDateInToday(date)

    val pattern = if (isToday) {
        "HH:mm"
    } else {
        "dd/MM - HH:mm"
    }

    val dateFormatter = NSDateFormatter().apply {
        setDateFormat(pattern)
        setLocale(NSLocale.currentLocale)
    }

    return dateFormatter.stringFromDate(date)
}

internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
