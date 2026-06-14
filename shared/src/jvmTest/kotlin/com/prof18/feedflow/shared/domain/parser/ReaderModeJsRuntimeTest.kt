package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.shared.test.testLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReaderModeJsRuntimeTest {

    @Test
    fun `reader runtime parses an article on GraalJS with linkedom`() {
        val articleHtml = buildString {
            append("<html><head><title>Test Article Title</title>")
            append("<meta property=\"og:site_name\" content=\"Test Site\">")
            append("</head><body>")
            append("<header><nav><a href=\"/\">Home</a></nav></header>")
            append("<article><h1>Test Article Title</h1>")
            repeat(10) { index ->
                append("<p>This is paragraph number $index of the article body. ")
                append("It contains enough meaningful text so the extractor keeps it around ")
                append("instead of discarding it as boilerplate content.</p>")
            }
            append("<img src=\"/images/photo.png\">")
            append("<iframe src=\"https://www.youtube.com/embed/abc123\"></iframe>")
            append("<script>localStorage.getItem('darkmode');</script>")
            append("<a href=\"/other-post\">Read more</a>")
            append("</article>")
            append("<footer>Copyright footer junk</footer>")
            append("</body></html>")
        }

        val runtime = ReaderModeJsRuntime(testLogger)
        try {
            runtime.warmUp()
            val result = runtime.parse(articleHtml, "https://example.com/posts/test-article", null)
            assertNotNull(result, "reader runtime should parse the article")

            val content = result.content
            assertEquals("Test Article Title", result.title)
            assertEquals("Test Site", result.siteName)
            assertTrue(content.contains("paragraph number 0"), "article body should be kept")
            assertTrue(content.contains("paragraph number 9"), "article body should be kept")
            assertTrue(!content.contains("localStorage"), "script body should be stripped")
            assertTrue(!content.contains("Copyright footer junk"), "footer boilerplate should be stripped")
            assertTrue(content.length > 200, "expected substantial content, got ${content.length} chars")
            // Content must be Markdown, not raw HTML, otherwise
            // the desktop markdown renderer drops the body.
            assertTrue(!content.contains("<p>"), "content should be Markdown, not HTML: ${content.take(120)}")
            assertTrue(!content.contains("<article"), "content should be Markdown, not HTML: ${content.take(120)}")
        } finally {
            runtime.close()
        }
    }
}
