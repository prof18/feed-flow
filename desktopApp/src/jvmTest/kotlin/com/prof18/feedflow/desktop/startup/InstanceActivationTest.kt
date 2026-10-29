package com.prof18.feedflow.desktop.startup

import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstanceActivationTest {

    private val directory: File = createShortTempDirectory("ff")
    private val activation = InstanceActivation()

    @AfterTest
    fun tearDown() {
        activation.close()
        directory.deleteRecursively()
    }

    @Test
    fun `a request reaches the listening instance`() {
        val activated = CountDownLatch(1)
        assertTrue(activation.listen(directory) { activated.countDown() })

        assertTrue(InstanceActivation.requestActivation(directory))
        assertTrue(activated.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun `every request is answered, not only the first`() {
        val activated = CountDownLatch(2)
        activation.listen(directory) { activated.countDown() }

        assertTrue(InstanceActivation.requestActivation(directory))
        assertTrue(InstanceActivation.requestActivation(directory))
        assertTrue(activated.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun `no listener means the request fails`() {
        assertFalse(InstanceActivation.requestActivation(directory))
    }

    @Test
    fun `a socket file left behind by a crashed instance fails the request and is replaced by the next listener`() {
        File(directory, "FeedFlow.sock").writeText("stale")

        assertFalse(InstanceActivation.requestActivation(directory))

        val activated = CountDownLatch(1)
        assertTrue(activation.listen(directory) { activated.countDown() })
        assertTrue(InstanceActivation.requestActivation(directory))
        assertTrue(activated.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun `a listener that never answers fails the request within the timeout`() {
        val address = UnixDomainSocketAddress.of(File(directory, "FeedFlow.sock").toPath())
        ServerSocketChannel.open(StandardProtocolFamily.UNIX).use { server ->
            server.bind(address)

            val started = System.nanoTime()
            assertFalse(InstanceActivation.requestActivation(directory, timeoutMillis = 300))
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < TIMEOUT_SECONDS * 1_000)
        }
    }

    @Test
    fun `an unknown message does not activate the window`() {
        val activations = AtomicInteger()
        activation.listen(directory) { activations.incrementAndGet() }

        SocketChannel.open(UnixDomainSocketAddress.of(File(directory, "FeedFlow.sock").toPath())).use { channel ->
            channel.write(ByteBuffer.wrap("hello\n".toByteArray()))
            channel.shutdownOutput()
            val reply = ByteBuffer.allocate(16)
            channel.read(reply)
            assertEquals(0, reply.position())
        }
        // A real request after it still works, so the bad one was handled and dropped.
        assertTrue(InstanceActivation.requestActivation(directory))
        assertEquals(1, activations.get())
    }

    @Test
    fun `a data folder too deep for a socket path still gets activation through the temp folder`() {
        val deepFolder = File(directory, "a".repeat(DEEP_FOLDER_NAME_LENGTH)).apply { mkdirs() }
        val activated = CountDownLatch(1)

        assertTrue(activation.listen(deepFolder) { activated.countDown() })
        assertTrue(InstanceActivation.requestActivation(deepFolder))
        assertTrue(activated.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        // Another data folder must not reach this instance.
        assertFalse(InstanceActivation.requestActivation(File(directory, "other").apply { mkdirs() }))
    }

    @Test
    fun `a folder that cannot hold the socket makes listen return false`() {
        val notADirectory = File(directory, "file").apply { writeText("x") }

        assertFalse(activation.listen(notADirectory) {})
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
        const val DEEP_FOLDER_NAME_LENGTH = 120
    }
}
