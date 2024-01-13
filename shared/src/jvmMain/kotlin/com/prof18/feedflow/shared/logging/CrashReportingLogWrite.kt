package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.LogWriter

actual fun crashReportingLogWriter(): LogWriter = SentryLogWriter()
