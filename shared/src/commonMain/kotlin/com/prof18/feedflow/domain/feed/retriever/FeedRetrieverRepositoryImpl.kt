package com.prof18.feedflow.domain.feed.retriever

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.DateFormatter
import com.prof18.feedflow.domain.HtmlParser
import com.prof18.feedflow.domain.feed.retriever.model.RssParsingError
import com.prof18.feedflow.domain.feed.retriever.model.RssParsingResult
import com.prof18.feedflow.domain.feed.retriever.model.RssParsingSuccess
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
import com.prof18.feedflow.utils.getNumberOfConcurrentParsingRequests
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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

    override fun getFeeds(): Flow<List<FeedItem>> =
        databaseHelper.getFeedItems().map { feedList ->
            feedList.map { selectedFeed ->
                FeedItem(
                    id = selectedFeed.url_hash,
                    url = selectedFeed.url,
                    title = selectedFeed.title,
                    subtitle = selectedFeed.subtitle,
                    content = null,
                    imageUrl = selectedFeed.image_url,
                    feedSource = FeedSource(
                        id = selectedFeed.feed_source_id,
                        url = selectedFeed.feed_source_url,
                        title = selectedFeed.feed_source_title,
                        lastSyncTimestamp = selectedFeed.feed_source_last_sync_timestamp,
                    ),
                    isRead = selectedFeed.is_read,
                    pubDateMillis = selectedFeed.pub_date,
                    dateString = if (selectedFeed.pub_date != null) {
                        dateFormatter.formatDate(selectedFeed.pub_date)
                    } else {
                        null
                    },
                    commentsUrl = selectedFeed.comments_url,
                )
            }
        }.catch {
            logger.e(it) { "Something wrong while getting data from Database" }
            errorMutableState.update {
                DatabaseError
            }
        }.flowOn(dispatcherProvider.io)

    override suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>) =
        databaseHelper.updateReadStatus(itemsToUpdates)

    override suspend fun fetchFeeds(updateLoadingInfo: Boolean, forceRefresh: Boolean) {
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

                val feedResults = parseFeeds(feedSourceUrls, updateLoadingInfo, forceRefresh)

                val items = getFeedItems(
                    rssChannelResults = feedResults.filterIsInstance<RssParsingSuccess>(),
                )
                databaseHelper.insertFeedItems(items, dateFormatter.currentTimeMillis())
            }
        }
    }

    override suspend fun markAllFeedAsRead() {
        databaseHelper.markAllFeedAsRead()
    }

    @Suppress("MagicNumber")
    override suspend fun deleteOldFeeds() {
        // One week
        // (((1 hour in seconds) * 24 hours) * 7 days)
        val oneWeekInMillis = (((60 * 60) * 24) * 7) * 1000L
        val threshold = dateFormatter.currentTimeMillis() - oneWeekInMillis
        databaseHelper.deleteOldFeedItems(threshold)
    }

    private suspend fun parseFeeds(
        feedSourceUrls: List<FeedSource>,
        updateLoadingInfo: Boolean,
        forceRefresh: Boolean,
    ): List<RssParsingResult> =
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
                        RssParsingSuccess(
                            rssChannel = rssChannel,
                            feedSource = feedSource,
                        )
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
                        RssParsingError(
                            feedSource = feedSource,
                            throwable = e,
                        )
                    }
                }.asFlow()
            }
            .toList()

    private suspend fun getFeedItems(
        rssChannelResults: List<RssParsingSuccess>,
    ): List<FeedItem> =
        coroutineScope {
            rssChannelResults.map { rssChannelResults ->
                async {
                    rssChannelResults.rssChannel.getFeedItems(
                        feedSource = rssChannelResults.feedSource,
                    )
                }
            }.awaitAll()
                .flatten()
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

            if (title == null || url == null) {
                logger.d { "Skipping: $rssItem" }
                null
            } else {
                FeedItem(
                    id = url.hashCode(),
                    url = url,
                    title = title,
                    subtitle = rssItem.description?.let { description ->
                        htmlParser.getTextFromHTML(description)
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
}
