package com.prof18.feedflow.domain

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

internal actual fun getDateMillisFromString(dateString: String): Long? {
    val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault())
    return try {
        dateFormat.parse(dateString)?.time
    } catch (e: ParseException) {
        null
    }
}
