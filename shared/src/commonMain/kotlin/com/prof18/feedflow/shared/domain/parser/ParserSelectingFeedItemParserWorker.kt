package com.prof18.feedflow.shared.domain.parser

import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker

internal class ParserSelectingFeedItemParserWorker(
    private val settingsRepository: SettingsRepository,
    private val legacyParser: FeedItemParserWorker,
    private val kleadParser: FeedItemParserWorker,
) : FeedItemParserWorker {

    override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
        val parser = if (settingsRepository.isKleadParserEnabled()) {
            kleadParser
        } else {
            legacyParser
        }
        return parser.parse(feedItemId, url, imageUrl)
    }
}
