package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource

internal interface OpmlFeedHandler {
    suspend fun generateFeedSources(opmlInput: OpmlInput): List<ParsedFeedSource>
    suspend fun exportFeed(
        opmlOutput: OpmlOutput,
        feedSourcesByCategory: Map<FeedSourceCategory?, List<FeedSource>>,
    )
}
