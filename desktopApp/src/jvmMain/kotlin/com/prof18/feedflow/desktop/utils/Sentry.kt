package com.prof18.feedflow.desktop.utils

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel

fun initSentry(
    dns: String,
    version: String,
) {
    val home = System.getProperty("user.home")
    Sentry.init { options ->
        options.dsn = dns

        options.release = "com.prof18.feedflow@$version"

        options.setDiagnosticLevel(
            SentryLevel.ERROR,
        )

        options.setBeforeSend { event, _ -> scrubHomePaths(event, home) }
    }
}

fun disableSentry() {
    Sentry.close()
}

/** Exception messages can carry absolute paths (e.g. native library locations); keep the user's home out. */
internal fun scrubHomePaths(event: SentryEvent, home: String?): SentryEvent {
    if (home.isNullOrBlank()) return event
    event.message?.let { message ->
        message.formatted = message.formatted?.replace(home, "~")
        message.message = message.message?.replace(home, "~")
    }
    event.exceptions?.forEach { exception ->
        exception.value = exception.value?.replace(home, "~")
    }
    return event
}
