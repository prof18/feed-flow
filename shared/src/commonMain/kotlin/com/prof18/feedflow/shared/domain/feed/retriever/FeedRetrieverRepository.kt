package com.prof18.feedflow.shared.domain.feed.retriever

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
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
import com.prof18.feedflow.shared.domain.feed.FeedSourceLogoRetriever
import com.prof18.feedflow.shared.domain.feed.FeedUrlRetriever
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.domain.model.AddFeedResponse
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.StartedFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.model.ErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.utils.sanitizeUrl
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

internal class FeedRetrieverRepository(
    private val parser: RssParser,
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
    private val dateFormatter: DateFormatter,
    private val feedSourceLogoRetriever: FeedSourceLogoRetriever,
    private val rssChannelMapper: RssChannelMapper,
    private val feedUrlRetriever: FeedUrlRetriever,
    private val feedSyncRepository: FeedSyncRepository,
    private val gReaderRepository: GReaderRepository,
    private val accountsRepository: AccountsRepository,
    private val feedStateRepository: FeedStateRepository,
) {

    private val errorMutableState: MutableSharedFlow<ErrorState> = MutableSharedFlow()
    val errorState = errorMutableState.asSharedFlow()

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

    suspend fun markAsRead(itemsToUpdates: HashSet<FeedItemId>) {
        feedStateRepository.markAsRead(itemsToUpdates)
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

    suspend fun markAllCurrentFeedAsRead() {
        val currentFilter = feedStateRepository.getCurrentFeedFilter()
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
        feedStateRepository.getFeeds()
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
        if (newFeedSource != originalFeedSource) {
            databaseHelper.insertFeedSourcePreference(
                feedSourceId = newFeedSource.id,
                preference = newFeedSource.linkOpeningPreference,
                isHidden = newFeedSource.isHiddenFromTimeline,
                isPinned = newFeedSource.isPinned,
            )
            feedStateRepository.getFeeds()
        }
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS -> editFeedSourceForFreshRss(newFeedSource, originalFeedSource)
            else -> editFeedSourceForLocalAccount(newFeedSource, originalFeedSource)
        }
    }

    suspend fun deleteOldFeeds() {
        // One week
        val threshold = Clock.System.now().minus(7.days).toEpochMilliseconds()
        val oldFeedIds = databaseHelper.getOldFeedItem(threshold)
        databaseHelper.deleteOldFeedItems(threshold)
        feedSyncRepository.deleteFeedItems(oldFeedIds)
        feedStateRepository.getFeeds()
    }

    suspend fun updateBookmarkStatus(feedItemId: FeedItemId, isBookmarked: Boolean) {
        feedStateRepository.updateBookmarkStatus(feedItemId, isBookmarked)

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
        feedStateRepository.updateReadStatus(feedItemId, isRead)

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

    fun search(query: String): Flow<List<Search>> =
        databaseHelper.search(
            searchQuery = query,
        )

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

        feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)
        gReaderRepository.sync()
        feedStateRepository.emitUpdateStatus(FinishedFeedUpdateStatus)
        feedStateRepository.getFeeds()
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
            feedStateRepository.updateCurrentFilterName(newName)
        }

        return if (newUrl != previousUrl) {
            when (val response = fetchSingleFeed(newUrl, newFeedSource.category)) {
                is AddFeedResponse.FeedFound -> {
                    updateFeedSource(
                        newFeedSource.copy(
                            url = response.parsedFeedSource.url,
                            isHiddenFromTimeline = newFeedSource.isHiddenFromTimeline,
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
                feedStateRepository.updateCurrentFilterName(newFeedSource.title)
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
            isHiddenFromTimeline = false,
            linkOpeningPreference = LinkOpeningPreference.DEFAULT,
            isPinned = false,
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
        feedStateRepository.emitUpdateStatus(FinishedFeedUpdateStatus)
        feedStateRepository.getFeeds()
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
}
