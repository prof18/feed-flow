package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.core.model.ParsingResult

interface FeedItemParserWorker {
    suspend fun parse(feedItemId: String, url: String): ParsingResult
}
