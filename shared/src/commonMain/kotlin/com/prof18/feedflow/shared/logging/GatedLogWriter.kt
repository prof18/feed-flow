package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Forwards to [delegate] only while [isEnabled] holds. The iOS Crashlytics writer throws an
 * NSException when Firebase isn't configured, which is always the case in app extensions.
 */
internal class GatedLogWriter(
    private val delegate: LogWriter,
    private val isEnabled: () -> Boolean,
) : LogWriter() {

    override fun isLoggable(tag: String, severity: Severity): Boolean =
        isEnabled() && delegate.isLoggable(tag, severity)

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (isEnabled()) {
            delegate.log(severity, message, tag, throwable)
        }
    }
}
