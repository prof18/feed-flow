package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.core.model.ParsingResult

interface FeedItemParserWorker {
    suspend fun enqueueParsing(feedItemId: String, url: String)
    suspend fun triggerImmediateParsing(feedItemId: String, url: String): ParsingResult
    suspend fun triggerBackgroundParsing(feedItemId: String, url: String): ParsingResult
}
