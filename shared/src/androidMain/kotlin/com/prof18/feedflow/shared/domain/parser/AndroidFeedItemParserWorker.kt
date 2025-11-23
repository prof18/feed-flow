package com.prof18.feedflow.shared.domain.parser

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.CompletableDeferred

internal class AndroidFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val settingsRepository: SettingsRepository,
) : FeedItemParserWorker {

    override suspend fun enqueueParsing(feedItemId: String, url: String) {
        logger.d { "Enqueueing parsing for feedItemId: $feedItemId, url: $url" }

        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<FeedItemParserWorkManager>()
                .addTag(feedItemId)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(
                    workDataOf(
                        FeedItemParserWorkManager.FEED_ITEM_ID_INPUT_KEY to feedItemId,
                        FeedItemParserWorkManager.URL_INPUT_KEY to url,
                    ),
                )
                .build()

        WorkManager.getInstance(appContext).enqueue(uploadWorkRequest)
    }

    override suspend fun triggerImmediateParsing(feedItemId: String, url: String): ParsingResult {
        logger.d { "Triggering immediate parsing for: $url (feedItemId: $feedItemId)" }

        val deferredResult = CompletableDeferred<ParsingResult>()
        FeedItemParser(
            htmlRetriever = htmlRetriever,
            appContext = appContext,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
        ).parseFeedItem(url) { result ->
            deferredResult.complete(result)
        }
        val result = deferredResult.await()

        if (result is ParsingResult.Success) {
            val content = result.htmlContent
            if (content != null && settingsRepository.isSaveItemContentOnOpenEnabled()) {
                feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
                logger.d { "Successfully parsed and cached content for: $url (feedItemId: $feedItemId)" }
            }
        }

        return result
    }

    override suspend fun triggerBackgroundParsing(feedItemId: String, url: String): ParsingResult {
        logger.d { "Triggering background parsing for: $url (feedItemId: $feedItemId)" }
        // On Android, background parsing is the same as immediate parsing
        // WorkManager handles the background scheduling
        return triggerImmediateParsing(feedItemId, url)
    }
}
