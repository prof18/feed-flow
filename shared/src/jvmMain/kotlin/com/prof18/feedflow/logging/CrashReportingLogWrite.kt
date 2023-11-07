package com.prof18.feedflow.logging

import co.touchlab.kermit.LogWriter

actual fun crashReportingLogWriter(): LogWriter = SentryLogWriter()
