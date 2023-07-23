package com.prof18.feedflow.logging

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import com.prof18.feedflow.utils.AppEnvironment

actual fun feedFlowLogWriter(
    appEnvironment: AppEnvironment,
    messageStringFormatter: MessageStringFormatter,
): LogWriter =
    SystemWriter(messageStringFormatter, appEnvironment)

// TODO: send stuff to sentry?
class SystemWriter(
    private val messageStringFormatter: MessageStringFormatter = DefaultFormatter,
    private val appEnvironment: AppEnvironment,
) : LogWriter() {

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val str = messageStringFormatter.formatMessage(severity, Tag(tag), Message(message))

        when {
            severity == Severity.Error -> {
                System.err.println(str)
            }
            appEnvironment.isDebug() -> {
                println(str)
            } // TODO: send log to sentry
        }
        throwable?.let {
            val thString = it.stackTraceToString()
            if (severity == Severity.Error) {
                System.err.println(thString)
            } else {
                println(thString)
            }
        }
    }
}
