package com.prof18.feedflow.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderModeEligibilityTest {

    @Test
    fun `article-like http urls can use reader mode`() {
        assertTrue(ReaderModeEligibility.canOpenReaderMode("https://example.com/news/story"))
        assertTrue(ReaderModeEligibility.canOpenReaderMode("https://example.com/articles/story.html"))
    }

    @Test
    fun `non-http urls cannot use reader mode`() {
        assertFalse(ReaderModeEligibility.canOpenReaderMode("feedflow://feed/123"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("mailto:hello@example.com"))
    }

    @Test
    fun `media and download extensions cannot use reader mode`() {
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://example.com/podcast/episode.mp3"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://example.com/video/clip.MP4?token=abc"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://example.com/report.pdf"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://example.com/files/archive.tar.gz"))
    }

    @Test
    fun `known media hosts cannot use reader mode`() {
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://youtube.com/watch?v=abc"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://www.youtube.com/watch?v=abc"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://youtu.be/abc"))
    }

    @Test
    fun `telegram trigger hosts cannot use reader mode`() {
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://t.me/example_channel/123"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://www.t.me/example_channel/123"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://telegram.me/example_channel/123"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://telegram.dog/example_channel/123"))
    }

    @Test
    fun `pdf and download query hints cannot use reader mode`() {
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://example.com/article?type=pdf"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://example.com/article?format=pdf"))
        assertFalse(ReaderModeEligibility.canOpenReaderMode("https://example.com/article?download=1"))
    }

    @Test
    fun `feed item browser-only flags prevent reader mode`() {
        val urlInfo = FeedItemUrlInfo(
            id = "id",
            url = "https://example.com/article",
            title = "Title",
            openOnlyOnBrowser = true,
            isBookmarked = false,
            linkOpeningPreference = LinkOpeningPreference.DEFAULT,
            commentsUrl = null,
        )

        assertFalse(urlInfo.canOpenReaderMode())
    }
}
