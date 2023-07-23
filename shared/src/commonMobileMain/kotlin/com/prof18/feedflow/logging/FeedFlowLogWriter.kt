package com.prof18.feedflow.logging

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter

@OptIn(ExperimentalKermitApi::class)
actual fun crashReportingLogWriter(): LogWriter = CrashlyticsLogWriter()
