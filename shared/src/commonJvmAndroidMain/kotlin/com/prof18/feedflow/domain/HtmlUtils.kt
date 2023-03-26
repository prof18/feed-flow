package com.prof18.feedflow.domain

import org.jsoup.Jsoup

internal actual fun getTextFromHTML(html: String): String {
    val doc = Jsoup.parse(html)
    return doc.text()
}