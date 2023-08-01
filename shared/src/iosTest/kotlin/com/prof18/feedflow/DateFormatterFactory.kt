package com.prof18.feedflow

import com.prof18.feedflow.domain.DateFormatter
import com.prof18.feedflow.domain.IosDateFormatter

internal actual object DateFormatterFactory {
    actual fun createDateFormatter(): DateFormatter =
        IosDateFormatter(logger = testLogger)
}
