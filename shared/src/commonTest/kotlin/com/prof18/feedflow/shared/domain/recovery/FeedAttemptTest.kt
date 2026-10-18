package com.prof18.feedflow.shared.domain.recovery

import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.RssParserWrapper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FeedAttemptTest : KoinTestBase() {

    private val databaseHelper: DatabaseHelper by inject()
    private val settingsRepository: SettingsRepository by inject()

    private val source = FeedSourceGenerator.feedSource(id = "source", url = "https://e.test/feed")
    private val wrapperChannel = RssChannelGenerator.rssChannel()
    private val sessionChannel = RssChannelGenerator.rssChannel()
    private val sessionHttp = FakeWebsiteSessionHttp()
    private val wrapper = CountingWrapper { wrapperChannel }

    private val repository by lazy {
        FeedRecoveryRepository(databaseHelper, settingsRepository, supported = true)
    }

    private fun attempt(
        http: WebsiteSessionHttp? = sessionHttp,
        rssParserWrapper: RssParserWrapper = wrapper,
    ) = FeedAttempt(
        rssParserWrapper = rssParserWrapper,
        recoveryRepository = repository,
        sessionHttp = http,
        capturedFeedParser = CapturedFeedParser { sessionChannel },
    )

    @Test
    fun `no record makes exactly one wrapper call`() = runTest(testDispatcher) {
        val outcome = attempt().run(source, record = null)

        assertEquals(FeedOutcome.Fetched(wrapperChannel, viaSession = false), outcome)
        assertEquals(1, wrapper.calls)
        assertTrue(sessionHttp.requests.isEmpty())
    }

    @Test
    fun `no record with not modified response`() = runTest(testDispatcher) {
        val notModified = CountingWrapper { throw HttpException(304, "Not Modified") }

        val outcome = attempt(rssParserWrapper = notModified).run(source, null)

        assertEquals(FeedOutcome.NotModified(viaSession = false), outcome)
    }

    @Test
    fun `no record with verification required is challenged`() = runTest(testDispatcher) {
        val challenge = FeedChallenge(source.url, "https://e.test/feed", 403, null)
        val challenged = CountingWrapper { throw FeedVerificationRequired(challenge) }

        assertEquals(FeedOutcome.Challenged(challenge), attempt(rssParserWrapper = challenged).run(source, null))
    }

    @Test
    fun `approved record fetches through the session`() = runTest(testDispatcher) {
        val record = approvedRecord(activeProfile().id)
        sessionHttp.enqueue(WebsiteSessionResponse.Body(ByteArray(0), "application/rss+xml", record.effectiveUrl))

        val outcome = attempt().run(source, record)

        assertEquals(FeedOutcome.Fetched(sessionChannel, viaSession = true), outcome)
        assertEquals(0, wrapper.calls)
        val request = sessionHttp.requests.single()
        assertEquals(source.url, request.cacheKey)
        assertEquals(record.effectiveUrl, request.url)
        assertEquals(WebsiteSessionKind.Feed, request.kind)
    }

    @Test
    fun `approved record with session not modified`() = runTest(testDispatcher) {
        sessionHttp.enqueue(WebsiteSessionResponse.NotModified)

        val outcome = attempt().run(source, approvedRecord(activeProfile().id))

        assertEquals(FeedOutcome.NotModified(viaSession = true), outcome)
    }

    @Test
    fun `approved record with session challenge is challenged without the wrapper`() = runTest(testDispatcher) {
        val record = approvedRecord(activeProfile().id)
        sessionHttp.enqueue(WebsiteSessionResponse.Challenge(403, "120"))

        val outcome = attempt().run(source, record)

        val challenged = assertIs<FeedOutcome.Challenged>(outcome)
        assertEquals("120", challenged.challenge.retryAfter)
        assertEquals(record.effectiveUrl, challenged.challenge.effectiveUrl)
        assertEquals(0, wrapper.calls)
    }

    @Test
    fun `approved record with session failure falls back to the wrapper`() = runTest(testDispatcher) {
        sessionHttp.enqueue(WebsiteSessionResponse.Failure(WebsiteSessionFailure.Redirect))

        val outcome = attempt().run(source, approvedRecord(activeProfile().id))

        assertEquals(FeedOutcome.Fetched(wrapperChannel, viaSession = false), outcome)
        assertEquals(1, wrapper.calls)
    }

    @Test
    fun `approved record with a retired profile uses the wrapper only`() = runTest(testDispatcher) {
        val profile = activeProfile()
        repository.clearWebsite(profile.id)

        attempt().run(source, approvedRecord(profile.id))

        assertEquals(1, wrapper.calls)
        assertTrue(sessionHttp.requests.isEmpty())
    }

    @Test
    fun `verification required record uses the wrapper only`() = runTest(testDispatcher) {
        val record = approvedRecord(activeProfile().id).copy(mode = RecoveryMode.VerificationRequired)

        attempt().run(source, record)

        assertEquals(1, wrapper.calls)
        assertTrue(sessionHttp.requests.isEmpty())
    }

    @Test
    fun `missing session http uses the wrapper only`() = runTest(testDispatcher) {
        val outcome = attempt(http = null).run(source, approvedRecord(activeProfile().id))

        assertSame(wrapperChannel, assertIs<FeedOutcome.Fetched>(outcome).channel)
        assertEquals(1, wrapper.calls)
    }

    @Test
    fun `approved record with a throwing session falls back to the wrapper`() = runTest(testDispatcher) {
        val throwing = object : WebsiteSessionHttp {
            override suspend fun fetch(request: WebsiteSessionRequest): WebsiteSessionResponse =
                error("profile store failed")
        }

        val outcome = attempt(http = throwing).run(source, approvedRecord(activeProfile().id))

        assertEquals(FeedOutcome.Fetched(wrapperChannel, viaSession = false), outcome)
        assertEquals(1, wrapper.calls)
    }

    private suspend fun activeProfile(): FeedRecoveryProfile = repository.acquireProfile(
        epoch = settingsRepository.cloudSyncSessionState.value,
        origin = requireNotNull(RecoveryOrigin.of(source.url)),
        userAgent = "ua",
        engineVersion = "engine",
    )

    private fun approvedRecord(profileId: String) = FeedRecoveryRecord(
        accountEpoch = settingsRepository.cloudSyncSessionState.value,
        sourceId = source.id,
        subscriptionUrl = source.url,
        effectiveUrl = "https://e.test/feed?verified",
        origin = "https://e.test:443",
        mode = RecoveryMode.SessionApproved,
        profileId = profileId,
        statusCode = 403,
        retryAfter = null,
    )

    private class CountingWrapper(private val result: () -> RssChannel) : RssParserWrapper {
        var calls = 0
            private set

        override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
            calls++
            return result()
        }
    }
}
