package com.prof18.feedflow.core.domain

interface HtmlParser {
    fun getTextFromHTML(html: String): String?
    fun getFaviconUrl(html: String): String?
    fun getRssUrl(html: String): String?
    fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent
}

data class ParsedFeedContent(
    val text: String?,
    val commentsUrl: String?,
)
