package com.prof18.feedflow.desktop.utils

import io.sentry.Sentry
import io.sentry.SentryLevel

fun initSentry(
    dns: String,
    version: String,
) {
    Sentry.init { options ->
        options.dsn = dns

        options.release = "com.prof18.feedflow@$version"

        options.setDiagnosticLevel(
            SentryLevel.ERROR,
        )
    }
}
