package com.prof18.feedflow.domain.feed.retriever.model

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.rssparser.model.RssChannel

internal sealed interface RssParsingResult

internal data class RssParsingSuccess(
    val rssChannel: RssChannel,
    val feedSource: FeedSource,
) : RssParsingResult

internal data class RssParsingError(
    val feedSource: FeedSource,
    val throwable: Throwable,
) : RssParsingResult
