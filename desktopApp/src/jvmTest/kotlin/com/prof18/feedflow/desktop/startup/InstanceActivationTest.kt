package com.prof18.feedflow.desktop.startup

import com.prof18.feedflow.core.utils.AddFeedLinkParser
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        assertTrue(activation.listen(directory) { _ -> activated.countDown() })

        assertTrue(InstanceActivation.requestActivation(directory))
        assertTrue(activated.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun `every request is answered, not only the first`() {
        val activated = CountDownLatch(2)
        activation.listen(directory) { _ -> activated.countDown() }

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
        assertTrue(activation.listen(directory) { _ -> activated.countDown() })
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
        activation.listen(directory) { _ -> activations.incrementAndGet() }

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

        assertTrue(activation.listen(deepFolder) { _ -> activated.countDown() })
        assertTrue(InstanceActivation.requestActivation(deepFolder))
        assertTrue(activated.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        // Another data folder must not reach this instance.
        assertFalse(InstanceActivation.requestActivation(File(directory, "other").apply { mkdirs() }))
    }

    @Test
    fun `a folder that cannot hold the socket makes listen return false`() {
        val notADirectory = File(directory, "file").apply { writeText("x") }

        assertFalse(activation.listen(notADirectory) { _ -> })
    }

    @Test
    fun `an open-link request delivers the parsed feed url`() {
        val received = CopyOnWriteArrayList<String?>()
        activation.listen(directory) { feedUrl -> received.add(feedUrl) }

        assertTrue(InstanceActivation.requestActivation(directory, link = "feed://example.com/feed.xml"))
        assertEquals(listOf<String?>("https://example.com/feed.xml"), received.toList())
    }

    @Test
    fun `a plain activation delivers no url`() {
        val received = CopyOnWriteArrayList<String?>()
        activation.listen(directory) { feedUrl -> received.add(feedUrl) }

        assertTrue(InstanceActivation.requestActivation(directory))
        assertEquals(listOf<String?>(null), received.toList())
    }

    @Test
    fun `a long unicode link arrives intact`() {
        val received = CopyOnWriteArrayList<String?>()
        activation.listen(directory) { feedUrl -> received.add(feedUrl) }
        val link = "feed://example.com/" + "\u6F22".repeat(LONG_LINK_CHARACTERS)
        val expected = AddFeedLinkParser.parse(link)
        assertNotNull(expected)

        assertTrue(InstanceActivation.requestActivation(directory, link = link))
        assertEquals(listOf<String?>(expected), received.toList())
    }

    @Test
    fun `an oversized message without a newline is ignored`() {
        val received = CopyOnWriteArrayList<String?>()
        activation.listen(directory) { feedUrl -> received.add(feedUrl) }

        SocketChannel.open(UnixDomainSocketAddress.of(File(directory, "FeedFlow.sock").toPath())).use { channel ->
            channel.write(ByteBuffer.wrap(ByteArray(OVERSIZED_MESSAGE_BYTES) { 'a'.code.toByte() }))
        }

        assertTrue(InstanceActivation.requestActivation(directory))
        assertEquals(listOf<String?>(null), received.toList())
    }

    @Test
    fun `a link waits for a listener that starts late`() {
        val received = CopyOnWriteArrayList<String?>()
        val listener = thread {
            Thread.sleep(LATE_LISTENER_DELAY_MILLIS)
            activation.listen(directory) { feedUrl -> received.add(feedUrl) }
        }

        assertTrue(InstanceActivation.requestActivation(directory, link = "feed://example.com/feed.xml"))
        listener.join()
        assertEquals(listOf<String?>("https://example.com/feed.xml"), received.toList())
    }

    @Test
    fun `an open-link request with an invalid link still activates`() {
        val received = CopyOnWriteArrayList<String?>()
        activation.listen(directory) { feedUrl -> received.add(feedUrl) }

        assertTrue(InstanceActivation.requestActivation(directory, link = "feedflow://add?url=javascript%3Aalert(1)"))
        assertEquals(listOf<String?>(null), received.toList())
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
        const val DEEP_FOLDER_NAME_LENGTH = 120
        const val LONG_LINK_CHARACTERS = 1_400
        const val OVERSIZED_MESSAGE_BYTES = 8_192
        const val LATE_LISTENER_DELAY_MILLIS = 300L
    }
}
