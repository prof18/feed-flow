package com.prof18.feedflow.shared.utils

interface Telemetry {
    fun trackError(id: String, message: String)
    fun signal(id: String)
}
