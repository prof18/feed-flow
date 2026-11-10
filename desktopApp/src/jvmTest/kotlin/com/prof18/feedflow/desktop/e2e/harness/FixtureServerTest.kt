package com.prof18.feedflow.desktop.e2e.harness

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixtureServerTest {

    @Test
    fun `serves fixture feeds and refuses paths outside the fixtures folder`() {
        FixtureServer.start(repoRoot()).use { server ->
            val client = HttpClient.newHttpClient()

            val feed = client.send(
                HttpRequest.newBuilder(URI(server.url("/feeds/feedflow-e2e-feed.xml"))).build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, feed.statusCode())
            assertEquals("application/rss+xml", feed.headers().firstValue("Content-Type").orElse(""))
            assertTrue(feed.body().contains(FIXTURE_FEED_TITLE))

            val outside = client.send(
                HttpRequest.newBuilder(URI("${server.baseUrl}/../settings.gradle.kts")).build(),
                HttpResponse.BodyHandlers.discarding(),
            )
            assertEquals(404, outside.statusCode())

            val encoded = client.send(
                HttpRequest.newBuilder(URI("${server.baseUrl}/%2e%2e/settings.gradle.kts")).build(),
                HttpResponse.BodyHandlers.discarding(),
            )
            assertEquals(404, encoded.statusCode())
        }
    }
}
