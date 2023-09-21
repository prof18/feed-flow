package com.prof18.feedflow.domain.feed.retriever

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.db.SelectFeeds
import com.prof18.feedflow.domain.DateFormatter
import com.prof18.feedflow.domain.HtmlParser
import com.prof18.feedflow.domain.model.FeedItemId
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
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalObjCRefinement::class)
@HiddenFromObjC
internal class FeedRetrieverRepositoryImpl(
    private val parser: RssParser,
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val htmlParser: HtmlParser,
    private val logger: Logger,
    private val dateFormatter: DateFormatter,
) : FeedRetrieverRepository {

    private val updateMutableState: MutableStateFlow<FeedUpdateStatus> = MutableStateFlow(
        FinishedFeedUpdateStatus,
    )
    override val updateState = updateMutableState.asStateFlow()

    private val errorMutableState: MutableStateFlow<ErrorState?> = MutableStateFlow(null)
    override val errorState = errorMutableState.asStateFlow()

    private val feedToUpdate = hashSetOf<String>()

    private val mutableFeedState: MutableStateFlow<List<FeedItem>> = MutableStateFlow(emptyList())
    override val feedState = mutableFeedState.asStateFlow()

    override fun getFeeds() {
        try {
            val feeds = executeWithRetry {
                databaseHelper.getFeedItems()
            }
            mutableFeedState.update {
                feeds.map { it.toFeedItem() }
            }
        } catch (e: Throwable) {
            logger.e(e) { "Something wrong while getting data from Database" }
            errorMutableState.update {
                DatabaseError
            }
        }
    }

    override suspend fun updateReadStatus(
        lastUpdateIndex: Int,
        lastVisibleIndex: Int,
    ) {
        val urlToUpdates = mutableListOf<FeedItemId>()

        val items = feedState.value.toMutableList()
        if (lastVisibleIndex <= lastUpdateIndex) {
            return
        }
        for (index in lastUpdateIndex..lastVisibleIndex) {
            items.getOrNull(index)?.let { item ->
                if (!item.isRead) {
                    urlToUpdates.add(
                        FeedItemId(
                            id = item.id,
                        ),
                    )
                }
                items[index] = item.copy(isRead = true)
            }
        }
        mutableFeedState.update { items }
        databaseHelper.updateReadStatus(urlToUpdates)
    }

    override suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>) =
        databaseHelper.updateReadStatus(itemsToUpdates)

    override suspend fun fetchFeeds(
        updateLoadingInfo: Boolean,
        forceRefresh: Boolean,
        isFirstLaunch: Boolean,
    ) {
        return withContext(dispatcherProvider.io) {
            if (updateLoadingInfo) {
                updateMutableState.update { StartedFeedUpdateStatus }
            } else {
                updateMutableState.update { FinishedFeedUpdateStatus }
            }
            val feedSourceUrls = databaseHelper.getFeedSources()
            feedToUpdate.clear()
            feedToUpdate.addAll(feedSourceUrls.map { it.url })
            if (feedSourceUrls.isEmpty()) {
                updateMutableState.update {
                    NoFeedSourcesStatus
                }
            } else {
                if (updateLoadingInfo) {
                    updateMutableState.emit(
                        InProgressFeedUpdateStatus(
                            refreshedFeedCount = 0,
                            totalFeedCount = feedSourceUrls.size,
                        ),
                    )
                }
                databaseHelper.updateNewStatus()

                if (!isFirstLaunch) {
                    getFeeds()
                }

                parseFeeds(feedSourceUrls, updateLoadingInfo, forceRefresh)

                getFeeds()
            }
        }
    }

    override suspend fun markAllFeedAsRead() {
        databaseHelper.markAllFeedAsRead()
        getFeeds()
    }

    @Suppress("MagicNumber")
    override suspend fun deleteOldFeeds() {
        // One week
        // (((1 hour in seconds) * 24 hours) * 7 days)
        val oneWeekInMillis = (((60 * 60) * 24) * 7) * 1000L
        val threshold = dateFormatter.currentTimeMillis() - oneWeekInMillis
        databaseHelper.deleteOldFeedItems(threshold)
        getFeeds()
    }

    private suspend fun parseFeeds(
        feedSourceUrls: List<FeedSource>,
        updateLoadingInfo: Boolean,
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
                    if (updateLoadingInfo) {
                        updateRefreshCount()
                    }
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
                        if (updateLoadingInfo) {
                            updateRefreshCount()
                        }

                        val items = rssChannel.getFeedItems(
                            feedSource = feedSource,
                        )

                        databaseHelper.insertFeedItems(items, dateFormatter.currentTimeMillis())
                    } catch (e: Throwable) {
                        logger.e(e) { "Something went wrong, skipping: ${feedSource.url}}" }
                        errorMutableState.update {
                            FeedErrorState(
                                failingSourceName = feedSource.title,
                            )
                        }
                        feedToUpdate.remove(feedSource.url)
                        if (updateLoadingInfo) {
                            updateRefreshCount()
                        }
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
    private fun RssChannel.getFeedItems(feedSource: FeedSource): List<FeedItem> =
        this.items.mapNotNull { rssItem ->

            val title = rssItem.title
            val url = rssItem.link
            val pubDate = rssItem.pubDate

            val dateMillis = if (pubDate != null) {
                dateFormatter.getDateMillisFromString(pubDate)
            } else {
                null
            }

            val imageUrl = if (rssItem.image?.contains("http:") == true) {
                rssItem.image?.replace("http:", "https:")
            } else {
                rssItem.image
            }

            if (url == null) {
                logger.d { "Skipping: ${rssItem.title}" }
                null
            } else {
                FeedItem(
                    id = url.hashCode(),
                    url = url,
                    title = title,
                    subtitle = rssItem.description?.let { description ->
                        val partialDesc = if (description.isNotEmpty()) {
                            description.take(500)
                        } else {
                            description
                        }
                        htmlParser.getTextFromHTML(partialDesc)
                    },
                    content = null,
                    imageUrl = imageUrl,
                    feedSource = feedSource,
                    isRead = false,
                    pubDateMillis = dateMillis,
                    dateString = if (dateMillis != null) {
                        dateFormatter.formatDate(dateMillis)
                    } else {
                        null
                    },
                    commentsUrl = rssItem.commentsUrl,
                )
            }
        }

    private fun SelectFeeds.toFeedItem() = FeedItem(
        id = url_hash,
        url = url,
        title = title,
        subtitle = subtitle,
        content = null,
        imageUrl = image_url,
        feedSource = FeedSource(
            id = feed_source_id,
            url = feed_source_url,
            title = feed_source_title,
            lastSyncTimestamp = feed_source_last_sync_timestamp,
        ),
        isRead = is_read,
        pubDateMillis = pub_date,
        dateString = if (pub_date != null) {
            dateFormatter.formatDate(pub_date)
        } else {
            null
        },
        commentsUrl = comments_url,
    )
}
