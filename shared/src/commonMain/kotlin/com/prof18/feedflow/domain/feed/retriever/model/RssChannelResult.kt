package com.prof18.feedflow.domain.feed.retriever.model

import com.prof.rssparser.Channel
import com.prof18.feedflow.domain.model.FeedSource

internal data class RssChannelResult(
    val rssChannel: Channel,
    val feedSource: FeedSource,
)