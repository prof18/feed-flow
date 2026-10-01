package com.prof18.feedflow.desktop.utils

import java.io.IOException
import java.net.URI
import kotlin.test.Test
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
}
