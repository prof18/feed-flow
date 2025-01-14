package com.prof18.feedflow.shared.domain.feed.retriever

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.shared.data.SettingsHelper
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.feed.FeedUrlRetriever
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import com.prof18.feedflow.shared.domain.model.AddFeedResponse
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.domain.model.StartedFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.ErrorState
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.utils.executeWithRetry
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
    private val feedUrlRetriever: FeedUrlRetriever,
    private val feedSyncRepository: FeedSyncRepository,
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
    private var isFeedSyncDone = true

    private val knownUrlSuffix = listOf(
        "",
        "feed",
        "rss",
        "atom.xml",
        "feed.xml",
        "rss.xml",
        "index.xml",
        "atom.json",
        "feed.json",
        "rss.json",
        "index.json",
    )

    suspend fun getFeeds() {
        try {
            val feeds = executeWithRetry {
                databaseHelper.getFeedItems(
                    feedFilter = currentFeedFilterMutableState.value,
                    pageSize = PAGE_SIZE,
                    offset = 0,
                    showReadItems = settingsHelper.getShowReadArticlesTimeline(),
                )
            }
            currentPage = 1
            val removeTitleFromDesc = settingsHelper.getRemoveTitleFromDescription()
            mutableFeedState.update {
                feeds.map { it.toFeedItem(dateFormatter, removeTitleFromDesc) }.toImmutableList()
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
                    showReadItems = settingsHelper.getShowReadArticlesTimeline(),
                )
            }
            currentPage += 1
            val removeTitleFromDesc = settingsHelper.getRemoveTitleFromDescription()
            mutableFeedState.update { currentItems ->
                val newList = feeds.map { it.toFeedItem(dateFormatter, removeTitleFromDesc) }.toImmutableList()
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
        getFeeds()
    }

    suspend fun markAsRead(itemsToUpdates: HashSet<FeedItemId>) {
        mutableFeedState.update { currentItems ->
            currentItems.map { feedItem ->
                if (FeedItemId(feedItem.id) in itemsToUpdates) {
                    feedItem.copy(isRead = true)
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
        databaseHelper.markAsRead(itemsToUpdates.toList())
        feedSyncRepository.setIsSyncUploadRequired()
    }

    @Suppress("MagicNumber")
    suspend fun fetchFeeds(
        forceRefresh: Boolean = false,
        isFirstLaunch: Boolean = false,
    ) {
        return withContext(dispatcherProvider.io) {
            updateMutableState.update { StartedFeedUpdateStatus }

            feedSyncRepository.syncFeedSources()

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

                if (!isFirstLaunch) {
                    getFeeds()
                }

                isFeedSyncDone = false
                parseFeeds(
                    feedSourceUrls = feedSourceUrls,
                    forceRefresh = forceRefresh,
                )

                feedSyncRepository.syncFeedItems()
                isFeedSyncDone = true
                // If the sync is skipped quickly, sometimes the loading spinner stays out
                delay(50)
                updateRefreshCount()

                getFeeds()
            }
        }
    }

    suspend fun markAllCurrentFeedAsRead() {
        val currentFilter = currentFeedFilterMutableState.value
        databaseHelper.markAllFeedAsRead(currentFilter)
        feedSyncRepository.setIsSyncUploadRequired()
        getFeeds()
    }

    suspend fun fetchSingleFeed(
        url: String,
        category: FeedSourceCategory?,
    ): AddFeedResponse = withContext(dispatcherProvider.io) {
        val addResult = guessLinkAndParseFeed(url)
            ?: return@withContext AddFeedResponse.NotRssFeed
        val rssChannel = addResult.channel
        val urlToSave = addResult.usedUrl

        logger.d { "<- Got back ${rssChannel.title}" }

        val title = rssChannel.title

        if (title != null) {
            val logoUrl = feedSourceLogoRetriever.getFeedSourceLogoUrl(rssChannel)

            val parsedFeedSource = ParsedFeedSource(
                id = urlToSave.hashCode().toString(),
                url = urlToSave,
                title = title,
                category = category,
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
            id = parsedFeedSource.url.hashCode().toString(),
            url = parsedFeedSource.url,
            title = parsedFeedSource.title,
            lastSyncTimestamp = currentTimestamp,
            category = parsedFeedSource.category,
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
        feedSyncRepository.insertSyncedFeedSource(listOf(parsedFeedSource.toFeedSource()))
        feedSyncRepository.performBackup()
        updateMutableState.update { FinishedFeedUpdateStatus }
        getFeeds()
    }

    @Suppress("MagicNumber")
    suspend fun deleteOldFeeds() {
        // One week
        // (((1 hour in seconds) * 24 hours) * 7 days)
        val oneWeekInMillis = (((60 * 60) * 24) * 7) * 1000L
        val threshold = dateFormatter.currentTimeMillis() - oneWeekInMillis
        val oldFeedIds = databaseHelper.getOldFeedItem(threshold)
        databaseHelper.deleteOldFeedItems(threshold)
        feedSyncRepository.deleteFeedItems(oldFeedIds)
        getFeeds()
    }

    suspend fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        mutableFeedState.update { currentItems ->
            currentItems.mapNotNull { feedItem ->
                if (feedItem.id == feedItemId.id) {
                    if (currentFeedFilter.value == FeedFilter.Bookmarks && !isBookmarked) {
                        null
                    } else {
                        feedItem.copy(isBookmarked = isBookmarked)
                    }
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
        databaseHelper.updateBookmarkStatus(feedItemId, isBookmarked)
        feedSyncRepository.setIsSyncUploadRequired()
    }

    suspend fun updateReadStatus(feedItemId: FeedItemId, isRead: Boolean) {
        mutableFeedState.update { currentItems ->
            currentItems.mapNotNull { feedItem ->
                if (feedItem.id == feedItemId.id) {
                    if (currentFeedFilter.value == FeedFilter.Read && !isRead) {
                        null
                    } else {
                        feedItem.copy(isRead = isRead)
                    }
                } else {
                    feedItem
                }
            }.toImmutableList()
        }
        databaseHelper.updateReadStatus(feedItemId, isRead)
        feedSyncRepository.setIsSyncUploadRequired()
    }

    private suspend fun parseFeeds(
        feedSourceUrls: List<FeedSource>,
        forceRefresh: Boolean,
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

                        databaseHelper.insertFeedItems(items, dateFormatter.currentTimeMillis())
                    } catch (e: Throwable) {
                        logger.e { "Error, skip: ${feedSource.url}}. Error: $e" }
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

    fun search(query: String): Flow<List<Search>> =
        databaseHelper.search(
            searchQuery = query,
        )

    fun updateCurrentFilterName(newName: String) {
        val currentFilter = currentFeedFilter.value
        if (currentFilter is FeedFilter.Source) {
            currentFeedFilterMutableState.update {
                FeedFilter.Source(
                    feedSource = currentFilter.feedSource.copy(
                        title = newName,
                    ),
                )
            }
        }
    }

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

            if (feedToUpdate.isEmpty() && isFeedSyncDone) {
                FinishedFeedUpdateStatus
            } else {
                InProgressFeedUpdateStatus(
                    refreshedFeedCount = refreshedFeedCount,
                    totalFeedCount = totalFeedCount,
                )
            }
        }
    }

    private fun String.buildUrl(originalUrl: String) =
        when {
            this.isEmpty() -> originalUrl
            originalUrl.endsWith("/") -> "$originalUrl$this"
            else -> "$originalUrl/$this"
        }

    private suspend fun guessLinkAndParseFeed(originalUrl: String): AddResult? {
        for (suffix in knownUrlSuffix) {
            val actualUrl = suffix.buildUrl(originalUrl).trim()
            logger.d { "Trying with actualUrl: $actualUrl" }
            try {
                val channel = parser.getRssChannel(actualUrl)
                return AddResult(
                    channel = channel,
                    usedUrl = actualUrl,
                )
            } catch (_: Throwable) {
                // Do nothing
            }
        }

        logger.d { "Trying to get: $originalUrl" }
        val url = feedUrlRetriever.getFeedUrl(originalUrl) ?: return null
        logger.d { "Found url: $url" }
        return try {
            val channel = parser.getRssChannel(url)
            AddResult(
                channel = channel,
                usedUrl = url,
            )
        } catch (_: Throwable) {
            // Do nothing
            null
        }
    }

    private data class AddResult(
        val channel: RssChannel,
        val usedUrl: String,
    )

    private companion object {
        private const val PAGE_SIZE = 40L
    }
}
