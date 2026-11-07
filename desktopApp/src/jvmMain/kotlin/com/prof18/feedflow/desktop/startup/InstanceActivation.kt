package com.prof18.feedflow.desktop.startup

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AddFeedLinkParser
import java.io.File
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Lets a second launch hand off to the running instance: the instance that holds [SingleInstanceLock]
 * listens on a Unix domain socket, normally in the data folder, and a second launch asks it to bring its
 * window forward instead of showing a message.
 */
internal class InstanceActivation(
    private val logger: Logger = Logger.withTag("InstanceActivation"),
) : AutoCloseable {

    private var server: ServerSocketChannel? = null
    private var socketPath: Path? = null

    /** Call only while holding [SingleInstanceLock], since it replaces any socket file left by a crash. */
    fun listen(appDataPath: File, onActivate: (feedUrl: String?) -> Unit): Boolean {
        val path = try {
            socketPath(appDataPath)
        } catch (e: InvalidPathException) {
            logger.w(e) { "Instance activation unavailable" }
            return false
        }
        val newServer = try {
            Files.deleteIfExists(path)
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).apply {
                bind(UnixDomainSocketAddress.of(path))
            }
        } catch (e: IOException) {
            // An unusable folder or an old Windows build lands here; a second launch then falls back to
            // the already-running message.
            logger.w(e) { "Instance activation unavailable" }
            return false
        } catch (e: UnsupportedOperationException) {
            logger.w(e) { "Instance activation unavailable" }
            return false
        }
        server = newServer
        socketPath = path
        thread(isDaemon = true, name = "FeedFlow-instance-activation") {
            acceptRequests(newServer, onActivate)
        }
        return true
    }

    override fun close() {
        runCatching { server?.close() }
        socketPath?.let { runCatching { Files.deleteIfExists(it) } }
        server = null
        socketPath = null
    }

    private fun acceptRequests(server: ServerSocketChannel, onActivate: (feedUrl: String?) -> Unit) {
        while (server.isOpen) {
            val client = try {
                server.accept()
            } catch (_: IOException) {
                return
            }
            try {
                client.use { handleRequest(it, onActivate) }
            } catch (e: IOException) {
                logger.d(e) { "Dropped an activation request" }
            }
        }
    }

    private fun handleRequest(client: SocketChannel, onActivate: (feedUrl: String?) -> Unit) {
        val buffer = ByteBuffer.allocate(MAX_MESSAGE_BYTES)
        while (buffer.hasRemaining() && client.read(buffer) >= 0) {
            if (buffer.array().take(buffer.position()).contains(NEW_LINE)) break
        }
        if (!buffer.array().take(buffer.position()).contains(NEW_LINE)) return
        val message = String(buffer.array(), 0, buffer.position(), Charsets.UTF_8).trim()
        val feedUrl = when {
            message == ACTIVATE_MESSAGE -> null
            message.startsWith(OPEN_LINK_PREFIX) -> AddFeedLinkParser.parse(message.removePrefix(OPEN_LINK_PREFIX))
            else -> return
        }
        onActivate(feedUrl)
        client.write(ByteBuffer.wrap("$ACTIVATED_REPLY\n".toByteArray(Charsets.UTF_8)))
    }

    companion object {
        private const val SOCKET_FILE_NAME = "FeedFlow.sock"
        private const val ACTIVATE_MESSAGE = "activate"
        private const val ACTIVATED_REPLY = "ok"
        private const val OPEN_LINK_PREFIX = "open-link "
        private const val LINK_CONNECT_ATTEMPTS = 10
        private const val LINK_CONNECT_RETRY_DELAY_MILLIS = 100L
        private const val MAX_MESSAGE_BYTES = 8_192
        private const val NEW_LINE = '\n'.code.toByte()
        private const val DEFAULT_TIMEOUT_MILLIS = 2_000L
        private const val MAX_SOCKET_PATH_BYTES = 100
        private const val FOLDER_ID_BYTES = 8

        /**
         * Returns true only when the running instance confirmed it brought its window forward. A [link] is
         * forwarded so the running instance can open it.
         */
        fun requestActivation(
            appDataPath: File,
            link: String? = null,
            timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        ): Boolean {
            // The running instance takes the lock a moment before its listener starts, so a link launched in
            // that gap retries instead of being lost. A plain activation keeps a single attempt.
            val attempts = if (link == null) 1 else LINK_CONNECT_ATTEMPTS
            repeat(attempts) { attempt ->
                when (sendRequest(appDataPath, link, timeoutMillis)) {
                    RequestResult.Activated -> return true
                    RequestResult.Failed -> return false
                    RequestResult.NotConnected -> if (attempt < attempts - 1) {
                        Thread.sleep(LINK_CONNECT_RETRY_DELAY_MILLIS)
                    }
                }
            }
            return false
        }

        private fun sendRequest(appDataPath: File, link: String?, timeoutMillis: Long): RequestResult {
            val channel = try {
                SocketChannel.open(StandardProtocolFamily.UNIX)
            } catch (_: IOException) {
                return RequestResult.Failed
            } catch (_: UnsupportedOperationException) {
                return RequestResult.Failed
            }
            // A blocking Unix socket has no read timeout, so a watchdog closes it instead.
            val watchdog = thread(isDaemon = true, name = "FeedFlow-activation-timeout") {
                try {
                    Thread.sleep(timeoutMillis)
                    runCatching { channel.close() }
                } catch (_: InterruptedException) {
                    // The request finished in time.
                }
            }
            return try {
                try {
                    channel.connect(UnixDomainSocketAddress.of(socketPath(appDataPath)))
                } catch (_: IOException) {
                    return RequestResult.NotConnected
                }
                val message = if (link == null) "$ACTIVATE_MESSAGE\n" else "$OPEN_LINK_PREFIX$link\n"
                channel.write(ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)))
                channel.shutdownOutput()
                if (readReply(channel) == ACTIVATED_REPLY) RequestResult.Activated else RequestResult.Failed
            } catch (_: IOException) {
                RequestResult.Failed
            } catch (_: InvalidPathException) {
                RequestResult.Failed
            } finally {
                watchdog.interrupt()
                runCatching { channel.close() }
            }
        }

        private fun readReply(channel: SocketChannel): String {
            val buffer = ByteBuffer.allocate(MAX_MESSAGE_BYTES)
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // Read until the instance closes the connection.
            }
            return String(buffer.array(), 0, buffer.position(), Charsets.UTF_8).trim()
        }

        // macOS caps Unix socket paths at 103 bytes, Linux and Windows at 107. The sandboxed Mac App Store data
        // folder alone is close to that, so a deep folder uses a per-folder name in the temp folder instead.
        private fun socketPath(appDataPath: File): Path {
            val inDataFolder = File(appDataPath, SOCKET_FILE_NAME).toPath()
            if (inDataFolder.toString().toByteArray(Charsets.UTF_8).size <= MAX_SOCKET_PATH_BYTES) {
                return inDataFolder
            }
            val folderId = MessageDigest.getInstance("SHA-256")
                .digest(appDataPath.absolutePath.toByteArray(Charsets.UTF_8))
                .take(FOLDER_ID_BYTES)
                .joinToString("") { "%02x".format(Locale.ROOT, it) }
            return Path.of(System.getProperty("java.io.tmpdir"), "feedflow-$folderId.sock")
        }
    }
}

private enum class RequestResult {
    Activated,
    NotConnected,
    Failed,
}
