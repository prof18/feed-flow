package com.prof18.feedflow.desktop.utils

import com.prof18.feedflow.desktop.startup.containedDatabaseFailure
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions

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

        options.dropContainedDatabaseCrashes()
        options.setBeforeSend { event, _ -> scrubHomePaths(event, home) }
    }
}

fun disableSentry() {
    Sentry.close()
}

/**
 * The uncaught handler that Sentry wraps reports contained database failures as handled events and shows the
 * database error window, so the fatal copy Sentry captures first would only duplicate them as crashes.
 *
 * The filter runs ahead of Sentry's duplicate detection: that processor remembers every throwable it sees, so
 * dropping the fatal copy any later (in beforeSend, say) would also discard the handled report that follows.
 */
internal fun SentryOptions.dropContainedDatabaseCrashes() {
    eventProcessors.add(
        0,
        object : EventProcessor {
            override fun process(event: SentryEvent, hint: Hint): SentryEvent? = dropContainedDatabaseCrash(event)
        },
    )
}

internal fun dropContainedDatabaseCrash(event: SentryEvent): SentryEvent? =
    if (event.level == SentryLevel.FATAL && event.throwable?.containedDatabaseFailure() != null) null else event

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
