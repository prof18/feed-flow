package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger
import org.jsoup.Jsoup

internal class JvmHtmlParser(
    private val logger: Logger,
) : HtmlParser {
    override fun getTextFromHTML(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            doc.text()
        } catch (e: Throwable) {
            logger.e(e) { "Unable to get text from HTML, skipping" }
            null
        }
    }
}
