package com.prof18.feedflow.domain

import org.jsoup.Jsoup

internal class JvmHtmlParser : HtmlParser {
    override fun getTextFromHTML(html: String): String? {
        val doc = Jsoup.parse(html)
        return doc.text()
    }
}
