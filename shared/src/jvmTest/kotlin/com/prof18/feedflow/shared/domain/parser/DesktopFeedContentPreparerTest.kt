package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.testLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopFeedContentPreparerTest {

    private val preparer = DesktopFeedContentPreparer(
        logger = testLogger,
        dispatcherProvider = TestDispatcherProvider,
    )

    @Test
    fun `prepare converts feed html to markdown`() = runTest {
        val markdown = preparer.prepare(
            html = "<p>First <strong>bold</strong> paragraph with " +
                "<a href=\"https://example.com/link\">a link</a>.</p>" +
                "<h2>Section</h2><ul><li>One</li><li>Two</li></ul>",
            baseUrl = "https://example.com/post",
        )

        assertTrue("**bold**" in markdown, markdown)
        assertTrue("[a link](https://example.com/link)" in markdown, markdown)
        assertTrue("## Section" in markdown, markdown)
        assertTrue("One" in markdown && "Two" in markdown, markdown)
        assertFalse("<p>" in markdown, markdown)
    }

    @Test
    fun `prepare keeps short feed content`() = runTest {
        val markdown = preparer.prepare(
            html = "<p>Short note.</p>",
            baseUrl = "https://example.com/post",
        )

        assertTrue("Short note." in markdown, markdown)
        assertFalse("<p>" in markdown, markdown)
    }

    @Test
    fun `prepare keeps image-only feed content`() = runTest {
        val markdown = preparer.prepare(
            html = "<img src=\"https://example.com/photo.jpg\" alt=\"Photo\">",
            baseUrl = "https://example.com/post",
        )

        assertTrue("https://example.com/photo.jpg" in markdown, markdown)
        assertFalse("<img" in markdown, markdown)
    }

    @Test
    fun `prepare resolves relative urls against the base url`() = runTest {
        val markdown = preparer.prepare(
            html = "<p>Read <a href=\"/more\">more</a> of this longer paragraph about the topic.</p>" +
                "<p><img src=\"images/pic.png\" alt=\"Pic\"></p>",
            baseUrl = "https://example.com/blog/post",
        )

        assertTrue("https://example.com/more" in markdown, markdown)
        assertTrue("https://example.com/blog/images/pic.png" in markdown, markdown)
    }

    @Test
    fun `prepare uses lazy-loaded image sources`() = runTest {
        val markdown = preparer.prepare(
            html = "<p>Some text around a lazy image in the feed body.</p>" +
                "<img src=\"data:image/gif;base64,R0lGODlhAQABAAAAACw=\" data-src=\"https://example.com/lazy.jpg\">",
            baseUrl = "https://example.com/post",
        )

        assertTrue("https://example.com/lazy.jpg" in markdown, markdown)
    }

    @Test
    fun `prepare works without a base url`() = runTest {
        val markdown = preparer.prepare(
            html = "<p>No base url here.</p>",
            baseUrl = null,
        )

        assertTrue("No base url here." in markdown, markdown)
    }
}
