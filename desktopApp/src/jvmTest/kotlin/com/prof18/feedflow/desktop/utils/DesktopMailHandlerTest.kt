package com.prof18.feedflow.desktop.utils

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopE2eProperties
import com.prof18.feedflow.desktop.testing.DesktopE2eHooks
import java.io.File
import java.io.IOException
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopMailHandlerTest {

    @Test
    fun `successful mail send does not use fallback`() {
        var fallbackCalled = false

        val result = openDesktopMailSafely(
            url = "mailto:hello@example.com",
            sendMail = { uri -> assertTrue(uri.scheme == "mailto") },
            openFallback = {
                fallbackCalled = true
                true
            },
        )

        assertTrue(result)
        assertFalse(fallbackCalled)
    }

    @Test
    fun `unsupported mail handler uses successful fallback`() {
        var fallbackCalled = false

        val result = openDesktopMailSafely(
            url = "mailto:hello@example.com",
            sendMail = { throw UnsupportedOperationException("Mail is not supported") },
            openFallback = {
                fallbackCalled = true
                true
            },
        )

        assertTrue(result)
        assertTrue(fallbackCalled)
    }

    @Test
    fun `io exception uses fallback`() {
        var fallbackCalled = false

        val result = openDesktopMailSafely(
            url = "mailto:hello@example.com",
            sendMail = { throw IOException("Could not open mail client") },
            openFallback = {
                fallbackCalled = true
                true
            },
        )

        assertTrue(result)
        assertTrue(fallbackCalled)
    }

    @Test
    fun `returns false when fallback returns false`() {
        val result = openDesktopMailSafely(
            url = "mailto:hello@example.com",
            sendMail = { throw UnsupportedOperationException() },
            openFallback = { false },
        )

        assertFalse(result)
    }

    @Test
    fun `returns false when fallback throws`() {
        val result = openDesktopMailSafely(
            url = "mailto:hello@example.com",
            sendMail = { throw UnsupportedOperationException() },
            openFallback = { throw IOException("Could not open fallback") },
        )

        assertFalse(result)
    }

    @Test
    fun `malformed uri uses fallback`() {
        var sendMailCalled = false
        var fallbackCalled = false

        val result = openDesktopMailSafely(
            url = "mailto:hello@example.com?subject=bad%escape",
            sendMail = { _: URI -> sendMailCalled = true },
            openFallback = {
                fallbackCalled = true
                true
            },
        )

        assertTrue(result)
        assertFalse(sendMailCalled)
        assertTrue(fallbackCalled)
    }

    @Test
    fun `E2E uri log records mail without sending it`() {
        val log = File.createTempFile("ffe2e-uris", ".txt").apply { deleteOnExit() }
        val properties = mapOf(DesktopE2eProperties.OPENED_URI_LOG to log.absolutePath)
        DesktopE2eHooks.install(AppEnvironment.Debug) { properties[it] }
        try {
            var sendMailCalled = false

            val result = openDesktopMailSafely(
                url = "mailto:a@b.c",
                sendMail = { sendMailCalled = true },
                openFallback = { true },
            )

            assertTrue(result)
            assertFalse(sendMailCalled)
            assertEquals(listOf("mailto:a@b.c"), log.readLines())
        } finally {
            DesktopE2eHooks.reset()
        }
    }
}
