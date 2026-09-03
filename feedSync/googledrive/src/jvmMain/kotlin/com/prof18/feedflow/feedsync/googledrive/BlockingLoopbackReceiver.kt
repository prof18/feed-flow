package com.prof18.feedflow.feedsync.googledrive

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal class BlockingLoopbackReceiver : VerificationCodeReceiver {

    private val lock = Any()
    private var serverSocket: ServerSocket? = null

    override fun getRedirectUri(): String = synchronized(lock) {
        if (serverSocket != null) {
            throw IOException("Loopback receiver is already running")
        }

        val socket = ServerSocket()
        try {
            socket.bind(InetSocketAddress(LOOPBACK_HOST, 0))
            serverSocket = socket
        } catch (e: IOException) {
            socket.close()
            throw e
        }

        "http://$LOOPBACK_HOST:${socket.localPort}$CALLBACK_PATH"
    }

    override fun waitForCode(): String {
        val listeningSocket = synchronized(lock) {
            serverSocket ?: throw IOException("Loopback receiver has not been started")
        }

        while (true) {
            val callback = try {
                listeningSocket.accept().use(::readCallback)
            } catch (e: SocketException) {
                if (listeningSocket.isClosed) {
                    throw IOException("Loopback receiver was stopped", e)
                }
                throw e
            }

            when (callback) {
                is CallbackResult.Code -> return callback.value
                is CallbackResult.Error -> throw IOException("User authorization failed (${callback.value})")
                CallbackResult.Ignored -> Unit
            }
        }
    }

    override fun stop() {
        val socket = synchronized(lock) {
            serverSocket.also { serverSocket = null }
        }
        socket?.close()
    }

    private fun readCallback(socket: Socket): CallbackResult {
        socket.soTimeout = SOCKET_READ_TIMEOUT_MILLIS

        val requestLine = try {
            readRequestLine(socket)
        } catch (_: IOException) {
            socket.sendResponse(HTTP_BAD_REQUEST)
            return CallbackResult.Ignored
        }

        val parts = requestLine.split(' ', limit = REQUEST_LINE_PARTS)
        if (parts.size != REQUEST_LINE_PARTS || parts[0] != HTTP_GET || !parts[2].startsWith(HTTP_VERSION_PREFIX)) {
            socket.sendResponse(HTTP_BAD_REQUEST)
            return CallbackResult.Ignored
        }

        val uri = try {
            URI(parts[1])
        } catch (_: URISyntaxException) {
            socket.sendResponse(HTTP_BAD_REQUEST)
            return CallbackResult.Ignored
        }

        if (uri.path != CALLBACK_PATH) {
            socket.sendResponse(HTTP_NOT_FOUND)
            return CallbackResult.Ignored
        }

        val query = try {
            parseQuery(uri.rawQuery)
        } catch (_: IllegalArgumentException) {
            socket.sendResponse(HTTP_BAD_REQUEST)
            return CallbackResult.Ignored
        }

        query["error"]?.let { error ->
            socket.sendResponse(HTTP_OK)
            return CallbackResult.Error(error)
        }

        val code = query["code"]
        if (code.isNullOrEmpty()) {
            socket.sendResponse(HTTP_BAD_REQUEST)
            return CallbackResult.Ignored
        }

        socket.sendResponse(HTTP_OK)
        return CallbackResult.Code(code)
    }

    private fun readRequestLine(socket: Socket): String {
        val requestLine = ByteArrayOutputStream()
        val input = socket.getInputStream()

        while (true) {
            when (val byte = input.read()) {
                -1, NEW_LINE -> break
                CARRIAGE_RETURN -> Unit
                else -> {
                    if (requestLine.size() >= MAX_REQUEST_LINE_BYTES) {
                        throw IOException("HTTP request line is too long")
                    }
                    requestLine.write(byte)
                }
            }
        }

        return requestLine.toString(StandardCharsets.US_ASCII)
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) {
            return emptyMap()
        }

        return buildMap {
            rawQuery.split('&').forEach { parameter ->
                val separatorIndex = parameter.indexOf('=')
                val rawName = if (separatorIndex >= 0) parameter.substring(0, separatorIndex) else parameter
                val rawValue = if (separatorIndex >= 0) parameter.substring(separatorIndex + 1) else ""
                put(decodeQueryPart(rawName), decodeQueryPart(rawValue))
            }
        }
    }

    private fun decodeQueryPart(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

    private fun Socket.sendResponse(status: String) {
        try {
            getOutputStream().write(
                buildString {
                    append("HTTP/1.1 ")
                    append(status)
                    append("\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
                }.toByteArray(StandardCharsets.US_ASCII),
            )
            getOutputStream().flush()
        } catch (_: IOException) {
            // The OAuth result is still valid if the browser closes before reading the response.
        }
    }
}

private sealed interface CallbackResult {
    data class Code(val value: String) : CallbackResult
    data class Error(val value: String) : CallbackResult
    data object Ignored : CallbackResult
}

private const val LOOPBACK_HOST = "127.0.0.1"
private const val CALLBACK_PATH = "/Callback"
private const val HTTP_GET = "GET"
private const val HTTP_VERSION_PREFIX = "HTTP/"
private const val HTTP_OK = "200 OK"
private const val HTTP_BAD_REQUEST = "400 Bad Request"
private const val HTTP_NOT_FOUND = "404 Not Found"
private const val REQUEST_LINE_PARTS = 3
private const val SOCKET_READ_TIMEOUT_MILLIS = 5_000
private const val MAX_REQUEST_LINE_BYTES = 8_192
private const val CARRIAGE_RETURN = '\r'.code
private const val NEW_LINE = '\n'.code
