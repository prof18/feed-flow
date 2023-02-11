package com.prof18.feedflow

import co.touchlab.kermit.Logger
import com.prof.rssparser.Channel
import com.prof.rssparser.Parser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownServiceException
import java.util.Date

class FeedRetrieverRepository(
    private val parser: Parser,
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val updateMutableState: MutableStateFlow<FeedUpdateStatus> =
        MutableStateFlow(StartedFeedUpdateStatus)
    val updateState = updateMutableState.asStateFlow()

    fun getFeeds(): Flow<List<FeedItem>> = databaseHelper.getFeedItems()

    suspend fun updateReadStatus(itemsToUpdates: List<FeedItemId>) =
        databaseHelper.updateReadStatus(itemsToUpdates)

    suspend fun fetchFeeds() {
        val feedSourceUrls = databaseHelper.getFeedSourceUrls()
        if (feedSourceUrls.isEmpty()) {
            updateMutableState.emit(
                FinishedFeedUpdateStatus(
                    refreshedFeedCount = 0,
                    totalFeedCount = 0,
                )
            )
            throw NoFeedException()
        } else {
            databaseHelper.updateNewStatus()
            createFetchingPipeline(feedSourceUrls)
        }
    }

    private fun CoroutineScope.produceFeedSources(feedSourceUrls: List<FeedSource>) = produce {
        updateMutableState.emit(
            InProgressFeedUpdateStatus(
                refreshedFeedCount = 0,
                totalFeedCount = feedSourceUrls.size,
            )
        )
        Logger.d { "Feed Size: ${feedSourceUrls.size}" }
        for (feedSource in feedSourceUrls) {
            send(feedSource)
        }
    }

    private suspend fun createFetchingPipeline(feedSourceUrls: List<FeedSource>) = coroutineScope {
        val feedSourcesChannel = produceFeedSources(feedSourceUrls)

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
                        } catch (e: UnknownServiceException) {
                            // TODO: send error as well as result
                            // TODO: report error somewhere?
                            Logger.e(e) { "Something went wrong, skipping: ${e.printStackTrace()}" }
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

                    val feedItems = rssChannelResult.rssChannel.getFeedItems(rssChannelResult.feedSource)
                    databaseHelper.insertFeedItems(feedItems)
                }
            }
        }

    }

    private fun Channel.getFeedItems(feedSource: FeedSource): List<FeedItem> =
        this.articles.mapNotNull { article ->

            val title = article.title
            val url = article.link
            val pubDate = article.pubDate
            val dateMillis = if (pubDate != null) {
                getDateMillisFromString(pubDate)
            } else {
                Date().time
            }

            if (title == null || url == null || dateMillis == null) {
                Logger.d { "Skipping: $article" }
                null
            } else {
                FeedItem(
                    id = url.hashCode(),
                    url = url,
                    title = title,
                    subtitle = article.description,
                    content = article.content,
                    imageUrl = article.image,
                    feedSource = feedSource,
                    isRead = false,
                    pubDateMillis = dateMillis
                )
            }

        }


    private companion object {
        const val NUMBER_OF_CONCURRENT_PARSING_REQUESTS = 20
        const val NUMBER_OF_CONCURRENT_FEED_SAVER = 20
    }

}
