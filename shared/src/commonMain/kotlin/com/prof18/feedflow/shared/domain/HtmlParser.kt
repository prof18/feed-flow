package com.prof18.feedflow.shared.domain

interface HtmlParser {
    fun getTextFromHTML(html: String): String?
    fun getFaviconUrl(html: String): String?
    fun getRssUrl(html: String): String?
}
