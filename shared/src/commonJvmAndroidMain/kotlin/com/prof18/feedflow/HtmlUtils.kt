package com.prof18.feedflow

import org.jsoup.Jsoup

fun getTextFromHTML(html: String): String {
    val doc = Jsoup.parse(html)
    return doc.text()
}