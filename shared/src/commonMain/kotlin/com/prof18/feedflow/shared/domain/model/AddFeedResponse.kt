package com.prof18.feedflow.shared.domain.model

import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.rssparser.model.RssChannel

internal sealed interface AddFeedResponse {
    data class FeedFound(
        val rssChannel: RssChannel,
        val parsedFeedSource: ParsedFeedSource,
    ) : AddFeedResponse

    data object EmptyFeed : AddFeedResponse

    data object NotRssFeed : AddFeedResponse
}
