package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker

internal class SelectionAwareCachingFeedItemParserWorker(
    private val parser: FeedItemParserWorker,
    private val settingsRepository: SettingsRepository,
    private val parserSelectionCoordinator: ParserSelectionCoordinator,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) : FeedItemParserWorker {

    override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
        val selectionSnapshot = parserSelectionCoordinator.snapshot()
        val result = parser.parse(feedItemId, url, imageUrl)
        return parserSelectionCoordinator.withSelection(selectionSnapshot) {
            val content = (result as? ParsingResult.Success)?.htmlContent
            if (content != null && settingsRepository.isSaveItemContentOnOpenEnabled()) {
                feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
            }
            result
        } ?: ParsingResult.Error
    }
}
