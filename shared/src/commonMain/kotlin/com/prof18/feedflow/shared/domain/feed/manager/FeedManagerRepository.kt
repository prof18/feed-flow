package com.prof18.feedflow.shared.domain.feed.manager

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryWithUnreadCount
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceWithUnreadCount
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.core.model.onError
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.feedsync.greader.GReaderRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.model.NotValidFeedSources
import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandler
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.model.ErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val accountsRepository: AccountsRepository,
    private val gReaderRepository: GReaderRepository,
) {

    private val errorMutableState: MutableSharedFlow<ErrorState> = MutableSharedFlow()
    val errorState = errorMutableState.asSharedFlow()

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
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.deleteFeedSource(feedSource.id)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }

            else -> {
                databaseHelper.deleteFeedSource(feedSource.id)
                feedSyncRepository.deleteFeedSource(feedSource)
                feedSyncRepository.performBackup()
            }
        }
    }

    fun observeCategories(): Flow<List<FeedSourceCategory>> =
        databaseHelper.observeFeedSourceCategories()

    fun observeCategoriesWithUnreadCount(): Flow<List<CategoryWithUnreadCount>> =
        databaseHelper.observeCategoriesWithUnreadCount()

    suspend fun createCategory(categoryName: CategoryName) {
        val categoryId = when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                "user/-/label/${categoryName.name}"
            }

            else -> categoryName.name.hashCode().toString()
        }

        val category = FeedSourceCategory(
            id = categoryId,
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

    fun observeFeedSourcesByCategoryWithUnreadCount(): Flow<Map<FeedSourceCategory?, List<FeedSourceWithUnreadCount>>> =
        databaseHelper.getFeedSourcesWithUnreadCountFlow()
            .map { feedSources ->
                val sourcesByCategory = feedSources.groupBy { it.feedSource.category }
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
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.deleteCategory(categoryId)
                    .fold(
                        onSuccess = {
                            gReaderRepository.fetchFeedSourcesAndCategories()
                                .onErrorSuspend {
                                    errorMutableState.emit(SyncError)
                                }
                        },
                        onFailure = {
                            errorMutableState.emit(SyncError)
                        },
                    )
            }

            else -> {
                databaseHelper.deleteCategory(categoryId)
                feedSyncRepository.deleteFeedSourceCategory(categoryId)
            }
        }
    }

    suspend fun updateFeedSourceName(feedSourceId: String, newName: String) =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.editFeedSourceName(feedSourceId, newName)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }
            else -> {
                databaseHelper.updateFeedSourceName(feedSourceId, newName)
                feedSyncRepository.updateFeedSourceName(feedSourceId, newName)
                feedSyncRepository.performBackup()
            }
        }

    suspend fun insertFeedSourcePreference(
        feedSourceId: String,
        preference: LinkOpeningPreference,
        isHidden: Boolean,
        isPinned: Boolean,
    ) = withContext(dispatcherProvider.io) {
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = feedSourceId,
            preference = preference,
            isHidden = isHidden,
            isPinned = isPinned,
        )
    }
}
