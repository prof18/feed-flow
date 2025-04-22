package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.onError
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.NotValidFeedSources
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

internal class FeedImportExportRepository(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedHandler: OpmlFeedHandler,
    private val dispatcherProvider: DispatcherProvider,
    private val feedSyncRepository: FeedSyncRepository,
    private val accountsRepository: AccountsRepository,
    private val gReaderRepository: GReaderRepository,
    private val logger: Logger,
    private val logoRetriever: FeedSourceLogoRetriever,
    private val rssParser: RssParser,
) {
    suspend fun addFeedsFromFile(
        opmlInput: OpmlInput,
    ): NotValidFeedSources = withContext(dispatcherProvider.io) {
        val feeds = opmlFeedHandler.generateFeedSources(opmlInput)
        val categories = feeds.mapNotNull { it.category }.distinct()

        val feedSourcesWithError = mutableListOf<ParsedFeedSource>()
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                for (feed in feeds) {
                    gReaderRepository.addFeedSource(
                        url = feed.url,
                        categoryName = feed.category,
                        isNotificationEnabled = false,
                    ).onError {
                        feedSourcesWithError.add(feed)
                    }
                }
                return@withContext NotValidFeedSources(
                    feedSources = emptyList(),
                    feedSourcesWithError = feedSourcesWithError,
                )
            }
            else -> {
                val validatedFeeds = validateFeeds(feeds)

                val validFeedSources: List<ParsedFeedSource> = validatedFeeds
                    .filter { it.isValid }
                    .map { it.parsedFeedSource }

                val notValidFeedSources = validatedFeeds
                    .filter { !it.isValid }
                    .map { it.parsedFeedSource }

                databaseHelper.insertCategories(categories)
                databaseHelper.insertFeedSource(validFeedSources)

                feedSyncRepository.addSourceAndCategories(validFeedSources.map { it.toFeedSource() }, categories)
                feedSyncRepository.performBackup()

                return@withContext NotValidFeedSources(
                    feedSources = notValidFeedSources,
                    feedSourcesWithError = emptyList(),
                )
            }
        }
    }

    suspend fun exportFeedsAsOpml(opmlOutput: OpmlOutput) {
        val feeds = databaseHelper.getFeedSources()
        val feedsByCategory = feeds.groupBy { it.category }
        opmlFeedHandler.exportFeed(opmlOutput, feedsByCategory)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun validateFeeds(
        feedSources: List<ParsedFeedSource>,
    ): List<FeedValidationResult> =
        feedSources
            .asFlow()
            .flatMapMerge(concurrency = getNumberOfConcurrentParsingRequests()) { feedSource ->
                suspend {
                    val rssChannel = checkIfValidRss(feedSource.url)
                    val isValidRss = rssChannel != null
                    val feedSourceLogoUrl = rssChannel?.let { logoRetriever.getFeedSourceLogoUrl(it) }

                    logger.d { "${feedSource.url} is valid? $isValidRss" }
                    FeedValidationResult(
                        parsedFeedSource = feedSource.copy(logoUrl = feedSourceLogoUrl),
                        isValid = isValidRss,
                    )
                }.asFlow()
            }
            .toList()

    private suspend fun checkIfValidRss(url: String): RssChannel? {
        return try {
            rssParser.getRssChannel(url)
        } catch (e: Throwable) {
            logger.d { "Wrong url input: $e" }
            null
        }
    }

    private data class FeedValidationResult(
        val parsedFeedSource: ParsedFeedSource,
        val isValid: Boolean,
    )
}
