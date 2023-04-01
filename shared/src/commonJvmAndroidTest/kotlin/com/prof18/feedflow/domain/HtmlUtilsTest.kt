package com.prof18.feedflow.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlUtilsTest {

    private val htmlParser: JvmHtmlParser()

    @Test
    fun `When a text has HTML tags then getTextFromHTML returns the text without HTML tags`() {
        val html = """
                <div class="feat-image"><img src="https://9to5mac.com/wp-content/uploads/sites/6/2022/06/get-macos-ventura.jpg?quality=82&#038;strip=all&#038;w=1280" /></div> <p>Apple on Monday released macOS Ventura 13.2.1 for Mac users. According to Apple, the update brings “important bug fixes,” but there are no details on what exactly today’s update fixes. The update comes three weeks after the release of macOS 13.2, which introduced support for Security Keys with Apple.</p> <p> <a href="https://9to5mac.com/2023/02/13/macos-ventura-13-2-1-update/#more-864283" class="more-link">more…</a></p> <p>The post <a rel="nofollow" href="https://9to5mac.com/2023/02/13/macos-ventura-13-2-1-update/">Apple releases macOS Ventura 13.2.1 with important bug fixes for Mac users</a> appeared first on <a rel="nofollow" href="https://9to5mac.com">9to5Mac</a>.</p>
            """.trimIndent()

        val text = htmlParser.getTextFromHTML(html)

        val expectedText = """
            Apple on Monday released macOS Ventura 13.2.1 for Mac users. According to Apple, the update brings “important bug fixes,” but there are no details on what exactly today’s update fixes. The update comes three weeks after the release of macOS 13.2, which introduced support for Security Keys with Apple. more… The post Apple releases macOS Ventura 13.2.1 with important bug fixes for Mac users appeared first on 9to5Mac.
        """.trimIndent()

        assertEquals(expectedText, text)
    }

    @Test
    fun `When text is not HTML then getTextFromHTML returns the text`() {
        val text = """
            Apple on Monday released macOS Ventura 13.2.1 for Mac users. According to Apple, the update brings “important bug fixes,” but there are no details on what exactly today’s update fixes. The update comes three weeks after the release of macOS 13.2, which introduced support for Security Keys with Apple. more… The post Apple releases macOS Ventura 13.2.1 with important bug fixes for Mac users appeared first on 9to5Mac.
        """.trimIndent()

        val cleanText = htmlParser.getTextFromHTML(text)

        assertEquals(text, cleanText)
    }
}
