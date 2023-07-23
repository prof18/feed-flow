package com.prof18.feedflow.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter

actual fun crashReportingLogWriter(): LogWriter = CrashlyticsLogWriter()
