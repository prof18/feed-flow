package com.prof18.feedflow.shared.domain.feeditem

/**
 * Prepares feed-provided HTML (`content:encoded` / `description`) for the reader view.
 *
 * The returned string is platform-specific:
 * - Android / iOS render HTML in a WebView, so the feed HTML is returned as-is. The reader
 *   shell adds the title and hero image when it renders the document.
 * - Desktop renders Markdown, so the HTML is converted to Markdown and decorated with the
 *   same available article metadata as parsed web content.
 */
interface FeedContentPreparer {
    suspend fun prepare(
        html: String,
        baseUrl: String?,
        title: String?,
        imageUrl: String?,
        siteName: String?,
    ): String
}
