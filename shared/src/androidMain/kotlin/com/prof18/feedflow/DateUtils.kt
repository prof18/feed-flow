package com.prof18.feedflow

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(millis: Long): String {
    val pattern = if (DateUtils.isToday(millis)) {
        "HH:mm"
    } else {
        "dd/MM - HH:mm"
    }
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return simpleDateFormat.format(Date(millis))
}