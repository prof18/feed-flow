package com.prof18.feedflow.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.XcodeSeverityWriter
import com.prof18.feedflow.utils.AppEnvironment

actual fun feedFlowLogWriter(
    appEnvironment: AppEnvironment,
    messageStringFormatter: MessageStringFormatter,
): LogWriter =
    XcodeSeverityWriter(messageStringFormatter)
// TODO: add crashlytics if not debug
