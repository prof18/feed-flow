@file:OptIn(ExperimentalKermitApi::class)

package com.prof18.feedflow.shared

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestConfig
import co.touchlab.kermit.TestLogWriter

private val testLogWriter = TestLogWriter(
    loggable = Severity.Verbose, // accept everything
)

val testLogger = Logger(
    TestConfig(
        minSeverity = Severity.Debug,
        logWriterList = listOf(testLogWriter),
    ),
)
