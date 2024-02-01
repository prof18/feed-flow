package com.prof18.feedflow.shared.domain.feed.manager

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.model.NotValidFeedSources
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.utils.DispatcherProvider
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
) {
    suspend fun addFeedsFromFile(opmlInput: OpmlInput): NotValidFeedSources = withContext(dispatcherProvider.io) {
        val feeds = opmlFeedHandler.generateFeedSources(opmlInput)
        val categories = feeds.mapNotNull { it.categoryName }.distinct()

        val validatedFeeds = validateFeeds(feeds)

        val validFeedSources = validatedFeeds
            .filter { it.isValid }
            .map { it.parsedFeedSource }

        val notValidFeedSources = validatedFeeds
            .filter { !it.isValid }
            .map { it.parsedFeedSource }

        databaseHelper.insertCategories(categories)
        databaseHelper.insertFeedSource(validFeedSources)

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
        databaseHelper.deleteFeedSource(feedSource)
    }

    fun observeCategories(): Flow<List<FeedSourceCategory>> =
        databaseHelper.observeFeedSourceCategories()

    suspend fun getCategories(): List<FeedSourceCategory> =
        databaseHelper.getFeedSourceCategories()

    suspend fun createCategory(categoryName: CategoryName) =
        databaseHelper.insertCategories(
            listOf(categoryName),
        )

    fun deleteAllFeeds() {
        databaseHelper.deleteAllFeeds()
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

    suspend fun deleteCategory(categoryId: Long) {
        databaseHelper.deleteCategory(categoryId)
    }
}
