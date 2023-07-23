package com.prof18.feedflow.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.MessageStringFormatter
import com.prof18.feedflow.utils.AppEnvironment

actual fun feedFlowLogWriter(
    appEnvironment: AppEnvironment,
    messageStringFormatter: MessageStringFormatter,
): LogWriter =
    LogcatWriter() // / TODO: add crashlytics if not debug
