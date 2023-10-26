package com.prof18.feedflow.domain

interface HtmlParser {
    fun getTextFromHTML(html: String): String?

    fun getFaviconUrl(html: String): String?
}
