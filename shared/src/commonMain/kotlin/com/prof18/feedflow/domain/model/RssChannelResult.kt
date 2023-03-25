package com.prof18.feedflow.domain.model

import com.prof.rssparser.Channel
import com.prof18.feedflow.domain.model.FeedSource

data class RssChannelResult(
    val rssChannel: Channel,
    val feedSource: FeedSource,
)