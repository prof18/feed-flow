package com.prof18.feedflow.feedsync.googledrive

import java.io.IOException
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlockingLoopbackReceiverTest {

    @Test
    fun `receiver binds to IPv4 loopback on a dynamic port and returns code`() {
        val receiver = BlockingLoopbackReceiver()
        val redirectUri = URI(receiver.redirectUri)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val code = executor.submit<String> { receiver.waitForCode() }
            val status = sendRequest(
                redirectUri = redirectUri,
                target = "${redirectUri.path}?code=code%2Fwith%2Bsymbols",
            )

            assertEquals("127.0.0.1", redirectUri.host)
            assertTrue(redirectUri.port > 0)
            assertEquals("HTTP/1.1 200 OK", status)
            assertEquals("code/with+symbols", code.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            receiver.stop()
            executor.shutdownNow()
        }
    }

    @Test
    fun `receiver returns OAuth error`() {
        val receiver = BlockingLoopbackReceiver()
        val redirectUri = URI(receiver.redirectUri)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val code = executor.submit<String> { receiver.waitForCode() }
            val status = sendRequest(
                redirectUri = redirectUri,
                target = "${redirectUri.path}?error=access_denied",
            )
            val exception = assertFailsWith<ExecutionException> {
                code.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }

            assertEquals("HTTP/1.1 200 OK", status)
            assertIs<IOException>(exception.cause)
            assertEquals("User authorization failed (access_denied)", exception.cause?.message)
        } finally {
            receiver.stop()
            executor.shutdownNow()
        }
    }

    @Test
    fun `receiver ignores unrelated requests before callback`() {
        val receiver = BlockingLoopbackReceiver()
        val redirectUri = URI(receiver.redirectUri)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val code = executor.submit<String> { receiver.waitForCode() }

            assertEquals("HTTP/1.1 404 Not Found", sendRequest(redirectUri, "/favicon.ico"))
            assertEquals(
                "HTTP/1.1 200 OK",
                sendRequest(redirectUri, "${redirectUri.path}?code=expected-code"),
            )
            assertEquals("expected-code", code.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            receiver.stop()
            executor.shutdownNow()
        }
    }

    @Test
    fun `stopping receiver unblocks callback wait`() {
        val receiver = BlockingLoopbackReceiver()
        val redirectUri = URI(receiver.redirectUri)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val code = executor.submit<String> { receiver.waitForCode() }
            assertEquals("HTTP/1.1 404 Not Found", sendRequest(redirectUri, "/favicon.ico"))

            receiver.stop()
            val exception = assertFailsWith<ExecutionException> {
                code.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }

            assertIs<IOException>(exception.cause)
            assertEquals("Loopback receiver was stopped", exception.cause?.message)
        } finally {
            receiver.stop()
            executor.shutdownNow()
        }
    }

    private fun sendRequest(
        redirectUri: URI,
        target: String,
    ): String = Socket(redirectUri.host, redirectUri.port).use { socket ->
        socket.soTimeout = SOCKET_TIMEOUT_MILLIS
        socket.getOutputStream().bufferedWriter(StandardCharsets.US_ASCII).apply {
            write("GET $target HTTP/1.1\r\n")
            write("Host: ${redirectUri.host}:${redirectUri.port}\r\n")
            write("Connection: close\r\n\r\n")
            flush()
        }
        socket.getInputStream().bufferedReader(StandardCharsets.US_ASCII).readLine()
    }
}

private const val TEST_TIMEOUT_SECONDS = 5L
private const val SOCKET_TIMEOUT_MILLIS = 5_000
