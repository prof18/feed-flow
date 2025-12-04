package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.core.model.ParsingResult

interface FeedItemParserWorker {
    suspend fun enqueueParsing(feedItemId: String, url: String)
    suspend fun triggerImmediateParsing(feedItemId: String, url: String): ParsingResult

    // TODO: check how is used and maybe delete it
    // On Android/Desktop this mirrors triggerImmediateParsing; iOS uses a shared parser instance instead of a new one.
    suspend fun triggerBackgroundParsing(feedItemId: String, url: String): ParsingResult
}
