package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.domain.model.ParsedFeedSource

internal expect class OpmlFeedHandler {
    suspend fun importFeed(opmlInput: OpmlInput): List<ParsedFeedSource>
    suspend fun exportFeed(opmlOutput: OpmlOutput, feedSources: List<FeedSource>)
}
