package com.prof18.feedflow.android

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

object CrashlyticsHelper {
    fun initCrashlytics() {
        // no-op
    }

    fun crashReportingLogWriter(): LogWriter = object : LogWriter() {
        override fun isLoggable(tag: String, severity: Severity): Boolean = false

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            // no-op
        }
    }

    @Suppress("UnusedParameter")
    fun setCollectionEnabled(enabled: Boolean) {
        // no-op
    }
}
