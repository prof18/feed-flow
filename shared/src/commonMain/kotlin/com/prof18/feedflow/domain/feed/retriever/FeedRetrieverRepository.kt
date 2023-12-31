package com.prof18.feedflow.domain.feed.retriever

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.data.SettingsHelper
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.domain.DateFormatter
import com.prof18.feedflow.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.domain.mappers.RssChannelMapper
import com.prof18.feedflow.domain.mappers.toFeedItem
import com.prof18.feedflow.domain.model.AddFeedResponse
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.domain.model.StartedFeedUpdateStatus
import com.prof18.feedflow.presentation.model.DatabaseError
import com.prof18.feedflow.presentation.model.ErrorState
import com.prof18.feedflow.presentation.model.FeedErrorState
import com.prof18.feedflow.utils.DispatcherProvider
import com.prof18.feedflow.utils.executeWithRetry
import com.prof18.feedflow.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions", "LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedRetrieverRepository(
    private val parser: RssParser,
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
    private val dateFormatter: DateFormatter,
    private val settingsHelper: SettingsHelper,
    private val feedSourceLogoRetriever: FeedSourceLogoRetriever,
    private val rssChannelMapper: RssChannelMapper,
) {
    private val updateMutableState: MutableStateFlow<FeedUpdateStatus> = MutableStateFlow(
        FinishedFeedUpdateStatus,
    )
    val updateState = updateMutableState.asStateFlow()

    private val errorMutableState: MutableStateFlow<ErrorState?> = MutableStateFlow(null)
    val errorState = errorMutableState.asStateFlow()

    private val feedToUpdate = hashSetOf<String>()

    private val mutableFeedState: MutableStateFlow<ImmutableList<FeedItem>> = MutableStateFlow(persistentListOf())
    val feedState = mutableFeedState.asStateFlow()

    private val currentFeedFilterMutableState: MutableStateFlow<FeedFilter> = MutableStateFlow(FeedFilter.Timeline)
    val currentFeedFilter: StateFlow<FeedFilter> = currentFeedFilterMutableState.asStateFlow()

    private var currentPage: Int = 0

    suspend fun getFeeds() {
        try {
            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = currentFeedFilterMutableState.value,
                    pageSize = PAGE_SIZE,
                    offset = 0,
                )
            }
            currentPage = 1
            mutableFeedState.update {
                feeds.map { it.toFeedItem(dateFormatter) }.toImmutableList()
            }
        } catch (e: Throwable) {
            logger.e(e) { "Something wrong while getting data from Database" }
            errorMutableState.update {
                DatabaseError
            }
        }
    }

    suspend fun loadMoreFeeds() {
        // Stop loading if there are no more items
        if (mutableFeedState.value.size % PAGE_SIZE != 0L) {
            return
        }
        try {
            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = currentFeedFilterMutableState.value,
                    pageSize = PAGE_SIZE,
                    offset = currentPage * PAGE_SIZE,
                )
            }
            currentPage += 1
            mutableFeedState.update { currentItems ->
                val newList = feeds.map { it.toFeedItem(dateFormatter) }.toImmutableList()
                (currentItems + newList).toImmutableList()
            }
        } catch (e: Throwable) {
            logger.e(e) { "Something wrong while getting data from Database" }
            errorMutableState.update {
                DatabaseError
            }
        }
    }

    suspend fun updateFeedFilter(feedFilter: FeedFilter) {
        currentFeedFilterMutableState.update {
            feedFilter
        }
        getFeeds()
    }

    fun getUnreadFeedCountFlow(): Flow<Long> =
        currentFeedFilter.flatMapLatest { feedFilter ->
            databaseHelper.getUnreadFeedCountFlow(
                feedFilter = feedFilter,
            )
        }

    suspend fun clearReadFeeds() {
        databaseHelper.updateNewStatus()
        getFeeds()
    }

    suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>) {
        databaseHelper.updateReadStatus(itemsToUpdates.toList())
    }

    suspend fun fetchFeeds(
        forceRefresh: Boolean = false,
        isFirstLaunch: Boolean = false,
    ) {
        return withContext(dispatcherProvider.io) {
            updateMutableState.update { StartedFeedUpdateStatus }

            val feedSourceUrls = databaseHelper.getFeedSources()
            feedToUpdate.clear()
            feedToUpdate.addAll(feedSourceUrls.map { it.url })
            if (feedSourceUrls.isEmpty()) {
                updateMutableState.update {
                    NoFeedSourcesStatus
                }
            } else {
                updateMutableState.emit(
                    InProgressFeedUpdateStatus(
                        refreshedFeedCount = 0,
                        totalFeedCount = feedSourceUrls.size,
                    ),
                )
                databaseHelper.updateNewStatus()

                if (!isFirstLaunch) {
                    getFeeds()
                }

                parseFeeds(
                    feedSourceUrls = feedSourceUrls,
                    forceRefresh = forceRefresh,
                    isFeedSourceMigrationRequired = isFirstLaunch && isSourceImageMigrationRequired(),
                )

                if (isFirstLaunch && isSourceImageMigrationRequired()) {
                    settingsHelper.setFeedSourceImageMigrationDone()
                }

                getFeeds()
            }
        }
    }

    suspend fun markAllFeedAsRead() {
        val currentFilter = currentFeedFilterMutableState.value
        databaseHelper.markAllFeedAsRead(currentFilter)
        getFeeds()
    }

    suspend fun fetchSingleFeed(
        url: String,
        category: FeedSourceCategory?,
    ): AddFeedResponse = withContext(dispatcherProvider.io) {
        val rssChannel = try {
            parser.getRssChannel(url)
        } catch (e: Throwable) {
            logger.d { "Wrong url input: $e" }
            return@withContext AddFeedResponse.NotRssFeed
        }
        logger.d { "<- Got back ${rssChannel.title}" }

        val title = rssChannel.title

        if (title != null) {
            val logoUrl = feedSourceLogoRetriever.getFeedSourceLogoUrl(rssChannel)

            val parsedFeedSource = ParsedFeedSource(
                url = url,
                title = title,
                categoryName = category?.title?.let {
                    CategoryName(it)
                },
                logoUrl = logoUrl,
            )

            return@withContext AddFeedResponse.FeedFound(
                rssChannel = rssChannel,
                parsedFeedSource = parsedFeedSource,
            )
        } else {
            return@withContext AddFeedResponse.EmptyFeed
        }
    }

    suspend fun addFeedSource(feedFound: AddFeedResponse.FeedFound) = withContext(dispatcherProvider.io) {
        val rssChannel = feedFound.rssChannel
        val parsedFeedSource = feedFound.parsedFeedSource
        val currentTimestamp = dateFormatter.currentTimeMillis()
        val feedSource = FeedSource(
            id = parsedFeedSource.hashCode(),
            url = parsedFeedSource.url,
            title = parsedFeedSource.title,
            lastSyncTimestamp = currentTimestamp,
            category = parsedFeedSource.categoryName?.let { categoryName ->
                FeedSourceCategory(
                    id = 2,
                    title = categoryName.name,
                )
            },
            logoUrl = parsedFeedSource.logoUrl,
        )

        val feedItems = rssChannelMapper.getFeedItems(
            rssChannel = rssChannel,
            feedSource = feedSource,
        )

        databaseHelper.insertFeedSource(
            listOf(
                parsedFeedSource,
            ),
        )
        databaseHelper.insertFeedItems(feedItems, currentTimestamp)
        updateMutableState.update { FinishedFeedUpdateStatus }
        getFeeds()
    }

    @Suppress("MagicNumber")
    suspend fun deleteOldFeeds() {
        // One week
        // (((1 hour in seconds) * 24 hours) * 7 days)
        val oneWeekInMillis = (((60 * 60) * 24) * 7) * 1000L
        val threshold = dateFormatter.currentTimeMillis() - oneWeekInMillis
        databaseHelper.deleteOldFeedItems(threshold)
        getFeeds()
    }

    private suspend fun parseFeeds(
        feedSourceUrls: List<FeedSource>,
        forceRefresh: Boolean,
        isFeedSourceMigrationRequired: Boolean,
    ) =
        feedSourceUrls
            .mapNotNull { feedSource ->
                val shouldRefresh = shouldRefreshFeed(feedSource, forceRefresh)
                if (shouldRefresh) {
                    feedSource
                } else {
                    logger.d { "One hour is not passed, skipping: ${feedSource.url}}" }
                    feedToUpdate.remove(feedSource.url)
                    updateRefreshCount()
                    null
                }
            }
            .asFlow()
            .flatMapMerge(concurrency = getNumberOfConcurrentParsingRequests()) { feedSource ->
                suspend {
                    logger.d { "-> Getting ${feedSource.url}" }
                    try {
                        val rssChannel = parser.getRssChannel(feedSource.url)
                        logger.d { "<- Got back ${rssChannel.title}" }
                        feedToUpdate.remove(feedSource.url)
                        updateRefreshCount()

                        val items = rssChannelMapper.getFeedItems(
                            rssChannel = rssChannel,
                            feedSource = feedSource,
                        )

                        if (isFeedSourceMigrationRequired) {
                            val logoUrl = feedSourceLogoRetriever.getFeedSourceLogoUrl(rssChannel)

                            logger.d { "Setting source logo url: $logoUrl" }

                            databaseHelper.updateFeedSourceLogo(
                                feedSource = feedSource.copy(logoUrl = logoUrl),
                            )
                        }

                        databaseHelper.insertFeedItems(items, dateFormatter.currentTimeMillis())
                    } catch (e: Throwable) {
                        logger.e(e) { "Something went wrong, skipping: ${feedSource.url}}" }
                        errorMutableState.update {
                            FeedErrorState(
                                failingSourceName = feedSource.title,
                            )
                        }
                        feedToUpdate.remove(feedSource.url)
                        updateRefreshCount()
                    }
                }.asFlow()
            }.collect()

    @Suppress("MagicNumber")
    private fun shouldRefreshFeed(
        feedSource: FeedSource,
        forceRefresh: Boolean,
    ): Boolean {
        val lastSyncTimestamp = feedSource.lastSyncTimestamp
        val oneHourInMillis = (60 * 60) * 1000
        val currentTime = dateFormatter.currentTimeMillis()
        return forceRefresh ||
            lastSyncTimestamp == null ||
            currentTime - lastSyncTimestamp >= oneHourInMillis
    }

    private fun updateRefreshCount() {
        updateMutableState.update { oldUpdate ->
            val refreshedFeedCount = oldUpdate.refreshedFeedCount + 1
            val totalFeedCount = oldUpdate.totalFeedCount

            if (feedToUpdate.isEmpty()) {
                FinishedFeedUpdateStatus
            } else {
                InProgressFeedUpdateStatus(
                    refreshedFeedCount = refreshedFeedCount,
                    totalFeedCount = totalFeedCount,
                )
            }
        }
    }

    @Suppress("MagicNumber")
    private fun isSourceImageMigrationRequired(): Boolean {
        val databaseVersion = databaseHelper.getDatabaseVersion()
        val isMigrationDone = settingsHelper.isFeedSourceImageMigrationDone()
        return !isMigrationDone && databaseVersion >= 4.0
    }

    private companion object {
        private const val PAGE_SIZE = 40L
    }
}
