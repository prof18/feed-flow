package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker

internal class ParserSelectingFeedItemParserWorker(
    private val legacyParser: FeedItemParserWorker,
    private val kleadParser: FeedItemParserWorker,
    private val parserSelectionCoordinator: ParserSelectionCoordinator,
) : FeedItemParserWorker {

    override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
        val selectionSnapshot = parserSelectionCoordinator.snapshot()
        val parser = if (selectionSnapshot.useKleadParser) {
            kleadParser
        } else {
            legacyParser
        }
        val result = parser.parse(feedItemId, url, imageUrl)
        return parserSelectionCoordinator.withSelection(selectionSnapshot) { result } ?: ParsingResult.Error
    }
}
