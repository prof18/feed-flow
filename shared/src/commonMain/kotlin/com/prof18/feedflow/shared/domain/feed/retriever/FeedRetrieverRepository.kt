package com.prof18.feedflow.shared.domain.feed.retriever

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.core.model.isError
import com.prof18.feedflow.core.model.isSuccess
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.feedsync.greader.GReaderRepository
import com.prof18.feedflow.shared.data.SettingsHelper
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.feed.FeedUrlRetriever
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import com.prof18.feedflow.shared.domain.model.AddFeedResponse
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.domain.model.StartedFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.ErrorState
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.utils.executeWithRetry
import com.prof18.feedflow.shared.utils.getNumberOfConcurrentParsingRequests
import com.prof18.feedflow.shared.utils.sanitizeUrl
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val gReaderRepository: GReaderRepository,
    private val accountsRepository: AccountsRepository,
) {
    private val updateMutableState: MutableStateFlow<FeedUpdateStatus> = MutableStateFlow(
        FinishedFeedUpdateStatus,
    )
    val updateState = updateMutableState.asStateFlow()

    private val errorMutableState: MutableSharedFlow<ErrorState> = MutableSharedFlow()
    val errorState = errorMutableState.asSharedFlow()

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
            if (feeds.isNotEmpty()) {
                updateMutableState.update { FinishedFeedUpdateStatus }
            }
            mutableFeedState.update {
                feeds.map { it.toFeedItem(dateFormatter, removeTitleFromDesc) }.toImmutableList()
            }
        } catch (e: Throwable) {
            logger.e(e) { "Something wrong while getting data from Database" }
            errorMutableState.emit(DatabaseError)
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
            errorMutableState.emit(DatabaseError)
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
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateReadStatus(itemsToUpdates.toList(), isRead = true)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }

            else -> {
                databaseHelper.markAsRead(itemsToUpdates.toList())
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
    }

    @Suppress("MagicNumber")
    suspend fun fetchFeeds(
        forceRefresh: Boolean = false,
        isFirstLaunch: Boolean = false,
    ) {
        return withContext(dispatcherProvider.io) {
            updateMutableState.update { StartedFeedUpdateStatus }

            when {
                gReaderRepository.isAccountSet() -> fetchFeedsWithGReader()
                else -> fetchFeedsWithRssParser(forceRefresh, isFirstLaunch)
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun fetchFeedsWithGReader() {
        val feedSourceUrls = databaseHelper.getFeedSources()
        if (feedSourceUrls.isEmpty()) {
            updateMutableState.update {
                NoFeedSourcesStatus
            }
        } else {
            gReaderRepository.sync()
                .onErrorSuspend {
                    errorMutableState.emit(SyncError)
                }
        }
        // If the sync is skipped quickly, sometimes the loading spinner stays out
        delay(50)
        isFeedSyncDone = true
        updateRefreshCount()
        getFeeds()
    }

    @Suppress("MagicNumber")
    private suspend fun fetchFeedsWithRssParser(
        forceRefresh: Boolean = false,
        isFirstLaunch: Boolean = false,
    ) {
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
            // If the sync is skipped quickly, sometimes the loading spinner stays out
            delay(50)
            isFeedSyncDone = true
            updateRefreshCount()
            getFeeds()
        }
    }

    suspend fun markAllCurrentFeedAsRead() {
        val currentFilter = currentFeedFilterMutableState.value
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.markAllFeedAsRead(currentFilter)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }

            else -> {
                databaseHelper.markAllFeedAsRead(currentFilter)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
        getFeeds()
    }

    private suspend fun fetchSingleFeed(
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

    suspend fun addFeedSource(feedUrl: String, categoryName: FeedSourceCategory?): FeedAddedState =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> addFeedSourceForFreshRss(sanitizeUrl(feedUrl), categoryName)
            else -> addFeedSourceForLocalAccount(sanitizeUrl(feedUrl), categoryName)
        }

    suspend fun editFeedSource(
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
    ): FeedEditedState {
        if (newFeedSource.linkOpeningPreference != originalFeedSource?.linkOpeningPreference) {
            databaseHelper.insertFeedSourcePreference(newFeedSource.id, newFeedSource.linkOpeningPreference)
            getFeeds()
        }
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> editFeedSourceForFreshRss(newFeedSource, originalFeedSource)
            else -> editFeedSourceForLocalAccount(newFeedSource, originalFeedSource)
        }
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

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateBookmarkStatus(feedItemId, isBookmarked)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }

            else -> {
                databaseHelper.updateBookmarkStatus(feedItemId, isBookmarked)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
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

        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> {
                gReaderRepository.updateReadStatus(listOf(feedItemId), isRead)
                    .onErrorSuspend {
                        errorMutableState.emit(SyncError)
                    }
            }

            else -> {
                databaseHelper.updateReadStatus(feedItemId, isRead)
                feedSyncRepository.setIsSyncUploadRequired()
            }
        }
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
                        errorMutableState.emit(
                            FeedErrorState(
                                failingSourceName = feedSource.title,
                            ),
                        )
                        feedToUpdate.remove(feedSource.url)
                        updateRefreshCount()
                    }
                }.asFlow()
            }.collect()

    fun search(query: String): Flow<List<Search>> =
        databaseHelper.search(
            searchQuery = query,
        )

    private fun updateCurrentFilterName(newName: String) {
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

    private suspend fun addFeedSourceForLocalAccount(
        feedUrl: String,
        categoryName: FeedSourceCategory?,
    ): FeedAddedState {
        return when (val feedResponse = fetchSingleFeed(feedUrl, categoryName)) {
            is AddFeedResponse.FeedFound -> {
                addFeedSource(feedResponse)

                FeedAddedState.FeedAdded(
                    feedResponse.parsedFeedSource.title,
                )
            }

            AddFeedResponse.EmptyFeed -> {
                FeedAddedState.Error.InvalidTitleLink
            }

            AddFeedResponse.NotRssFeed -> {
                FeedAddedState.Error.InvalidUrl
            }
        }
    }

    private suspend fun addFeedSourceForFreshRss(
        originalUrl: String,
        categoryName: FeedSourceCategory?,
    ): FeedAddedState {
        for (suffix in knownUrlSuffix) {
            val actualUrl = suffix.buildUrl(originalUrl).trim()
            logger.d { "Trying with actualUrl: $actualUrl" }

            val addResult = gReaderRepository.addFeedSource(url = actualUrl, categoryName = categoryName)
            if (addResult.isSuccess()) {
                return FeedAddedState.FeedAdded()
            }
        }

        logger.d { "Trying to get: $originalUrl" }
        val url = feedUrlRetriever.getFeedUrl(originalUrl) ?: return FeedAddedState.Error.InvalidUrl
        logger.d { "Found url: $url" }

        val addResult = gReaderRepository.addFeedSource(url = url, categoryName = categoryName)
        if (addResult.isError()) {
            return FeedAddedState.Error.GenericError
        }

        updateMutableState.update { StartedFeedUpdateStatus }
        gReaderRepository.sync()
        updateMutableState.update { FinishedFeedUpdateStatus }
        getFeeds()
        return FeedAddedState.FeedAdded()
    }

    private suspend fun editFeedSourceForLocalAccount(
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
    ): FeedEditedState {
        val newUrl = sanitizeUrl(newFeedSource.url)
        val previousUrl = originalFeedSource?.url

        val newName = newFeedSource.title
        val previousName = originalFeedSource?.title
        if (newName != previousName) {
            updateCurrentFilterName(newName)
        }

        return if (newUrl != previousUrl) {
            when (val response = fetchSingleFeed(newUrl, newFeedSource.category)) {
                is AddFeedResponse.FeedFound -> {
                    updateFeedSource(
                        newFeedSource.copy(
                            url = response.parsedFeedSource.url,
                        ),
                    )
                    FeedEditedState.FeedEdited(newName)
                }

                AddFeedResponse.EmptyFeed -> {
                    FeedEditedState.Error.InvalidTitleLink
                }

                AddFeedResponse.NotRssFeed -> {
                    FeedEditedState.Error.InvalidUrl
                }
            }
        } else {
            updateFeedSource(newFeedSource)
            FeedEditedState.FeedEdited(newName)
        }
    }

    private suspend fun editFeedSourceForFreshRss(
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
    ): FeedEditedState {
        gReaderRepository.editFeedSource(
            newFeedSource = newFeedSource,
            originalFeedSource = originalFeedSource,
        ).fold(
            onFailure = {
                return FeedEditedState.Error.GenericError
            },
            onSuccess = {
                updateCurrentFilterName(newFeedSource.title)
                return FeedEditedState.FeedEdited(newFeedSource.title)
            },
        )
    }

    private suspend fun addFeedSource(feedFound: AddFeedResponse.FeedFound) = withContext(dispatcherProvider.io) {
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

    private suspend fun updateFeedSource(feedSource: FeedSource) {
        databaseHelper.updateFeedSource(feedSource)
        feedSyncRepository.updateFeedSource(feedSource)
        feedSyncRepository.performBackup()
    }

    private data class AddResult(
        val channel: RssChannel,
        val usedUrl: String,
    )

    private companion object {
        private const val PAGE_SIZE = 40L
    }
}
