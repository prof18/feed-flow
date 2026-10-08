package com.prof18.feedflow.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AudioEnclosureTest {

    @Test
    fun `missing or malformed enclosure urls are rejected`() {
        listOf(
            null,
            "",
            "   ",
            "episode.mp3",
            "mailto:episode@example.com",
            "ftp://example.com/episode.mp3",
            "https://100%example.com/episode.mp3",
            "https://",
            "https://?token=x",
            "https://#fragment",
            "https:///episode.mp3",
            "https://user@/episode.mp3",
        ).forEach { url ->
            assertNull(AudioEnclosure.validatedUrl(url), "url=$url")
        }
    }

    @Test
    fun `audio mime accepts extensionless url and normalizes case and parameters`() {
        assertEquals(
            "https://example.com/download?id=episode",
            AudioEnclosure.audioUrlOrNull(
                "https://example.com/download?id=episode",
                " AUDIO/MPEG ; charset=binary ",
            ),
        )
    }

    @Test
    fun `validated url trims surrounding whitespace but preserves the original url`() {
        val url = "HTTPS://example.com/episode?sig=a%2Fb"

        assertEquals(url, AudioEnclosure.validatedUrl("  $url  "))
        assertEquals("feedflow-audio://episode/open", AudioEnclosure.ACTION_URL)
    }

    @Test
    fun `audio mime and uppercase path extension preserve signed url`() {
        val signedUrl = "https://example.com/EPISODE.MP3?sig=a%2Fb&part=1#track"

        assertEquals(signedUrl, AudioEnclosure.audioUrlOrNull(signedUrl, "audio/mpeg"))
    }

    @Test
    fun `unknown mime may use a supported path extension only`() {
        val url = "https://example.com/episode.OGG?download=1"

        listOf(null, "", "application/octet-stream", "binary/octet-stream", "application/ogg")
            .forEach { mimeType ->
                assertEquals(url, AudioEnclosure.audioUrlOrNull(url, mimeType), "mimeType=$mimeType")
            }

        assertNull(AudioEnclosure.audioUrlOrNull("https://example.com/download?file=episode.mp3", null))
    }

    @Test
    fun `explicit non-audio mime rejects even when path looks like audio`() {
        listOf("image/jpeg", "video/mp4", "application/pdf").forEach { mimeType ->
            assertNull(
                AudioEnclosure.audioUrlOrNull("https://example.com/episode.mp3", mimeType),
                "mimeType=$mimeType",
            )
        }
    }

    @Test
    fun `article without enclosure has no audio url`() {
        assertNull(AudioEnclosure.audioUrlOrNull(null, null))
    }
}
