package com.prof18.feedflow.shared.domain.feed.manager

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.NotValidFeedSources
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

internal class FeedManagerRepository(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedHandler: OpmlFeedHandler,
    private val rssParser: RssParser,
    private val logger: Logger,
    private val logoRetriever: FeedSourceLogoRetriever,
    private val dispatcherProvider: DispatcherProvider,
    private val feedSyncRepository: FeedSyncRepository,
) {
    suspend fun addFeedsFromFile(opmlInput: OpmlInput): NotValidFeedSources = withContext(dispatcherProvider.io) {
        val feeds = opmlFeedHandler.generateFeedSources(opmlInput)
        val categories = feeds.mapNotNull { it.category }.distinct()

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
        )
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

    private data class FeedValidationResult(
        val parsedFeedSource: ParsedFeedSource,
        val isValid: Boolean,
    )

    fun getFeedSources(): Flow<List<FeedSource>> =
        databaseHelper.getFeedSourcesFlow()

    suspend fun exportFeedsAsOpml(opmlOutput: OpmlOutput) {
        val feeds = databaseHelper.getFeedSources()
        val feedsByCategory = feeds.groupBy { it.category }
        opmlFeedHandler.exportFeed(opmlOutput, feedsByCategory)
    }

    suspend fun deleteFeed(feedSource: FeedSource) {
        databaseHelper.deleteFeedSource(feedSource.id)
        feedSyncRepository.deleteFeedSource(feedSource)
        feedSyncRepository.performBackup()
    }

    fun observeCategories(): Flow<List<FeedSourceCategory>> =
        databaseHelper.observeFeedSourceCategories()

    suspend fun getCategories(): List<FeedSourceCategory> =
        databaseHelper.getFeedSourceCategories()

    suspend fun createCategory(categoryName: CategoryName) {
        val category = FeedSourceCategory(
            id = categoryName.name.hashCode().toString(),
            title = categoryName.name,
        )
        databaseHelper.insertCategories(
            listOf(category),
        )

        feedSyncRepository.insertFeedSourceCategories(listOf(category))
    }

    fun deleteAllFeeds() {
        databaseHelper.deleteAllFeeds()
        feedSyncRepository.deleteAllFeedSources()
    }

    fun observeFeedSourcesByCategory(): Flow<Map<FeedSourceCategory?, List<FeedSource>>> =
        databaseHelper.getFeedSourcesFlow()
            .map { feedSources ->
                val sourcesByCategory = feedSources.groupBy { it.category }
                val sortedKeys = sourcesByCategory.keys.sortedBy { it?.title }
                sortedKeys.associateWith {
                    sourcesByCategory[it] ?: emptyList()
                }
            }

    private suspend fun checkIfValidRss(url: String): RssChannel? {
        return try {
            rssParser.getRssChannel(url)
        } catch (e: Throwable) {
            logger.d { "Wrong url input: $e" }
            null
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        databaseHelper.deleteCategory(categoryId)
        feedSyncRepository.deleteFeedSourceCategory(categoryId)
    }

    suspend fun updateFeedSourceName(feedSourceId: String, newName: String) {
        databaseHelper.updateFeedSourceName(feedSourceId, newName)
        feedSyncRepository.updateFeedSourceName(feedSourceId, newName)
        feedSyncRepository.performBackup()
    }

    suspend fun updateFeedSource(feedSource: FeedSource) {
        databaseHelper.updateFeedSource(feedSource)
        feedSyncRepository.updateFeedSource(feedSource)
        feedSyncRepository.performBackup()
    }
}
