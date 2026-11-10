package com.prof18.feedflow.desktop.e2e.harness

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder

/** Serves the files under e2e/fixtures on 127.0.0.1, so flows never touch the network. */
internal class FixtureServer private constructor(
    private val server: HttpServer,
    private val fixturesDir: File,
) : AutoCloseable {

    val baseUrl: String = "http://127.0.0.1:${server.address.port}"

    fun url(path: String): String = baseUrl + "/" + path.removePrefix("/")

    private fun handle(exchange: HttpExchange) {
        exchange.use {
            val path = URLDecoder.decode(exchange.requestURI.rawPath, Charsets.UTF_8).removePrefix("/")
            val file = File(fixturesDir, path).canonicalFile
            val insideFixtures = file.path.startsWith(fixturesDir.path + File.separator)
            if (!insideFixtures || !file.isFile) {
                exchange.sendResponseHeaders(NOT_FOUND, -1)
                return
            }
            val bytes = file.readBytes()
            exchange.responseHeaders.add("Content-Type", contentType(file.extension))
            exchange.sendResponseHeaders(OK, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        }
    }

    override fun close() {
        server.stop(0)
    }

    companion object {
        private const val OK = 200
        private const val NOT_FOUND = 404

        fun start(repoRoot: File): FixtureServer {
            val httpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
            val fixtureServer = FixtureServer(httpServer, File(repoRoot, "e2e/fixtures").canonicalFile)
            httpServer.createContext("/") { fixtureServer.handle(it) }
            httpServer.start()
            return fixtureServer
        }

        private fun contentType(extension: String): String = when (extension.lowercase()) {
            "xml" -> "application/rss+xml"
            "html" -> "text/html"
            "wav" -> "audio/wav"
            "csv" -> "text/csv"
            else -> "application/octet-stream"
        }
    }
}
