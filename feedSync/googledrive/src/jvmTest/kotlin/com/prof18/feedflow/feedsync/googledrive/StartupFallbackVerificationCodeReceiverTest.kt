package com.prof18.feedflow.feedsync.googledrive

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class StartupFallbackVerificationCodeReceiverTest {

    @Test
    fun `primary receiver handles auth when startup succeeds`() {
        val primary = FakeVerificationCodeReceiver(
            redirectUriValue = "http://127.0.0.1:1234/Callback",
            codeValue = "primary-code",
        )
        var fallbackCreated = false
        val receiver = StartupFallbackVerificationCodeReceiver(
            primaryReceiver = primary,
            fallbackReceiverFactory = {
                fallbackCreated = true
                FakeVerificationCodeReceiver()
            },
        )

        assertEquals("http://127.0.0.1:1234/Callback", receiver.redirectUri)
        assertEquals("primary-code", receiver.waitForCode())
        receiver.stop()

        assertEquals(false, fallbackCreated)
        assertEquals(1, primary.stopCallCount)
    }

    @Test
    fun `fallback receiver handles auth when primary startup fails`() {
        val primaryFailure = IOException("primary startup failed")
        val primary = FakeVerificationCodeReceiver(redirectUriFailure = primaryFailure)
        val fallback = FakeVerificationCodeReceiver(
            redirectUriValue = "http://127.0.0.1:5678/Callback",
            codeValue = "fallback-code",
        )
        var reportedFailure: IOException? = null
        val receiver = StartupFallbackVerificationCodeReceiver(
            primaryReceiver = primary,
            fallbackReceiverFactory = { fallback },
            onFallback = { reportedFailure = it },
        )

        assertEquals("http://127.0.0.1:5678/Callback", receiver.redirectUri)
        assertEquals("fallback-code", receiver.waitForCode())
        receiver.stop()

        assertSame(primaryFailure, reportedFailure)
        assertEquals(1, primary.stopCallCount)
        assertEquals(1, fallback.stopCallCount)
    }

    @Test
    fun `callback failure does not switch receivers after browser redirect is resolved`() {
        val callbackFailure = IOException("callback failed")
        val primary = FakeVerificationCodeReceiver(waitForCodeFailure = callbackFailure)
        var fallbackCreated = false
        val receiver = StartupFallbackVerificationCodeReceiver(
            primaryReceiver = primary,
            fallbackReceiverFactory = {
                fallbackCreated = true
                FakeVerificationCodeReceiver()
            },
        )

        receiver.redirectUri
        val thrown = assertFailsWith<IOException> { receiver.waitForCode() }

        assertSame(callbackFailure, thrown)
        assertEquals(false, fallbackCreated)
    }

    @Test
    fun `fallback startup failure retains primary failure`() {
        val primaryFailure = IOException("primary startup failed")
        val fallbackFailure = IOException("fallback startup failed")
        val fallback = FakeVerificationCodeReceiver(redirectUriFailure = fallbackFailure)
        val receiver = StartupFallbackVerificationCodeReceiver(
            primaryReceiver = FakeVerificationCodeReceiver(redirectUriFailure = primaryFailure),
            fallbackReceiverFactory = { fallback },
        )

        val thrown = assertFailsWith<IOException> { receiver.redirectUri }
        receiver.stop()

        assertSame(fallbackFailure, thrown)
        assertEquals(listOf(primaryFailure), thrown.suppressedExceptions)
        assertEquals(1, fallback.stopCallCount)
    }
}

private class FakeVerificationCodeReceiver(
    private val redirectUriValue: String = "http://127.0.0.1:1234/Callback",
    private val codeValue: String = "code",
    private val redirectUriFailure: IOException? = null,
    private val waitForCodeFailure: IOException? = null,
) : VerificationCodeReceiver {

    var stopCallCount = 0
        private set

    override fun getRedirectUri(): String = redirectUriFailure?.let { throw it } ?: redirectUriValue

    override fun waitForCode(): String = waitForCodeFailure?.let { throw it } ?: codeValue

    override fun stop() {
        stopCallCount += 1
    }
}
