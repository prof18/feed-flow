package com.prof18.feedflow.desktop.ui

import coil3.PlatformContext
import coil3.executeBlocking
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopImageLoaderTest {

    @Test
    fun `network requests accept image content`() {
        val receivedAcceptHeader = AtomicReference<String?>()
        val server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/image") { exchange ->
                receivedAcceptHeader.set(exchange.requestHeaders.getFirst("Accept"))
                exchange.responseHeaders.add("Content-Type", "image/png")
                exchange.sendResponseHeaders(200, PNG_BYTES.size.toLong())
                exchange.responseBody.use { it.write(PNG_BYTES) }
            }
            start()
        }
        val imageLoader = coilImageLoader(
            context = PlatformContext.INSTANCE,
            debug = false,
        )

        try {
            imageLoader.executeBlocking(
                ImageRequest.Builder(PlatformContext.INSTANCE)
                    .data("http://127.0.0.1:${server.address.port}/image")
                    .build(),
            )

            assertEquals("image/*", receivedAcceptHeader.get())
        } finally {
            imageLoader.shutdown()
            server.stop(0)
        }
    }

    @Test
    fun `svg favicons decode into an image`() {
        // On this platform Coil renders SVG through Skia, not the decoder the mobile apps use.
        val server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/favicon.svg") { exchange ->
                exchange.responseHeaders.add("Content-Type", "image/svg+xml")
                exchange.sendResponseHeaders(200, SVG_BYTES.size.toLong())
                exchange.responseBody.use { it.write(SVG_BYTES) }
            }
            start()
        }
        val imageLoader = coilImageLoader(
            context = PlatformContext.INSTANCE,
            debug = false,
        )

        try {
            val result = imageLoader.executeBlocking(
                ImageRequest.Builder(PlatformContext.INSTANCE)
                    .data("http://127.0.0.1:${server.address.port}/favicon.svg")
                    .build(),
            )

            val image = assertIs<SuccessResult>(result).image
            assertEquals(expected = 24, actual = image.width)
            assertEquals(expected = 12, actual = image.height)
        } finally {
            imageLoader.shutdown()
            server.stop(0)
        }
    }

    private companion object {
        val SVG_BYTES: ByteArray =
            """<svg xmlns="http://www.w3.org/2000/svg" width="24" height="12"><rect width="24" height="12" fill="#ff6600"/></svg>"""
                .toByteArray()

        val PNG_BYTES: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
        )
    }
}
