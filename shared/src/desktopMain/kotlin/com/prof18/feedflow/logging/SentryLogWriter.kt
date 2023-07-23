package com.prof18.feedflow.logging

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import io.sentry.Sentry

class SentryLogWriter(
    private val minSeverity: Severity = Severity.Info,
    private val minCrashSeverity: Severity? = Severity.Warn,
    private val messageStringFormatter: MessageStringFormatter = DefaultFormatter,
) : LogWriter() {

    init {
        @Suppress("UseRequire")
        if (minCrashSeverity != null && minSeverity > minCrashSeverity) {
            throw IllegalArgumentException(
                "minSeverity ($minSeverity) cannot be greater than minCrashSeverity ($minCrashSeverity)",
            )
        }
    }

    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= minSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        Sentry.captureMessage(
            messageStringFormatter.formatMessage(severity, Tag(tag), Message(message)),
        )
        if (throwable != null && minCrashSeverity != null && severity >= minCrashSeverity) {
            Sentry.captureException(throwable)
        }
    }
}
