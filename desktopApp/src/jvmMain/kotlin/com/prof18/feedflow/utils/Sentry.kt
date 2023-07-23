package com.prof18.feedflow.utils

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
