package com.prof18.feedflow.core.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object Websites {
    const val MG_WEBSITE = "https://www.marcogomiero.com"
    const val FEED_FLOW_WEBSITE = "https://www.feedflow.dev"
    const val TRANSLATION_WEBSITE = "https://hosted.weblate.org/projects/feedflow/"
}

fun getArchiveISUrl(articleUrl: String): String {
    val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    return "https://archive.is/$currentYear/$articleUrl"
}
