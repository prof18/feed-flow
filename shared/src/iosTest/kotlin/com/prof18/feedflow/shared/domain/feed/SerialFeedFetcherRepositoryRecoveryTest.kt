package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.recovery.FeedChallenge
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.FeedVerificationRequired
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionResponse
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SerialFeedFetcherRepositoryRecoveryTest : KoinTestBase() {

    private val sessionHttp = FakeWebsiteSessionHttp()
    private val wrapper = UrlRssParserWrapper()

    private val repository: SerialFeedFetcherRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val recoveryRepository: FeedRecoveryRepository by inject()

    private val normal = source("normal")
    private val challenged = source("challenged")

    private val epoch: String get() = settingsRepository.cloudSyncSessionState.value

    override fun getTestModules(): List<Module> =
        super.getTestModules() +
            module { single<RssParserWrapper> { wrapper } } +
            RecoveryTestModules.enabled(sessionHttp, channel("session"))

    @Test
    fun `a challenged feed is recorded while normal feeds are saved`() = runTest(testDispatcher) {
        insert(normal, challenged)
        wrapper.channels[normal.url] = channel("normal")
        wrapper.challenged += challenged.url

        repository.fetchFeeds(forceRefresh = true)

        assertEquals(setOf(normal.id), timelineItems().map { it.feed_source_id }.toSet())
        assertEquals(RecoveryMode.VerificationRequired, recoveryRepository.records(epoch).getValue(challenged.id).mode)
    }

    @Test
    fun `a challenged feed does not set the fetch failed flag`() = runTest(testDispatcher) {
        insert(challenged)
        wrapper.challenged += challenged.url

        repository.fetchFeeds(forceRefresh = true)

        assertFalse(databaseHelper.getFeedSources().single().fetchFailed)
    }

    @Test
    fun `an approved feed refreshes through the session and stays approved`() = runTest(testDispatcher) {
        insert(challenged)
        storeRecord(approvedRecord(challenged))
        sessionHttp.enqueue(WebsiteSessionResponse.Body(ByteArray(0), "application/rss+xml", challenged.url))

        repository.fetchFeeds(forceRefresh = true)

        assertEquals(setOf(challenged.id), timelineItems().map { it.feed_source_id }.toSet())
        assertEquals(RecoveryMode.SessionApproved, recoveryRepository.records(epoch).getValue(challenged.id).mode)
        assertEquals(0, wrapper.calls)
    }

    @Test
    fun `a pending record is cleared when the normal fetch succeeds`() = runTest(testDispatcher) {
        insert(challenged)
        storeRecord(approvedRecord(challenged).copy(mode = RecoveryMode.VerificationRequired, profileId = null))
        wrapper.channels[challenged.url] = channel("recovered")

        repository.fetchFeeds(forceRefresh = true)

        assertTrue(recoveryRepository.records(epoch).isEmpty())
    }

    private suspend fun insert(vararg sources: FeedSource) {
        databaseHelper.insertFeedSource(sources.map { it.toParsedFeedSource() })
    }

    private suspend fun storeRecord(record: FeedRecoveryRecord) {
        databaseHelper.upsertRecoveryRecord(record) { it() }
    }

    private suspend fun timelineItems() = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 50,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    )

    private suspend fun approvedRecord(source: FeedSource): FeedRecoveryRecord {
        val origin = requireNotNull(RecoveryOrigin.of(source.url))
        val profile = recoveryRepository.acquireProfile(epoch, origin, "ua", "engine")
        return FeedRecoveryRecord(
            accountEpoch = epoch,
            sourceId = source.id,
            subscriptionUrl = source.url,
            effectiveUrl = source.url,
            origin = origin.value,
            mode = RecoveryMode.SessionApproved,
            profileId = profile.id,
            statusCode = 403,
            retryAfter = null,
        )
    }

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
        var calls = 0
            private set

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            calls++
            if (url in challenged) {
                throw FeedVerificationRequired(FeedChallenge(url, url, 403, null))
            }
            return channels[url] ?: error("No channel for $url")
        }
    }
}
