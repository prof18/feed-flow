package com.prof18.feedflow.shared.domain.feed

import app.cash.sqldelight.db.SqlDriver
import app.cash.turbine.test
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.test.FailableSqlDriver
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

class FeedFetcherRepositoryDatabaseFailureTest : FeedFetcherRepositoryTestBase() {

    private val failableDriver = FailableSqlDriver(createInMemoryDriver())
    private val parser = ScriptedRssParser()
    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + module {
            single<SqlDriver> { failableDriver }
            single<RssParserWrapper> { parser }
        }

    @BeforeTest
    fun setUpAccount() {
        getKoin().get<NetworkSettings>().setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `a busy database while saving the refresh is reported, not thrown`() = runTest(testDispatcher) {
        val source = insertSource("source-1")
        parser.channels[source.url] = channelWithOneItem(source)
        failWritesMatching("INSERT OR IGNORE INTO feed_item")

        assertSaveFailedIsEmitted()
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `a busy database while flagging a failed feed is reported, not thrown`() = runTest(testDispatcher) {
        val source = insertSource("source-1")
        parser.failingUrls += source.url
        failWritesMatching("SET fetch_failed = ")

        assertSaveFailedIsEmitted()
    }

    @Test
    fun `a busy database while saving per-source cache info is reported, not thrown`() =
        runTest(testDispatcher) {
            val source = insertSource("source-1")
            parser.failingUrls += source.url
            failWritesMatching("feed_source_cache_info")

            assertSaveFailedIsEmitted()
        }

    @Test
    fun `a busy database while cleaning old articles stays contained by the cleanup itself`() =
        runTest(testDispatcher) {
            // deleteOldFeedItems and cleanupOldDeletedItems already catch their own failures.
            val source = insertSource("source-1")
            parser.channels[source.url] = channelWithOneItem(source)
            settingsRepository.setAutoDeletePeriod(AutoDeletePeriod.ONE_DAY)
            failWritesMatching("deleted_feed_items")

            feedFetcherRepository.fetchFeeds(forceRefresh = true)
            advanceUntilIdle()

            assertTrue(databaseHelper.hasFeedItems())
            assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
        }

    @Test
    fun `a non database failure while saving keeps today's behaviour`() = runTest(testDispatcher) {
        val source = insertSource("source-1")
        parser.channels[source.url] = channelWithOneItem(source)
        failableDriver.writeFailure = { sql ->
            if ("INSERT OR IGNORE INTO feed_item" in sql) IllegalStateException("not a database failure") else null
        }

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    @Test
    fun `unchanged feeds still get their timestamp and lose the failure flag`() = runTest(testDispatcher) {
        val source = insertSource("source-1")
        databaseHelper.setFeedFetchFailed(source.id, true)
        parser.notModifiedUrls += source.url

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        val saved = assertNotNull(databaseHelper.getFeedSource(source.id))
        assertNotNull(saved.lastSyncTimestamp)
        assertFalse(saved.fetchFailed)
    }

    @Test
    fun `a feed that times out stays a per-feed failure and the others are saved`() = runTest(testDispatcher) {
        val hanging = insertSource("hanging")
        val healthy = insertSource("healthy")
        parser.hangingUrls += hanging.url
        parser.channels[healthy.url] = channelWithOneItem(healthy)

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertTrue(assertNotNull(databaseHelper.getFeedSource(hanging.id)).fetchFailed)
        assertTrue(databaseHelper.hasFeedItems())
        assertEquals(FinishedFeedUpdateStatus, feedStateRepository.updateState.value)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.assertSaveFailedIsEmitted() {
        feedStateRepository.errorState.test {
            feedFetcherRepository.fetchFeeds(forceRefresh = true)
            advanceUntilIdle()
            assertEquals(DatabaseError(DatabaseErrorCode.SaveFailed), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun failWritesMatching(fragment: String) {
        failableDriver.writeFailure = { sql ->
            if (fragment in sql) SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY) else null
        }
    }

    private suspend fun insertSource(id: String): FeedSource {
        val source = FeedSource(
            id = id,
            url = "https://example.com/$id/rss.xml",
            title = id,
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        return source
    }

    private fun channelWithOneItem(source: FeedSource): RssChannel = RssChannelGenerator.rssChannel(
        title = source.title,
        link = "https://example.com/${source.id}",
        items = listOf(
            RssItemGenerator.rssItem(
                guid = "${source.id}-item",
                title = "Item",
                link = "https://example.com/${source.id}/item",
            ),
        ),
    )

    private class ScriptedRssParser : RssParserWrapper {
        val channels = mutableMapOf<String, RssChannel>()
        val failingUrls = mutableSetOf<String>()
        val notModifiedUrls = mutableSetOf<String>()
        val hangingUrls = mutableSetOf<String>()

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            if (url in hangingUrls) awaitCancellation()
            if (url in notModifiedUrls) throw HttpException(code = 304, message = "Not Modified")
            if (url in failingUrls) error("Failure for $url")
            return requireNotNull(channels[url]) { "Missing channel for $url" }
        }
    }
}
