package com.prof18.feedflow.shared.domain.feeditem

/**
 * Prepares feed-provided HTML (`content:encoded` / `description`) for the reader view.
 *
 * The returned string is in the platform's reader format:
 * - Android / iOS render HTML in a WebView, so the feed HTML is returned as-is.
 * - Desktop renders Markdown, so the HTML is converted to Markdown.
 *
 * Title, site name and hero image are added by the reader when it renders the content.
 */
interface FeedContentPreparer {
    suspend fun prepare(html: String, baseUrl: String?): String
}
