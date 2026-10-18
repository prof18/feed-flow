package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.domain.model.FeedEditedState
import com.prof18.feedflow.shared.domain.recovery.FeedChallenge
import com.prof18.feedflow.shared.domain.recovery.FeedVerificationRequired
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal abstract class FeedSourcesRepositoryChallengeTestBase(
    private val recoverySupported: Boolean,
) : KoinTestBase() {

    protected val wrapper = ScriptedRssParserWrapper()
    protected val feedSourcesRepository: FeedSourcesRepository by inject()
    protected val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> {
        val modules = super.getTestModules() + module { single<RssParserWrapper> { wrapper } }
        return if (recoverySupported) modules + RecoveryTestModules.enabled(FakeWebsiteSessionHttp(), null) else modules
    }

    protected fun setupLocalAccount() {
        getKoin().get<NetworkSettings>().setSyncAccountType(SyncAccounts.LOCAL)
    }

    protected suspend fun add(url: String) = feedSourcesRepository.addFeedSource(
        feedUrl = url,
        categoryName = null,
        isNotificationEnabled = false,
    )

    protected companion object {
        const val SITE = "https://challenged.test"
    }
}

internal class FeedSourcesRepositoryLocalChallengeTest :
    FeedSourcesRepositoryChallengeTestBase(recoverySupported = true) {

    @Test
    fun `a challenged add returns the verification url`() = runTest(testDispatcher) {
        setupLocalAccount()
        wrapper.challengeEverything(retryAfter = "30")

        val result = assertIs<FeedAddedState.Error.FetchFailed>(add(SITE))

        assertEquals(403, result.statusCode)
        assertEquals("${wrapper.requestedUrls.first()}?challenged", result.verificationUrl)
        assertEquals("30", result.retryAfter)
    }

    @Test
    fun `a challenged original url still finds a suffix feed`() = runTest(testDispatcher) {
        setupLocalAccount()
        wrapper.challengeEverything(retryAfter = null)
        wrapper.channels["$SITE/feed"] = channel()

        assertIs<FeedAddedState.FeedAdded>(add(SITE))
        assertEquals("$SITE/feed", databaseHelper.getFeedSources().single().url)
    }

    @Test
    fun `the original challenge wins over candidate challenges`() = runTest(testDispatcher) {
        setupLocalAccount()
        wrapper.challengeEverything(retryAfter = null)

        val result = assertIs<FeedAddedState.Error.FetchFailed>(add(SITE))

        assertTrue(wrapper.requestedUrls.size > 1)
        assertEquals("${wrapper.requestedUrls.first()}?challenged", result.verificationUrl)
    }

    @Test
    fun `a challenge on a candidate url carries that url for verification`() = runTest(testDispatcher) {
        setupLocalAccount()
        wrapper.challengeEverything(retryAfter = null)
        wrapper.httpErrors[SITE] = 404

        val result = assertIs<FeedAddedState.Error.FetchFailed>(add(SITE))

        val candidate = wrapper.requestedUrls[1]
        assertEquals(candidate, result.challengedUrl)
        assertEquals("$candidate?challenged", result.verificationUrl)
    }

    @Test
    fun `an edit that fails with a server error still saves preferences`() = runTest(testDispatcher) {
        setupLocalAccount()
        val original = FeedSourceGenerator.feedSource(id = "edit", url = "$SITE/rss.xml", isPinned = false)
        databaseHelper.insertFeedSource(listOf(original.toParsedFeedSource()))
        wrapper.httpErrors["https://moved.test/rss.xml"] = 500

        val result = feedSourcesRepository.editFeedSource(
            newFeedSource = original.copy(url = "https://moved.test/rss.xml", isPinned = true),
            originalFeedSource = original,
        )

        assertIs<FeedEditedState.Error>(result)
        assertTrue(databaseHelper.getFeedSource(original.id)?.isPinned == true)
    }

    private fun channel() = RssChannelGenerator.rssChannel(
        title = "Suffix feed",
        link = SITE,
        items = listOf(RssItemGenerator.rssItem(link = "$SITE/post", pubDate = null, commentsUrl = null)),
    )
}

internal class FeedSourcesRepositoryUnsupportedChallengeTest :
    FeedSourcesRepositoryChallengeTestBase(recoverySupported = false) {

    @Test
    fun `a challenged add without recovery support has no verification url`() = runTest(testDispatcher) {
        setupLocalAccount()
        wrapper.challengeEverything(retryAfter = null)

        val result = assertIs<FeedAddedState.Error.FetchFailed>(add(SITE))

        assertEquals(403, result.statusCode)
        assertNull(result.verificationUrl)
    }
}

class ScriptedRssParserWrapper : RssParserWrapper {
    val channels = mutableMapOf<String, RssChannel>()
    val httpErrors = mutableMapOf<String, Int>()
    val requestedUrls = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null
    private var challengeAll = false
    private var retryAfter: String? = null

    fun challengeEverything(retryAfter: String?) {
        challengeAll = true
        this.retryAfter = retryAfter
    }

    override suspend fun getRssChannel(url: String, allowBrowserTier: Boolean): RssChannel {
        requestedUrls += url
        gate?.await()
        channels[url]?.let { return it }
        throw when {
            url in httpErrors -> HttpException(code = httpErrors.getValue(url), message = "Refused")
            challengeAll -> FeedVerificationRequired(FeedChallenge(url, "$url?challenged", 403, retryAfter))
            else -> HttpException(code = 404, message = "Not found")
        }
    }
}
