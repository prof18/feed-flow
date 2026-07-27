package com.prof18.feedflow.shared.domain.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopFeedContentPreparerTest {

    @Test
    fun `feed markdown includes title image and body`() {
        val markdown = buildFeedReaderMarkdown(
            content = "Article body",
            title = "Article title",
            imageUrl = "https://example.com/hero.jpg",
            siteName = "Example Site",
        )

        assertEquals(
            """
            # Article title

            **Example Site**

            ![](https://example.com/hero.jpg)

            Article body
            """.trimIndent(),
            markdown,
        )
    }

    @Test
    fun `feed markdown does not duplicate an existing image`() {
        val content = "![](https://example.com/hero.jpg)\n\nArticle body"

        val markdown = buildFeedReaderMarkdown(
            content = content,
            title = "Article title",
            imageUrl = "https://example.com/hero.jpg",
        )

        assertEquals("# Article title\n\n$content", markdown)
    }

    @Test
    fun `feed markdown does not inject hero when content starts with a different image URL`() {
        val content = "![Article image](https://cdn.example.com/hero.jpg?w=1200)\n\nArticle body"

        val markdown = buildFeedReaderMarkdown(
            content = content,
            title = "Article title",
            imageUrl = "https://cdn.example.com/hero.jpg?w=640",
        )

        assertEquals("# Article title\n\n$content", markdown)
    }

    @Test
    fun `feed markdown still injects hero when the first content image is not leading`() {
        val content = "A".repeat(1000) + "\n\n![](https://example.com/body-image.jpg)"

        val markdown = buildFeedReaderMarkdown(
            content = content,
            title = "Article title",
            imageUrl = "https://example.com/hero.jpg",
        )

        assertEquals(
            "# Article title\n\n![](https://example.com/hero.jpg)\n\n$content",
            markdown,
        )
    }
}
