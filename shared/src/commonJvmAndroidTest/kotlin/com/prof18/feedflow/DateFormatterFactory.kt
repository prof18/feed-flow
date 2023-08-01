package com.prof18.feedflow

import com.prof18.feedflow.domain.DateFormatter
import com.prof18.feedflow.domain.JvmAndroidDateFormatter

internal actual object DateFormatterFactory {
    actual fun createDateFormatter(): DateFormatter =
        JvmAndroidDateFormatter(logger = testLogger)
}
