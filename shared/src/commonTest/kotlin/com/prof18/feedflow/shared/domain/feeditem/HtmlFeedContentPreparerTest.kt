package com.prof18.feedflow.shared.domain.feeditem

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlFeedContentPreparerTest {

    @Test
    fun `feed html is handed to the webview untouched`() = runTest {
        val html = """
            <p>Article</p>
            <iframe src="https://www.youtube.com/embed/abc"></iframe>
            <a href="https://example.com/story">safe</a>
        """.trimIndent()

        val prepared = HtmlFeedContentPreparer().prepare(
            html = html,
            baseUrl = "https://example.com",
            title = "Article",
            imageUrl = null,
            siteName = "Example",
        )

        assertEquals(html, prepared)
    }
}
