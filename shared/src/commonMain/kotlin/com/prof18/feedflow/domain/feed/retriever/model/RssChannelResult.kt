package com.prof18.feedflow.domain.feed.retriever.model

import com.prof18.rssparser.model.RssChannel
import com.prof18.feedflow.domain.model.FeedSource

internal data class RssChannelResult(
    val rssChannel: RssChannel,
    val feedSource: FeedSource,
)
