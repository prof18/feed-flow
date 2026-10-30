package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCacheInfo
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.recovery.FeedChallenge
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.FeedVerificationRequired
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionResponse
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedFetcherRepositoryRecoveryTest : FeedFetcherRepositoryTestBase() {

    private val sessionHttp = FakeWebsiteSessionHttp()
    private val sessionChannel = channel("session")
    private val wrapper = UrlRssParserWrapper()

    private val feedFetcherRepository: FeedFetcherRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val recoveryRepository: FeedRecoveryRepository by inject()
    private val dateFormatter: DateFormatter by inject()

    private val normalA = source("normal-a")
    private val normalB = source("normal-b")
    private val challenged = source("challenged")

    private val epoch: String get() = settingsRepository.cloudSyncSessionState.value

    override fun getTestModules(): List<Module> =
        super.getTestModules() +
            module { single<RssParserWrapper> { wrapper } } +
            RecoveryTestModules.enabled(sessionHttp, sessionChannel)

    private fun setupLocalAccount() {
        getKoin().get<NetworkSettings>().setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `a challenged feed is recorded while normal feeds are saved`() = runTest(testDispatcher) {
        setupLocalAccount()
        insert(normalA, normalB, challenged)
        wrapper.channels[normalA.url] = channel("normal-a")
        wrapper.channels[normalB.url] = channel("normal-b")
        wrapper.challenged += challenged.url

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        val itemSources = timelineItems().map { it.feed_source_id }.toSet()
        assertEquals(setOf(normalA.id, normalB.id), itemSources)
        assertTrue(storedSource(challenged.id).fetchFailed)
        assertEquals(RecoveryMode.VerificationRequired, recoveryRepository.records(epoch).getValue(challenged.id).mode)
        assertNull(recoveryRepository.records(epoch)[normalA.id])
    }

    @Test
    fun `an approved feed refreshes through the session and stays approved`() = runTest(testDispatcher) {
        setupLocalAccount()
        insert(challenged)
        storeRecord(approvedRecord(challenged))
        sessionHttp.enqueue(WebsiteSessionResponse.Body(ByteArray(0), "application/rss+xml", challenged.url))

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertEquals(setOf(challenged.id), timelineItems().map { it.feed_source_id }.toSet())
        assertEquals(RecoveryMode.SessionApproved, recoveryRepository.records(epoch).getValue(challenged.id).mode)
        assertEquals(0, wrapper.calls)
    }

    @Test
    fun `an approved feed that is not modified keeps its validators`() = runTest(testDispatcher) {
        setupLocalAccount()
        insert(challenged)
        storeRecord(approvedRecord(challenged))
        databaseHelper.updateFeedSourcesCacheInfo(listOf(cacheInfo(challenged.id)))
        sessionHttp.enqueue(WebsiteSessionResponse.NotModified)

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertNotNull(storedSource(challenged.id).lastSyncTimestamp)
        assertEquals("\"etag\"", databaseHelper.getFeedSourcesCacheInfo().single().etag)
        assertEquals(challenged.url, sessionHttp.requests.single().cacheKey)
        assertEquals(0, wrapper.calls)
    }

    @Test
    fun `a pending record is cleared when the normal fetch succeeds`() = runTest(testDispatcher) {
        setupLocalAccount()
        insert(challenged)
        storeRecord(approvedRecord(challenged).copy(mode = RecoveryMode.VerificationRequired, profileId = null))
        wrapper.channels[challenged.url] = channel("recovered")

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertTrue(recoveryRepository.records(epoch).isEmpty())
        assertFalse(storedSource(challenged.id).fetchFailed)
    }

    @Test
    fun `an account change during the fetch leaves recovery untouched`() = runTest(testDispatcher) {
        setupLocalAccount()
        insert(challenged)
        val initialEpoch = epoch
        wrapper.challenged += challenged.url
        wrapper.onRequest = { settingsRepository.rotateCloudSyncSession() }

        feedFetcherRepository.fetchFeeds(forceRefresh = true)
        advanceUntilIdle()

        assertTrue(databaseHelper.recoveryRecords(initialEpoch).isEmpty())
        assertTrue(databaseHelper.recoveryRecords(epoch).isEmpty())
    }

    private suspend fun insert(vararg sources: FeedSource) {
        databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
    }

    private suspend fun storeRecord(record: FeedRecoveryRecord) {
        databaseHelper.upsertRecoveryRecord(record) { it() }
    }

    private suspend fun storedSource(id: String): FeedSource =
        databaseHelper.getFeedSources().single { it.id == id }

    private suspend fun timelineItems() = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 50,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    )

    private suspend fun approvedRecord(source: FeedSource): FeedRecoveryRecord {
        val profile = recoveryRepository.acquireProfile(
            epoch = epoch,
            origin = requireNotNull(RecoveryOrigin.of(source.url)),
            userAgent = "ua",
            engineVersion = "engine",
        )
        return FeedRecoveryRecord(
            accountEpoch = epoch,
            sourceId = source.id,
            subscriptionUrl = source.url,
            effectiveUrl = source.url,
            origin = requireNotNull(RecoveryOrigin.of(source.url)).value,
            mode = RecoveryMode.SessionApproved,
            profileId = profile.id,
            statusCode = 403,
            retryAfter = null,
        )
    }

    private fun cacheInfo(sourceId: String) = FeedSourceCacheInfo(
        feedSourceId = sourceId,
        etag = "\"etag\"",
        lastModified = null,
        validatorsTimestamp = dateFormatter.currentTimeMillis(),
        nextFetchTimestamp = null,
        backoffTimestamp = null,
        userAgentTier = null,
    )

    private fun source(id: String) = FeedSourceGenerator.feedSource(
        id = id,
        title = id,
        url = "https://$id.test/feed.xml",
        websiteUrl = "https://$id.test",
        logoUrl = "https://$id.test/logo.png",
    )

    private fun channel(name: String) = RssChannelGenerator.rssChannel(
        title = name,
        link = "https://$name.test",
        items = listOf(
            RssItemGenerator.rssItem(
                guid = "$name-item",
                title = "$name item",
                link = "https://$name.test/item",
                pubDate = null,
                commentsUrl = null,
            ),
        ),
    )

    private class UrlRssParserWrapper : RssParserWrapper {
        val channels = mutableMapOf<String, RssChannel>()
        val challenged = mutableSetOf<String>()
        var onRequest: () -> Unit = {}
        var calls = 0
            private set

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            calls++
            onRequest()
            if (url in challenged) {
                throw FeedVerificationRequired(FeedChallenge(url, url, 403, null))
            }
            return channels[url] ?: error("No channel for $url")
        }
    }
}
