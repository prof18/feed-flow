package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.utils.ContentDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderModeMarkdownTest {

    @Test
    fun `reader markdown includes title image and body`() {
        val markdown = buildReaderModeMarkdown(
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
    fun `reader markdown does not duplicate an existing image`() {
        val content = "![](https://example.com/hero.jpg)\n\nArticle body"

        val markdown = buildReaderModeMarkdown(
            content = content,
            title = "Article title",
            imageUrl = "https://example.com/hero.jpg",
            siteName = null,
        )

        assertEquals("# Article title\n\n$content", markdown)
    }

    @Test
    fun `reader markdown does not inject hero when content starts with a different image URL`() {
        val content = "![Article image](https://cdn.example.com/hero.jpg?w=1200)\n\nArticle body"

        val markdown = buildReaderModeMarkdown(
            content = content,
            title = "Article title",
            imageUrl = "https://cdn.example.com/hero.jpg?w=640",
            siteName = null,
        )

        assertEquals("# Article title\n\n$content", markdown)
    }

    @Test
    fun `reader markdown still injects hero when the first content image is not leading`() {
        val content = "A".repeat(1000) + "\n\n![](https://example.com/body-image.jpg)"

        val markdown = buildReaderModeMarkdown(
            content = content,
            title = "Article title",
            imageUrl = "https://example.com/hero.jpg",
            siteName = null,
        )

        assertEquals(
            "# Article title\n\n![](https://example.com/hero.jpg)\n\n$content",
            markdown,
        )
    }

    @Test
    fun `reader direction follows a right-to-left title`() {
        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            readerModeContentDirection(title = "خبر فوری", siteName = null, markdown = "Body"),
        )
    }

    @Test
    fun `reader direction skips a neutral title and markup before the first word of the body`() {
        val markdown = "![hero](https://example.com/hero.jpg)\n\n" +
            "<p class=\"lead\">[https://example.com](https://example.com) متن مقاله</p>"

        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            readerModeContentDirection(title = "2026 — 12:45", siteName = null, markdown = markdown),
        )
    }

    @Test
    fun `reader direction stays left-to-right for a Latin article`() {
        assertEquals(
            ContentDirection.LEFT_TO_RIGHT,
            readerModeContentDirection(title = "Article title", siteName = null, markdown = "متن"),
        )
    }

    @Test
    fun `reader direction is unknown without any strong character`() {
        assertNull(readerModeContentDirection(title = null, siteName = null, markdown = "![](https://x.com/a.jpg) 42"))
    }

    @Test
    fun `reader direction skips escaped parentheses angle brackets and titles in Klead destinations`() {
        val markdown = "![hero](https://example.com/photo\\(1\\).jpg)\n\n" +
            "![](<https://example.com/my photo.jpg> \"Latin \\\"title\\\"\")\n\n" +
            "[](https://example.com/wiki/Iran_\\(country\\) \"Wiki\") متن مقاله"

        assertEquals(
            ContentDirection.RIGHT_TO_LEFT,
            readerModeContentDirection(title = null, siteName = null, markdown = markdown),
        )
    }

    @Test
    fun `reader direction skips a very long inline image destination without overflowing the stack`() {
        val markdown = "![](data:image/png;base64,${"A".repeat(LONG_TOKEN_LENGTH)})\n\nمتن مقاله"

        assertEquals(ContentDirection.RIGHT_TO_LEFT, directionOnUiSizedStack(markdown))
    }

    @Test
    fun `reader direction skips a very long angle-bracket destination without overflowing the stack`() {
        val markdown = "[](<https://example.com/${"a b".repeat(LONG_TOKEN_LENGTH / 3)}>) متن مقاله"

        assertEquals(ContentDirection.RIGHT_TO_LEFT, directionOnUiSizedStack(markdown))
    }

    @Test
    fun `reader direction skips a very long link title without overflowing the stack`() {
        val markdown = "[](https://example.com \"${"Latin ".repeat(LONG_TOKEN_LENGTH / 6)}\") متن مقاله"

        assertEquals(ContentDirection.RIGHT_TO_LEFT, directionOnUiSizedStack(markdown))
    }

    // The desktop reader resolves the direction during composition, on AWT-EventQueue-0, which has the JVM's
    // default 1 MB thread stack.
    private fun directionOnUiSizedStack(markdown: String): ContentDirection? {
        var result: Result<ContentDirection?>? = null
        val thread = Thread(null, {
            result = runCatching { readerModeContentDirection(title = null, siteName = null, markdown = markdown) }
        }, "reader-direction", UI_THREAD_STACK_BYTES)
        thread.start()
        thread.join()
        return checkNotNull(result).getOrThrow()
    }

    private companion object {
        const val LONG_TOKEN_LENGTH = 7_000
        const val UI_THREAD_STACK_BYTES = 1024L * 1024L
    }
}
