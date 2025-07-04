package com.prof18.feedflow.shared.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun getArchiveISUrl(articleUrl: String): String {
    val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    return "https://archive.is/$currentYear/$articleUrl"
}
