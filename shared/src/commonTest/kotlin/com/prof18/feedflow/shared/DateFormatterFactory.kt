package com.prof18.feedflow.shared

import com.prof18.feedflow.shared.domain.DateFormatter

internal expect object DateFormatterFactory {
    fun createDateFormatter(): DateFormatter
}
