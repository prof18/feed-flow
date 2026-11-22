package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.FeedSourceLogoRetriever
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A refresh of a large subscription list must not hold every parsed article until the end:
 * that is what exhausts the heap on Android when hundreds of feeds are loaded.
 */
class FeedFetcherRepositoryBatchPersistTest : FeedFetcherRepositoryTestBase() {

    private val fakeRssParserWrapper = GatedRssParserWrapper()
    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<RssParserWrapper> { fakeRssParserWrapper }
            // Keeps every step of the refresh on the test dispatcher: the real retriever resolves
            // logos over HTTP, which would settle outside the test scheduler's control.
            factory<FeedSourceLogoRetriever> { StoredLogoRetriever() }
        }

    @Test
    fun `articles are persisted while the refresh is still running`() = runTest(testDispatcher) {
        setupLocalAccount()

        val bulkSources = (0 until BULK_SOURCE_COUNT).map { index ->
            createFeedSource(id = "bulk-$index", title = "Bulk $index")
        }
        val gatedSource = createFeedSource(id = "gated", title = "Gated")
        databaseHelper.insertFeedSource((bulkSources + gatedSource).map { it.toParsedFeedSource() })

        bulkSources.forEach { source ->
            fakeRssParserWrapper.setChannel(source.url, channelWith(source.id, ITEMS_PER_BULK_SOURCE))
        }
        fakeRssParserWrapper.setChannel(gatedSource.url, channelWith(gatedSource.id, itemCount = 1))

        // The last source never answers, so the refresh cannot reach a final save.
        val releaseGatedSource = CompletableDeferred<Unit>()
        fakeRssParserWrapper.onRequest = { url ->
            if (url == gatedSource.url) {
                releaseGatedSource.await()
            }
        }

        val refresh = launch { feedFetcherRepository.fetchFeeds(forceRefresh = true) }
        runCurrent()

        assertTrue(
            getTimelineItems().isNotEmpty(),
            "Articles from finished sources must already be saved while one source is still " +
                "in flight, instead of being held in memory until the whole refresh ends",
        )

        releaseGatedSource.complete(Unit)
        refresh.join()
        advanceUntilIdle()

        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
        assertEquals(
            BULK_SOURCE_COUNT * ITEMS_PER_BULK_SOURCE + 1,
            getTimelineItems(pageSize = ALL_ITEMS_PAGE_SIZE).size,
            "Every article of the refresh must still be saved exactly once",
        )
    }

    private suspend fun getTimelineItems(pageSize: Long = PAGE_SIZE) = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = pageSize,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    )

    private fun setupLocalAccount() {
        val settings: NetworkSettings = getKoin().get()
        settings.setSyncAccountType(SyncAccounts.LOCAL)
    }

    private fun channelWith(sourceId: String, itemCount: Int): RssChannel =
        RssChannelGenerator.rssChannel(
            title = sourceId,
            link = "https://example.com/$sourceId",
            items = (0 until itemCount).map { index ->
                RssItemGenerator.rssItem(
                    guid = "$sourceId-item-$index",
                    title = "$sourceId item $index",
                    link = "https://example.com/$sourceId/article-$index",
                    pubDate = null,
                    categories = emptyList(),
                    commentsUrl = null,
                )
            },
        )

    private fun createFeedSource(id: String, title: String): FeedSource = FeedSource(
        id = id,
        url = "https://example.com/$id/rss.xml",
        title = title,
        category = null,
        lastSyncTimestamp = null,
        logoUrl = "https://example.com/$id/logo.png",
        websiteUrl = "https://example.com/$id",
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private class GatedRssParserWrapper : RssParserWrapper {
        private val channelByUrl = mutableMapOf<String, RssChannel>()
        var onRequest: suspend (String) -> Unit = {}

        fun setChannel(url: String, channel: RssChannel) {
            channelByUrl[url] = channel
        }

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            onRequest(url)
            return requireNotNull(channelByUrl[url]) { "Missing channel for $url" }
        }
    }

    /** Reports every stored logo as still available, so no refresh step reaches the network. */
    private class StoredLogoRetriever : FeedSourceLogoRetriever {
        override suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel, feedUrl: String): String? = null

        override suspend fun getFeedSourceLogoUrl(websiteLink: String?): String? = null

        override suspend fun isStoredLogoStillAvailable(logoUrl: String): Boolean = true

        override suspend fun invalidateLookupCaches() = Unit
    }

    private companion object {
        // Enough articles to cross the persist batch size while one source is still pending.
        const val BULK_SOURCE_COUNT = 4
        const val ITEMS_PER_BULK_SOURCE = 200
        const val PAGE_SIZE = 50L
        const val ALL_ITEMS_PAGE_SIZE = 5000L
    }
}
