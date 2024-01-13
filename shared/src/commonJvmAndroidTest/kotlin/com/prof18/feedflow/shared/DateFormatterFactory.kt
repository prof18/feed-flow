package com.prof18.feedflow.shared

import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.JvmAndroidDateFormatter

internal actual object DateFormatterFactory {
    actual fun createDateFormatter(): DateFormatter =
        JvmAndroidDateFormatter(logger = testLogger)
}
