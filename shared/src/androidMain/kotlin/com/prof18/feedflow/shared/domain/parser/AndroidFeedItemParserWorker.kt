package com.prof18.feedflow.shared.domain.parser

import android.content.Context
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class AndroidFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val settingsRepository: SettingsRepository,
) : FeedItemParserWorker, ReaderModeParserWarmer {
    private val parserScope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private val foregroundParser = FeedItemParser(
        htmlRetriever = htmlRetriever,
        appContext = appContext,
        logger = logger,
        dispatcherProvider = dispatcherProvider,
    )

    override fun warmUp() {
        parserScope.launch {
            foregroundParser.warmUp()
        }
    }

    override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
        logger.d { "Triggering immediate parsing for: $url (feedItemId: $feedItemId)" }

        val result = foregroundParser.parseFeedItem(url)
        if (result is ParsingResult.Success) {
            val content = result.htmlContent
            if (content != null && settingsRepository.isSaveItemContentOnOpenEnabled()) {
                feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
                logger.d { "Successfully parsed and cached content for: $url (feedItemId: $feedItemId)" }
            }
        }

        return result
    }
}
