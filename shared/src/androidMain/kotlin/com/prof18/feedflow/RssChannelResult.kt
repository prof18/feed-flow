package com.prof18.feedflow

import com.prof.rssparser.Channel

data class RssChannelResult(
    val rssChannel: Channel,
    val feedSource: FeedSource,
)