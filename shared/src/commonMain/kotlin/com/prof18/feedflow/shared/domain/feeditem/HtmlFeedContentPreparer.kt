package com.prof18.feedflow.shared.domain.feeditem

internal class HtmlFeedContentPreparer : FeedContentPreparer {
    override suspend fun prepare(
        html: String,
        baseUrl: String?,
        title: String?,
        imageUrl: String?,
        siteName: String?,
    ): String = html
}
