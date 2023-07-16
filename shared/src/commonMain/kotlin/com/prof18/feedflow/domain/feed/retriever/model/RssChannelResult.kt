package com.prof18.feedflow.domain.feed.retriever.model

import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.rssparser.model.RssChannel

internal data class RssChannelResult(
    val rssChannel: RssChannel,
    val feedSource: FeedSource,
)
