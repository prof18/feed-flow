package com.prof18.feedflow.domain

import co.touchlab.kermit.Logger
import com.prof.rssparser.Channel
import com.prof.rssparser.Parser
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.domain.model.FeedItemId
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.domain.model.RssChannelResult
import com.prof18.feedflow.domain.model.StartedFeedUpdateStatus
import com.prof18.feedflow.presentation.model.ErrorState
import com.prof18.feedflow.presentation.model.FeedErrorState
import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class FeedRetrieverRepository(
    private val parser: Parser,
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val updateMutableState: MutableStateFlow<FeedUpdateStatus> = MutableStateFlow(
        FinishedFeedUpdateStatus(
            refreshedFeedCount = 0, totalFeedCount = 0,
        ),
    )
    val updateState = updateMutableState.asStateFlow()

    private val errorMutableState: MutableStateFlow<ErrorState?> = MutableStateFlow(null)
    val errorState = errorMutableState.asStateFlow()

    fun getFeeds(): Flow<List<FeedItem>> = databaseHelper.getFeedItems().map { feedList ->
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
                ),
                isRead = selectedFeed.is_read,
                pubDateMillis = selectedFeed.pub_date,
                dateString = formatDate(selectedFeed.pub_date),
                commentsUrl = selectedFeed.comments_url,
            )
        }
    }

    suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>) =
        databaseHelper.updateReadStatus(itemsToUpdates)

    suspend fun fetchFeeds(updateLoadingInfo: Boolean = true) {
        Logger.d { ">>> UpdateLoanding info? $updateLoadingInfo" }
        if (updateLoadingInfo) {
            updateMutableState.update { StartedFeedUpdateStatus }
        } else {
            updateMutableState.update {
                FinishedFeedUpdateStatus(
                    refreshedFeedCount = 0, totalFeedCount = 0,
                )
            }
        }
        val feedSourceUrls = databaseHelper.getFeedSources()
        if (feedSourceUrls.isEmpty()) {
            updateMutableState.update {
                NoFeedSourcesStatus
            }
        } else {
            databaseHelper.updateNewStatus()
            createFetchingPipeline(feedSourceUrls, updateLoadingInfo)
        }
    }

    suspend fun markAllFeedAsRead() {
        databaseHelper.markAllFeedAsRead()
    }

    suspend fun deleteOldFeeds() {
        val threshold = Clock.System.now().minus(7.days).epochSeconds
        databaseHelper.deleteOldFeedItems(threshold)
    }

    private fun CoroutineScope.produceFeedSources(
        feedSourceUrls: List<FeedSource>,
        updateLoadingInfo: Boolean,
    ) = produce {
        if (updateLoadingInfo) {
            updateMutableState.emit(
                InProgressFeedUpdateStatus(
                    refreshedFeedCount = 0,
                    totalFeedCount = feedSourceUrls.size,
                )
            )
        }
        Logger.d { "Feed Size: ${feedSourceUrls.size}" }
        for (feedSource in feedSourceUrls) {
            send(feedSource)
        }
    }

    private suspend fun createFetchingPipeline(
        feedSourceUrls: List<FeedSource>,
        updateLoadingInfo: Boolean,
    ) = coroutineScope {
        val feedSourcesChannel = produceFeedSources(feedSourceUrls, updateLoadingInfo)

        val feedToSaveChannel = produce(capacity = UNLIMITED) {
            repeat(NUMBER_OF_CONCURRENT_PARSING_REQUESTS) {
                launch(dispatcherProvider.default) {
                    for (feedSource in feedSourcesChannel) {
                        Logger.d { "-> Getting ${feedSource.url}" }
                        try {
                            val rssChannel = parser.getChannel(feedSource.url)
                            val result = RssChannelResult(
                                rssChannel = rssChannel,
                                feedSource = feedSource,
                            )
                            send(result)
                        } catch (e: Exception) {
                            Logger.e(e) { "Something went wrong, skipping: ${feedSource.url}}" }
                            e.printStackTrace()
                            errorMutableState.update {
                                FeedErrorState(
                                    failingSourceName = feedSource.title
                                )
                            }

                            if (updateLoadingInfo) {
                                updateRefreshCount()
                            }
                        }
                    }
                }
            }
        }

        repeat(NUMBER_OF_CONCURRENT_FEED_SAVER) {
            launch(dispatcherProvider.io) {
                for (rssChannelResult in feedToSaveChannel) {
                    Logger.d {
                        "<- Got back ${rssChannelResult.rssChannel.title}"
                    }

                    if (updateLoadingInfo) {
                        updateRefreshCount()
                    }

                    val feedItems = rssChannelResult.rssChannel.getFeedItems(
                        feedSource = rssChannelResult.feedSource
                    )
                    databaseHelper.insertFeedItems(feedItems)
                }
            }
        }

    }

    private fun updateRefreshCount() {
        updateMutableState.update { oldUpdate ->
            val refreshedFeedCount = oldUpdate.refreshedFeedCount + 1
            val totalFeedCount = oldUpdate.totalFeedCount

            Logger.d {
                "Refreshed: $refreshedFeedCount. Total: $totalFeedCount"
            }
            if (refreshedFeedCount == totalFeedCount) {
                FinishedFeedUpdateStatus(
                    refreshedFeedCount = refreshedFeedCount,
                    totalFeedCount = totalFeedCount,
                )
            } else {
                InProgressFeedUpdateStatus(
                    refreshedFeedCount = refreshedFeedCount,
                    totalFeedCount = totalFeedCount,
                )
            }
        }
    }

    private fun Channel.getFeedItems(feedSource: FeedSource): List<FeedItem> =
        this.articles.mapNotNull { article ->

            val title = article.title
            val url = article.link
            val pubDate = article.pubDate

            val randomTimeToSubtract = Random.nextLong(1800000L, 10800000L)
            val defaultDate = Clock.System.now().epochSeconds - randomTimeToSubtract

            val dateMillis = if (pubDate != null) {
                getDateMillisFromString(pubDate) ?: defaultDate
            } else {
                // Between 30 minutes and 3 hours ago
                defaultDate
            }

            if (title == null || url == null) {
                Logger.d { "Skipping: $article" }
                null
            } else {
                FeedItem(
                    id = url.hashCode(),
                    url = url,
                    title = title,
                    subtitle = article.description?.let { description ->
                        getTextFromHTML(description)
                    },
                    content = article.content,
                    imageUrl = article.image,
                    feedSource = feedSource,
                    isRead = false,
                    pubDateMillis = dateMillis,
                    dateString = formatDate(dateMillis),
                    commentsUrl = article.commentsUrl,
                )
            }

        }

    private companion object {
        const val NUMBER_OF_CONCURRENT_PARSING_REQUESTS = 20
        const val NUMBER_OF_CONCURRENT_FEED_SAVER = 20
    }
}
