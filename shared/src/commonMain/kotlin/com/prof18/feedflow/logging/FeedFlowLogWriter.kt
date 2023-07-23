package com.prof18.feedflow.logging

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.MessageStringFormatter
import com.prof18.feedflow.utils.AppEnvironment

expect fun feedFlowLogWriter(
    appEnvironment: AppEnvironment,
    messageStringFormatter: MessageStringFormatter = DefaultFormatter,
): LogWriter
