package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeVerificationBrowser
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedRecoveryServiceTest : KoinTestBase() {

    private var fixtureChannel: RssChannel? = channel(link = "https://recovery.test/post")

    private val service: FeedRecoveryService by inject()
    private val coordinator: FeedRecoveryCoordinator by inject()
    private val recoveryRepository: FeedRecoveryRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val networkSettings: NetworkSettings by inject()

    private val browser by lazy { FakeVerificationBrowser(coordinator, recoveryRepository) }

    override fun getTestModules(): List<Module> =
        super.getTestModules() +
            RecoveryTestModules.enabled(FakeWebsiteSessionHttp(), channel = null) +
            module { single { CapturedFeedParser { fixtureChannel ?: error("Invalid feed") } } }

    @Test
    fun `verified add saves the feed its items and an approved record`() = runTest(testDispatcher) {
        val saved = recover(addRequest())

        assertEquals(FEED_URL, saved.url)
        assertNotNull(databaseHelper.getFeedSource(saved.id))
        assertTrue(databaseHelper.hasFeedItems())
        val record = assertNotNull(recoveryRepository.records(epoch)[saved.id])
        assertEquals(RecoveryMode.SessionApproved, record.mode)
        assertNotNull(record.profileId)
    }

    @Test
    fun `verified add accepts relative item links`() = runTest(testDispatcher) {
        fixtureChannel = channel(link = "/post/1")

        val saved = recover(addRequest())

        assertEquals(1, timelineItems().count { it.feed_source_id == saved.id })
    }

    @Test
    fun `verified refresh keeps the user title`() = runTest(testDispatcher) {
        val source = FeedSourceGenerator.feedSource(id = "existing", title = "My title", url = FEED_URL)
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        val record = FeedRecoveryRecord(
            accountEpoch = epoch,
            sourceId = source.id,
            subscriptionUrl = source.url,
            effectiveUrl = source.url,
            origin = "https://recovery.test:443",
            mode = RecoveryMode.VerificationRequired,
            profileId = null,
            statusCode = 403,
            retryAfter = null,
        )

        recover(FeedRecoveryRequest.Refresh(source, record))

        assertEquals("My title", databaseHelper.getFeedSource(source.id)?.title)
        assertEquals(RecoveryMode.SessionApproved, recoveryRepository.records(epoch).getValue(source.id).mode)
    }

    @Test
    fun `remote sync account is unsupported without a browser`() = runTest(testDispatcher) {
        networkSettings.setSyncAccountType(SyncAccounts.FRESH_RSS)
        networkSettings.setSyncUrl("https://freshrss.test")
        networkSettings.setSyncPwd("password")
        browser.start(backgroundScope)

        val error = assertFailsWith<WebsiteVerificationFailure> {
            service.recover(addRequest(), recoveryRepository.issueTicket())
        }

        assertEquals(VerificationFailure.Unsupported, error.reason)
        assertTrue(browser.shownRequests.isEmpty())
    }

    @Test
    fun `future retry after is access denied without a browser`() = runTest(testDispatcher) {
        browser.start(backgroundScope)

        val error = assertFailsWith<WebsiteVerificationFailure> {
            service.recover(addRequest(retryAfter = "3600"), recoveryRepository.issueTicket())
        }

        assertEquals(VerificationFailure.AccessDenied, error.reason)
        assertTrue(browser.shownRequests.isEmpty())
    }

    @Test
    fun `account change while the browser is visible cancels and writes nothing`() = runTest(testDispatcher) {
        browser.respond = false
        browser.start(backgroundScope)
        val result = async { runCatching { service.recover(addRequest(), recoveryRepository.issueTicket()) } }
        runCurrent()
        assertEquals(1, browser.shownRequests.size)

        settingsRepository.rotateCloudSyncSession()
        runCurrent()

        assertTrue(result.await().exceptionOrNull() is CancellationException)
        assertTrue(databaseHelper.getFeedSources().isEmpty())
        assertTrue(databaseHelper.recoveryRecords(browser.shownRequests.single().accountEpoch).isEmpty())
    }

    @Test
    fun `invalid captured bytes are invalid content and write nothing`() = runTest(testDispatcher) {
        fixtureChannel = null

        val error = assertFailsWith<WebsiteVerificationFailure> { recover(addRequest()) }

        assertEquals(VerificationFailure.InvalidContent, error.reason)
        assertTrue(databaseHelper.getFeedSources().isEmpty())
        assertTrue(recoveryRepository.records(epoch).isEmpty())
    }

    private suspend fun TestScope.recover(request: FeedRecoveryRequest): FeedSource {
        browser.start(backgroundScope)
        return service.recover(request, recoveryRepository.issueTicket())
    }

    private val epoch: String get() = settingsRepository.cloudSyncSessionState.value

    private suspend fun timelineItems() = databaseHelper.getFeedItems(
        feedFilter = FeedFilter.Timeline,
        pageSize = 50,
        showReadItems = true,
        sortOrder = FeedOrder.NEWEST_FIRST,
    )

    private fun addRequest(retryAfter: String? = null) = FeedRecoveryRequest.Add(
        subscriptionUrl = FEED_URL,
        verificationUrl = FEED_URL,
        retryAfter = retryAfter,
        category = null,
        notificationsEnabled = false,
    )

    private fun channel(link: String) = RssChannelGenerator.rssChannel(
        title = "Recovered feed",
        link = "https://recovery.test",
        items = listOf(
            RssItemGenerator.rssItem(guid = "item", title = "Item", link = link, pubDate = null, commentsUrl = null),
        ),
    )

    private companion object {
        const val FEED_URL = "https://recovery.test/feed.xml"
    }
}
