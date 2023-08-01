package com.prof18.feedflow

import com.prof18.feedflow.domain.DateFormatter

internal expect object DateFormatterFactory {
    fun createDateFormatter(): DateFormatter
}
