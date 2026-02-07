package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import com.dropbox.core.NetworkIOException
import io.sentry.Sentry

class SentryLogWriter(
    private val minSeverity: Severity = Severity.Info,
    private val minCrashSeverity: Severity? = Severity.Warn,
    private val messageStringFormatter: MessageStringFormatter = DefaultFormatter,
    private val isSentryEnabled: () -> Boolean = { Sentry.isEnabled() },
    private val captureMessage: (String) -> Unit = { message -> Sentry.captureMessage(message) },
    private val captureException: (Throwable) -> Unit = { throwable -> Sentry.captureException(throwable) },
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
        if (throwable is NetworkIOException) {
            return
        }

        if (!isSentryEnabled()) {
            return
        }

        val shouldCaptureException = throwable != null &&
            minCrashSeverity != null &&
            severity >= minCrashSeverity

        if (shouldCaptureException) {
            // Avoid duplicate Sentry events for throwable logs.
            captureException(throwable)
            return
        }

        captureMessage(
            messageStringFormatter.formatMessage(severity, Tag(tag), Message(message)),
        )
    }
}
