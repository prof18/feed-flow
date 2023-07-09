package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.model.ParsedFeedSource

internal expect class OPMLFeedHandler {
    suspend fun importFeed(opmlInput: OPMLInput): List<ParsedFeedSource>
    suspend fun exportFeed(opmlOutput: OPMLOutput, feedSources: List<FeedSource>)
}