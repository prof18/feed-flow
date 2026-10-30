package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.shared.domain.recovery.FeedChallenge

interface ArticleContentParser {
    /**
     * Returns the article body extracted from [url], or null when the page cannot be turned into
     * reader content. The reader adds title, site name and hero image when it renders it.
     */
    suspend fun parse(url: String): String?

    /** Like [parse], but tells a website verification challenge apart from other failures. */
    suspend fun parseResult(url: String): ArticleParseResult =
        parse(url)?.let { ArticleParseResult.Content(it, url) } ?: ArticleParseResult.Failed

    /** Extracts the article body from already downloaded [html]. */
    suspend fun parseHtml(html: String, url: String): String? = null

    /**
     * Returns feed-provided HTML (`content:encoded` / `description`) in the platform's reader format.
     */
    suspend fun prepareFeedContent(html: String, baseUrl: String?): String
}

sealed interface ArticleParseResult {
    data class Content(val content: String, val baseUrl: String) : ArticleParseResult
    data class Challenge(val challenge: FeedChallenge) : ArticleParseResult
    data object Failed : ArticleParseResult
}
