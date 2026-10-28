package com.prof18.feedflow.desktop.startup

import co.touchlab.kermit.Logger
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
    fun listen(appDataPath: File, onActivate: () -> Unit): Boolean {
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

    private fun acceptRequests(server: ServerSocketChannel, onActivate: () -> Unit) {
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

    private fun handleRequest(client: SocketChannel, onActivate: () -> Unit) {
        val buffer = ByteBuffer.allocate(MAX_MESSAGE_BYTES)
        while (buffer.hasRemaining() && client.read(buffer) >= 0) {
            if (buffer.array().take(buffer.position()).contains(NEW_LINE)) break
        }
        val message = String(buffer.array(), 0, buffer.position(), Charsets.UTF_8).trim()
        if (message != ACTIVATE_MESSAGE) return
        onActivate()
        client.write(ByteBuffer.wrap("$ACTIVATED_REPLY\n".toByteArray(Charsets.UTF_8)))
    }

    companion object {
        private const val SOCKET_FILE_NAME = "FeedFlow.sock"
        private const val ACTIVATE_MESSAGE = "activate"
        private const val ACTIVATED_REPLY = "ok"
        private const val MAX_MESSAGE_BYTES = 64
        private const val NEW_LINE = '\n'.code.toByte()
        private const val DEFAULT_TIMEOUT_MILLIS = 2_000L
        private const val MAX_SOCKET_PATH_BYTES = 100
        private const val FOLDER_ID_BYTES = 8

        /** Returns true only when the running instance confirmed it brought its window forward. */
        fun requestActivation(appDataPath: File, timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS): Boolean {
            val channel = try {
                SocketChannel.open(StandardProtocolFamily.UNIX)
            } catch (_: IOException) {
                return false
            } catch (_: UnsupportedOperationException) {
                return false
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
                channel.connect(UnixDomainSocketAddress.of(socketPath(appDataPath)))
                channel.write(ByteBuffer.wrap("$ACTIVATE_MESSAGE\n".toByteArray(Charsets.UTF_8)))
                channel.shutdownOutput()
                readReply(channel) == ACTIVATED_REPLY
            } catch (_: IOException) {
                false
            } catch (_: InvalidPathException) {
                false
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
