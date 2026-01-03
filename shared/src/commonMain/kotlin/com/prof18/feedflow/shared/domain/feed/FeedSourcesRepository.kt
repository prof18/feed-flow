package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedSourceWithUnreadCount
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.model.StartedFeedUpdateStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.fold
import com.prof18.feedflow.core.model.isError
import com.prof18.feedflow.core.model.isSuccess
import com.prof18.feedflow.core.model.onErrorSuspend
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.domain.toFeedSource
import com.prof18.feedflow.feedsync.feedbin.domain.FeedbinRepository
import com.prof18.feedflow.feedsync.greader.domain.GReaderRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.prof18.feedflow.shared.domain.mappers.RssChannelMapper
import com.prof18.feedflow.shared.domain.model.AddFeedResponse
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.utils.sanitizeUrl
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class FeedSourcesRepository(
    private val databaseHelper: DatabaseHelper,
    private val accountsRepository: AccountsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val gReaderRepository: GReaderRepository,
    private val feedbinRepository: FeedbinRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
    private val feedStateRepository: FeedStateRepository,
    private val feedUrlRetriever: FeedUrlRetriever,
    private val feedSourceLogoRetriever: FeedSourceLogoRetriever,
    private val parser: RssParser,
    private val dateFormatter: DateFormatter,
    private val rssChannelMapper: RssChannelMapper,
) {
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

    fun getFeedSources(): Flow<List<FeedSource>> =
        databaseHelper.getFeedSourcesFlow()

    suspend fun deleteFeed(feedSource: FeedSource) {
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                gReaderRepository.deleteFeedSource(feedSource.id)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.DeleteFeedSourceFailed))
                    }
            }

            SyncAccounts.FEEDBIN -> {
                feedbinRepository.deleteFeedSource(feedSource.id)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.DeleteFeedSourceFailed))
                    }
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                try {
                    databaseHelper.deleteFeedSource(feedSource.id)
                } catch (e: Exception) {
                    logger.e(e) { "Error while deleting feed source" }
                    feedStateRepository.emitErrorState(DeleteFeedSourceError())
                }
                feedSyncRepository.deleteFeedSource(feedSource)
                feedSyncRepository.performBackup()
            }
        }
    }

    suspend fun deleteAllFeeds() {
        databaseHelper.deleteAll()
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

    suspend fun updateFeedSourceName(feedSourceId: String, newName: String) =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                gReaderRepository.editFeedSourceName(feedSourceId, newName)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.EditFeedSourceNameFailed))
                    }
            }

            SyncAccounts.FEEDBIN -> {
                feedbinRepository.editFeedSourceName(feedSourceId, newName)
                    .onErrorSuspend {
                        feedStateRepository.emitErrorState(SyncError(FeedSyncError.EditFeedSourceNameFailed))
                    }
            }
            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
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
        isNotificationEnabled: Boolean,
    ) = withContext(dispatcherProvider.io) {
        databaseHelper.insertFeedSourcePreference(
            feedSourceId = feedSourceId,
            preference = preference,
            isHidden = isHidden,
            isPinned = isPinned,
            isNotificationEnabled = isNotificationEnabled,
        )
    }

    suspend fun addFeedSource(
        feedUrl: String,
        categoryName: FeedSourceCategory?,
        isNotificationEnabled: Boolean,
    ): FeedAddedState =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> addFeedSourceForFreshRss(
                sanitizeUrl(feedUrl),
                categoryName,
                isNotificationEnabled,
            )

            SyncAccounts.FEEDBIN -> addFeedSourceForFeedbin(
                sanitizeUrl(feedUrl),
                categoryName,
                isNotificationEnabled,
            )
            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> addFeedSourceForLocalAccount(
                sanitizeUrl(feedUrl),
                categoryName,
                isNotificationEnabled,
            )
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
                isNotificationEnabled = newFeedSource.isNotificationEnabled,
            )
            feedStateRepository.getFeeds()
        }
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX ->
                editFeedSourceForFreshRss(newFeedSource, originalFeedSource)
            SyncAccounts.FEEDBIN -> editFeedSourceForFeedbin(newFeedSource, originalFeedSource)
            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> editFeedSourceForLocalAccount(newFeedSource, originalFeedSource)
        }
    }

    private suspend fun addFeedSourceForLocalAccount(
        feedUrl: String,
        categoryName: FeedSourceCategory?,
        isNotificationEnabled: Boolean,
    ): FeedAddedState {
        return when (val feedResponse = fetchSingleFeed(feedUrl, categoryName)) {
            is AddFeedResponse.FeedFound -> {
                addFeedSource(feedResponse, isNotificationEnabled)

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
        isNotificationEnabled: Boolean,
    ): FeedAddedState {
        for (suffix in knownUrlSuffix) {
            val actualUrl = suffix.buildUrl(originalUrl).trim()
            logger.d { "Trying with actualUrl: $actualUrl" }

            val addResult = gReaderRepository.addFeedSource(
                url = actualUrl,
                categoryName = categoryName,
                isNotificationEnabled = isNotificationEnabled,
            )
            if (addResult.isSuccess()) {
                return FeedAddedState.FeedAdded()
            }
        }

        logger.d { "Trying to get: $originalUrl" }
        val url = feedUrlRetriever.getFeedUrl(originalUrl) ?: return FeedAddedState.Error.InvalidUrl
        logger.d { "Found url: $url" }

        val addResult = gReaderRepository.addFeedSource(
            url = url,
            categoryName = categoryName,
            isNotificationEnabled = isNotificationEnabled,
        )
        if (addResult.isError()) {
            return FeedAddedState.Error.GenericError
        }

        feedStateRepository.emitUpdateStatus(StartedFeedUpdateStatus)
        gReaderRepository.sync()
        feedStateRepository.emitUpdateStatus(FinishedFeedUpdateStatus)
        feedStateRepository.getFeeds()
        return FeedAddedState.FeedAdded()
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

    private suspend fun addFeedSourceForFeedbin(
        originalUrl: String,
        categoryName: FeedSourceCategory?,
        isNotificationEnabled: Boolean,
    ): FeedAddedState {
        for (suffix in knownUrlSuffix) {
            val actualUrl = suffix.buildUrl(originalUrl).trim()
            logger.d { "Trying with actualUrl: $actualUrl" }

            val addResult = feedbinRepository.addFeedSource(
                url = actualUrl,
                categoryName = categoryName,
                isNotificationEnabled = isNotificationEnabled,
            )
            if (addResult.isSuccess()) {
                return FeedAddedState.FeedAdded()
            }
        }

        logger.d { "Trying to get: $originalUrl" }
        val url = feedUrlRetriever.getFeedUrl(originalUrl) ?: return FeedAddedState.Error.InvalidUrl
        logger.d { "Found url: $url" }

        val addResult = feedbinRepository.addFeedSource(
            url = url,
            categoryName = categoryName,
            isNotificationEnabled = isNotificationEnabled,
        )
        if (addResult.isError()) {
            return FeedAddedState.Error.GenericError
        }

        return FeedAddedState.FeedAdded()
    }

    private suspend fun editFeedSourceForFeedbin(
        newFeedSource: FeedSource,
        originalFeedSource: FeedSource?,
    ): FeedEditedState {
        feedbinRepository.editFeedSource(
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

    private suspend fun addFeedSource(
        feedFound: AddFeedResponse.FeedFound,
        isNotificationEnabled: Boolean,
    ) = withContext(
        dispatcherProvider.io,
    ) {
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
            websiteUrl = rssChannel.link,
            isHiddenFromTimeline = false,
            linkOpeningPreference = LinkOpeningPreference.DEFAULT,
            isPinned = false,
            isNotificationEnabled = isNotificationEnabled,
            fetchFailed = false,
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
        databaseHelper.updateNotificationEnabledStatus(feedSource.id, isNotificationEnabled)
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
            } catch (e: Throwable) {
                // Do nothing
                logger.d(e) { "Failed to parse rssChannel: $actualUrl" }
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

    private fun String.buildUrl(originalUrl: String) =
        when {
            this.isEmpty() -> originalUrl
            originalUrl.endsWith("/") -> "$originalUrl$this"
            else -> "$originalUrl/$this"
        }

    suspend fun addFeedSourceWithoutFetching(
        feedUrl: String,
        feedTitle: String,
        category: FeedSourceCategory?,
        logoUrl: String?,
    ) = withContext(dispatcherProvider.io) {
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.FRESH_RSS, SyncAccounts.MINIFLUX, SyncAccounts.BAZQUX -> {
                gReaderRepository.addFeedSource(
                    url = feedUrl,
                    categoryName = category,
                    isNotificationEnabled = false,
                )
            }

            SyncAccounts.FEEDBIN -> {
                feedbinRepository.addFeedSource(
                    url = feedUrl,
                    categoryName = category,
                    isNotificationEnabled = false,
                )
            }

            SyncAccounts.LOCAL,
            SyncAccounts.DROPBOX,
            SyncAccounts.GOOGLE_DRIVE,
            SyncAccounts.ICLOUD,
            -> {
                val parsedFeedSource = ParsedFeedSource(
                    id = feedUrl.hashCode().toString(),
                    url = feedUrl,
                    title = feedTitle,
                    category = category,
                    logoUrl = logoUrl,
                )

                if (category != null) {
                    databaseHelper.insertCategories(listOf(category))
                }

                databaseHelper.insertFeedSource(listOf(parsedFeedSource))

                feedSyncRepository.addSourceAndCategories(
                    listOf(parsedFeedSource.toFeedSource()),
                    category?.let { listOf(it) } ?: emptyList(),
                )
                feedSyncRepository.performBackup()
            }
        }
    }

    private data class AddResult(
        val channel: RssChannel,
        val usedUrl: String,
    )
}
