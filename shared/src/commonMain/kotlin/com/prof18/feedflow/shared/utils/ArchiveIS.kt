package com.prof18.feedflow.shared.utils

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@DefaultArgumentInterop.Enabled
fun getArchiveISUrl(
    articleUrl: String,
    clock: Clock = Clock.System,
): String {
    val currentYear = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    return "https://archive.is/$currentYear/$articleUrl"
}
