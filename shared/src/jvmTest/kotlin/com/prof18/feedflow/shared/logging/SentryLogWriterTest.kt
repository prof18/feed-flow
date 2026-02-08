package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.Severity
import com.dropbox.core.NetworkIOException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

class SentryLogWriterTest {

    @Test
    fun `it captures message when throwable is absent`() {
        val capturedMessages = mutableListOf<String>()
        val capturedExceptions = mutableListOf<Throwable>()

        val logWriter = SentryLogWriter(
            isSentryEnabled = { true },
            captureMessage = { capturedMessages.add(it) },
            captureException = { capturedExceptions.add(it) },
        )

        logWriter.log(
            severity = Severity.Warn,
            message = "hello",
            tag = "Test",
            throwable = null,
        )

        assertEquals(1, capturedMessages.size)
        assertEquals(0, capturedExceptions.size)
    }

    @Test
    fun `it captures only exception when throwable is present and severity reaches crash threshold`() {
        val capturedMessages = mutableListOf<String>()
        val capturedExceptions = mutableListOf<Throwable>()

        val logWriter = SentryLogWriter(
            minSeverity = Severity.Info,
            minCrashSeverity = Severity.Warn,
            isSentryEnabled = { true },
            captureMessage = { capturedMessages.add(it) },
            captureException = { capturedExceptions.add(it) },
        )
        val throwable = IllegalStateException("boom")

        logWriter.log(
            severity = Severity.Error,
            message = "failure",
            tag = "Test",
            throwable = throwable,
        )

        assertEquals(0, capturedMessages.size)
        assertEquals(1, capturedExceptions.size)
        assertEquals(throwable, capturedExceptions.single())
    }

    @Test
    fun `it captures message when throwable is present but below crash threshold`() {
        val capturedMessages = mutableListOf<String>()
        val capturedExceptions = mutableListOf<Throwable>()

        val logWriter = SentryLogWriter(
            minSeverity = Severity.Info,
            minCrashSeverity = Severity.Error,
            isSentryEnabled = { true },
            captureMessage = { capturedMessages.add(it) },
            captureException = { capturedExceptions.add(it) },
        )

        logWriter.log(
            severity = Severity.Warn,
            message = "warn with throwable",
            tag = "Test",
            throwable = IllegalArgumentException("warn"),
        )

        assertEquals(1, capturedMessages.size)
        assertEquals(0, capturedExceptions.size)
    }

    @Test
    fun `it does nothing when sentry is disabled`() {
        val capturedMessages = mutableListOf<String>()
        val capturedExceptions = mutableListOf<Throwable>()

        val logWriter = SentryLogWriter(
            isSentryEnabled = { false },
            captureMessage = { capturedMessages.add(it) },
            captureException = { capturedExceptions.add(it) },
        )

        logWriter.log(
            severity = Severity.Error,
            message = "failure",
            tag = "Test",
            throwable = IllegalStateException("boom"),
        )

        assertEquals(0, capturedMessages.size)
        assertEquals(0, capturedExceptions.size)
    }

    @Test
    fun `it skips network io exceptions`() {
        val capturedMessages = mutableListOf<String>()
        val capturedExceptions = mutableListOf<Throwable>()

        val logWriter = SentryLogWriter(
            isSentryEnabled = { true },
            captureMessage = { capturedMessages.add(it) },
            captureException = { capturedExceptions.add(it) },
        )

        logWriter.log(
            severity = Severity.Error,
            message = "network",
            tag = "Test",
            throwable = NetworkIOException(IOException("timeout")),
        )

        assertEquals(0, capturedMessages.size)
        assertEquals(0, capturedExceptions.size)
    }

    @Test
    fun `it skips socket timeout exceptions`() {
        val capturedMessages = mutableListOf<String>()
        val capturedExceptions = mutableListOf<Throwable>()

        val logWriter = SentryLogWriter(
            isSentryEnabled = { true },
            captureMessage = { capturedMessages.add(it) },
            captureException = { capturedExceptions.add(it) },
        )

        logWriter.log(
            severity = Severity.Error,
            message = "timeout",
            tag = "Test",
            throwable = SocketTimeoutException("Connect timed out"),
        )

        assertEquals(0, capturedMessages.size)
        assertEquals(0, capturedExceptions.size)
    }
}
