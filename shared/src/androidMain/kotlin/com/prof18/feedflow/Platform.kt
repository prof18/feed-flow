package com.prof18.feedflow

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getDateMillisFromString(dateString: String): Long? {
    val dateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault())
    return try {
        dateFormat.parse(dateString)?.time
    } catch (e: ParseException) {
        null
    }
}