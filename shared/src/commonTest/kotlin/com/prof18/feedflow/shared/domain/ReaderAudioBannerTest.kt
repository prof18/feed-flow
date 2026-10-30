package com.prof18.feedflow.shared.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderAudioBannerTest {

    @Test
    fun `mobile card keeps playback and external actions distinct and escapes metadata`() {
        val html = getReaderModeStyledHtml(
            colors = null,
            content = "<p>Show notes</p>",
            fontSize = 18,
            audioBanner = ReaderAudioBanner(
                title = "Episode <one>",
                audioLabel = "Audio",
                openAudioLabel = "Open in another app",
                untitledAudioLabel = "Episode",
                enablePlayback = true,
                subtitle = "Author & show",
                imageUrl = "https://example.com/art.jpg?a=1&b=2",
                playAudioLabel = "Play / pause",
            ),
        )
        assertTrue(html.contains("id=\"reader_play_audio\""))
        assertTrue(html.contains("href=\"feedflow-audio://episode/play\""))
        assertTrue(html.contains("href=\"feedflow-audio://episode/open\""))
        assertTrue(html.contains("Author &amp; show"))
        assertTrue(html.contains("Episode &lt;one&gt;"))
        assertTrue(html.contains("https://example.com/art.jpg?a=1&amp;b=2"))
        assertFalse(html.contains("<audio"))
    }

    @Test
    fun `reader audio banner renders escaped fallback for null or blank title`() {
        listOf(null, "  ").forEach { title ->
            val html = getReaderModeStyledHtml(
                colors = null,
                content = "<p>Show notes</p>",
                fontSize = 18,
                audioBanner = ReaderAudioBanner(
                    title = title,
                    audioLabel = "Audio",
                    openAudioLabel = "Open audio",
                    untitledAudioLabel = "Episode <untitled> & more",
                ),
            )

            assertTrue(html.contains("Episode &lt;untitled&gt; &amp; more"))
            assertTrue(html.contains("href=\"feedflow-audio://episode/open\""))
        }
    }
}
