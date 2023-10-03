package com.prof18.feedflow.domain.feed.manager

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.model.NotValidFeedSources
import com.prof18.feedflow.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.feedflow.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.toList

internal class FeedManagerRepository(
    private val databaseHelper: DatabaseHelper,
    private val opmlFeedHandler: OpmlFeedHandler,
    private val rssParser: RssParser,
    private val logger: Logger,
) {
    suspend fun addFeedsFromFile(opmlInput: OpmlInput): NotValidFeedSources {
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

        return NotValidFeedSources(
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
                    val isValidRss = checkIfValidRss(feedSource.url)
                    logger.d { "${feedSource.url} is valid? $isValidRss" }
                    FeedValidationResult(
                        parsedFeedSource = feedSource,
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
        databaseHelper.getFeedSourcesFlowWithNoTimestamp()

    suspend fun exportFeedsAsOpml(opmlOutput: OpmlOutput) {
        val feeds = databaseHelper.getFeedSources()
        opmlFeedHandler.exportFeed(opmlOutput, feeds)
    }

    suspend fun deleteFeed(feedSource: FeedSource) {
        databaseHelper.deleteFeedSource(feedSource)
    }

    suspend fun getCategories(): List<FeedSourceCategory> =
        databaseHelper.getFeedSourceCategories()

    suspend fun createCategory(categoryName: CategoryName) =
        databaseHelper.insertCategories(
            listOf(categoryName),
        )

    private suspend fun checkIfValidRss(url: String): Boolean {
        return try {
            rssParser.getRssChannel(url)
            true
        } catch (e: Throwable) {
            logger.d { "Wrong url input: $e" }
            false
        }
    }

    fun deleteAllFeeds() {
        databaseHelper.deleteAllFeeds()
    }
}
