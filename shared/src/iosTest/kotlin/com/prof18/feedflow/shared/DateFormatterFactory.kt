package com.prof18.feedflow.shared

import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.IosDateFormatter

internal actual object DateFormatterFactory {
    actual fun createDateFormatter(): DateFormatter =
        IosDateFormatter(logger = testLogger)
}
