package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GatedLogWriterTest {

    @Test
    fun `it never reaches the delegate while the gate is closed`() {
        val delegate = ThrowingWhenUnconfiguredLogWriter(isConfigured = { false })
        val logWriter = GatedLogWriter(delegate = delegate, isEnabled = { false })

        logWriter.log(Severity.Error, "Download failed", "FeedFlow", IllegalStateException("boom"))

        assertFalse(logWriter.isLoggable("FeedFlow", Severity.Error))
        assertEquals(0, delegate.logs.size)
    }

    @Test
    fun `it forwards logs once the gate opens`() {
        var isConfigured = false
        val delegate = ThrowingWhenUnconfiguredLogWriter(isConfigured = { isConfigured })
        val logWriter = GatedLogWriter(delegate = delegate, isEnabled = { isConfigured })

        logWriter.log(Severity.Warn, "Before configure", "FeedFlow", null)
        isConfigured = true
        logWriter.log(Severity.Warn, "After configure", "FeedFlow", null)

        assertTrue(logWriter.isLoggable("FeedFlow", Severity.Warn))
        assertEquals(listOf("After configure"), delegate.logs)
    }

    @Test
    fun `it keeps the delegate severity filter`() {
        val delegate = ThrowingWhenUnconfiguredLogWriter(isConfigured = { true })
        val logWriter = GatedLogWriter(delegate = delegate, isEnabled = { true })

        assertFalse(logWriter.isLoggable("FeedFlow", Severity.Debug))
        assertTrue(logWriter.isLoggable("FeedFlow", Severity.Info))
    }
}

// Behaves like Crashlytics: logging before Firebase is configured throws.
private class ThrowingWhenUnconfiguredLogWriter(
    private val isConfigured: () -> Boolean,
) : LogWriter() {
    val logs = mutableListOf<String>()

    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= Severity.Info

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        check(isConfigured()) { "The default Firebase app has not yet been configured" }
        logs.add(message)
    }
}
