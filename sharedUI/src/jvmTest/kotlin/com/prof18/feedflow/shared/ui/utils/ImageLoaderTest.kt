package com.prof18.feedflow.shared.ui.utils

import coil3.PlatformContext
import coil3.executeBlocking
import coil3.request.ImageRequest
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageLoaderTest {

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

    private companion object {
        val PNG_BYTES: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
        )
    }
}
