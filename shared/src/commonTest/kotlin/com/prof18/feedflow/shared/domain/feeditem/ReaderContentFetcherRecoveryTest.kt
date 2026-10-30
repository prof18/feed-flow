package com.prof18.feedflow.shared.domain.feeditem

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetrievalResult
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.ReaderFetchResult
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionFailure
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionHttp
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionRequest
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionResponse
import com.prof18.feedflow.shared.test.FeedItemContentFileHandlerTestImpl
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeVerificationBrowser
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.testLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ReaderContentFetcherRecoveryTest : KoinTestBase() {

    private val sessionHttp = FakeWebsiteSessionHttp()
    private val fileHandler = FeedItemContentFileHandlerTestImpl()
    private val primary = ScriptedPage()
    private val fallback = ScriptedPage()

    private val coordinator: FeedRecoveryCoordinator by inject()
    private val recoveryRepository: FeedRecoveryRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val dateFormatter: DateFormatter by inject()

    override fun getTestModules(): List<Module> =
        super.getTestModules() + RecoveryTestModules.enabled(sessionHttp, null)

    @Test
    fun `a normal article is fetched once without the session and saved as before`() = runTest(testDispatcher) {
        primary.html = ARTICLE

        val result = fetcher().fetchForReader(ARTICLE_ID, ARTICLE_URL, save = true, ticket())

        assertEquals(ReaderFetchResult.Content(ARTICLE, ARTICLE_URL), result)
        assertEquals(1, primary.requests)
        assertTrue(sessionHttp.requests.isEmpty())
        assertEquals(listOf(ARTICLE_ID), fileHandler.savedIds)
        assertTrue(fileHandler.stagedIds.isEmpty())
    }

    @Test
    fun `a plain forbidden article uses the fallback user agent without the session`() = runTest(testDispatcher) {
        primary.status = HttpStatusCode.Forbidden
        fallback.html = ARTICLE

        val result = fetcher().fetchForReader(ARTICLE_ID, ARTICLE_URL, save = false, ticket())

        assertIs<ReaderFetchResult.Content>(result)
        assertTrue(sessionHttp.requests.isEmpty())
    }

    @Test
    fun `a challenged article without a profile needs verification`() = runTest(testDispatcher) {
        challengeBoth(retryAfter = "120")
        val now = dateFormatter.currentTimeMillis()

        val result = assertIs<ReaderFetchResult.NeedsVerification>(
            fetcher().fetchForReader(ARTICLE_ID, ARTICLE_URL, save = false, ticket()),
        )

        assertEquals(ARTICLE_URL, result.verificationUrl)
        val blockedUntil = requireNotNull(result.blockedUntil)
        assertTrue(blockedUntil >= now + 119.seconds.inWholeMilliseconds)
        assertTrue(sessionHttp.requests.isEmpty())
    }

    @Test
    fun `a challenged article with a saved session uses it`() = runTest(testDispatcher) {
        challengeBoth(retryAfter = null)
        acquireProfile()
        sessionHttp.enqueue(sessionBody())

        val result = fetcher().fetchForReader(ARTICLE_ID, ARTICLE_URL, save = true, ticket())

        assertEquals(ReaderFetchResult.Content(ARTICLE, ARTICLE_URL), result)
        assertEquals(ARTICLE_URL, sessionHttp.requests.single().url)
        assertEquals(ARTICLE, fileHandler.loadFeedItemContent(ARTICLE_ID))
        assertEquals(listOf(ARTICLE_ID), fileHandler.stagedIds)
    }

    @Test
    fun `a failed session request keeps the anonymous challenge`() = runTest(testDispatcher) {
        challengeBoth(retryAfter = null)
        acquireProfile()
        sessionHttp.enqueue(WebsiteSessionResponse.Failure(WebsiteSessionFailure.Transport))

        val result = fetcher().fetchForReader(ARTICLE_ID, ARTICLE_URL, save = false, ticket())

        assertIs<ReaderFetchResult.NeedsVerification>(result)
    }

    @Test
    fun `without recovery support a challenge is unavailable`() = runTest(testDispatcher) {
        challengeBoth(retryAfter = null)
        val unsupported = FeedRecoveryRepository(databaseHelper, settingsRepository, supported = false)

        val result = fetcher(repository = unsupported)
            .fetchForReader(ARTICLE_ID, ARTICLE_URL, save = false, ticket())

        assertEquals(ReaderFetchResult.Unavailable, result)
    }

    @Test
    fun `a verification retains the profile for the reader and saves the article`() = runTest(testDispatcher) {
        val browser = FakeVerificationBrowser(coordinator, recoveryRepository)
        browser.bytes = ARTICLE.encodeToByteArray()
        browser.start(backgroundScope)

        val result = fetcher().verifyForReader(ARTICLE_ID, ARTICLE_URL, save = true, ticket())

        assertEquals(ReaderFetchResult.Content(ARTICLE, ARTICLE_URL), result)
        assertTrue(recoveryRepository.profiles().single().readerRetained)
        assertEquals(ARTICLE, fileHandler.loadFeedItemContent(ARTICLE_ID))
    }

    @Test
    fun `an account change during verification writes nothing`() = runTest(testDispatcher) {
        val browser = FakeVerificationBrowser(coordinator, recoveryRepository)
        browser.respond = false
        browser.start(backgroundScope)
        val verification = async {
            runCatching { fetcher().verifyForReader(ARTICLE_ID, ARTICLE_URL, save = true, ticket()) }
        }
        runCurrent()

        settingsRepository.rotateCloudSyncSession()
        runCurrent()

        assertTrue(verification.await().isFailure)
        assertNull(fileHandler.loadFeedItemContent(ARTICLE_ID))
        assertTrue(recoveryRepository.profiles().none { it.readerRetained })
    }

    @Test
    fun `a failed promotion reports the save failure`() = runTest(testDispatcher) {
        challengeBoth(retryAfter = null)
        acquireProfile()
        sessionHttp.enqueue(sessionBody())
        fileHandler.failPromotion = true

        val result = assertIs<ReaderFetchResult.Content>(
            fetcher().fetchForReader(ARTICLE_ID, ARTICLE_URL, save = true, ticket()),
        )

        assertTrue(result.saveFailed)
        assertFalse(fileHandler.isContentAvailable(ARTICLE_ID))
    }

    @Test
    fun `a challenged article with a throwing session needs verification`() = runTest(testDispatcher) {
        challengeBoth(retryAfter = null)
        acquireProfile()
        val throwing = object : WebsiteSessionHttp {
            override suspend fun fetch(request: WebsiteSessionRequest): WebsiteSessionResponse =
                error("profile store failed")
        }

        val result = fetcher(sessionHttp = throwing).fetchForReader(ARTICLE_ID, ARTICLE_URL, save = false, ticket())

        assertIs<ReaderFetchResult.NeedsVerification>(result)
    }

    private fun ticket() = recoveryRepository.issueTicket()

    private fun challengeBoth(retryAfter: String?) {
        primary.challenge(retryAfter)
        fallback.challenge(retryAfter)
    }

    private suspend fun acquireProfile(): FeedRecoveryProfile = recoveryRepository.acquireProfile(
        epoch = settingsRepository.cloudSyncSessionState.value,
        origin = requireNotNull(RecoveryOrigin.of(ARTICLE_URL)),
        userAgent = "ua",
        engineVersion = "engine",
    )

    private fun sessionBody() = WebsiteSessionResponse.Body(ARTICLE.encodeToByteArray(), "text/html", ARTICLE_URL)

    private fun fetcher(
        repository: FeedRecoveryRepository = recoveryRepository,
        sessionHttp: WebsiteSessionHttp = this.sessionHttp,
    ): ReaderContentFetcher {
        val retriever = HtmlRetriever(
            logger = testLogger,
            client = primary.client(),
            forbiddenFallbackClient = fallback.client(),
        )
        return ReaderContentFetcher(
            articleContentParser = RetrieverParser(retriever),
            feedItemContentFileHandler = fileHandler,
            htmlRetriever = retriever,
            recoveryRepository = repository,
            coordinator = coordinator,
            sessionHttp = sessionHttp,
            dateFormatter = dateFormatter,
        )
    }

    private suspend fun FeedRecoveryRepository.profiles() = databaseHelper.recoveryProfiles()

    private class ScriptedPage {
        var html: String = ""
        var status: HttpStatusCode = HttpStatusCode.OK
        var requests = 0
        private var challengeHeaders: Map<String, List<String>> = emptyMap()

        fun challenge(retryAfter: String?) {
            status = HttpStatusCode.Forbidden
            challengeHeaders = buildMap {
                put("cf-mitigated", listOf("challenge"))
                retryAfter?.let { put(HttpHeaders.RetryAfter, listOf(it)) }
            }
        }

        fun client() = HttpClient(MockEngine) {
            engine {
                addHandler {
                    requests++
                    respond(
                        content = html,
                        status = status,
                        headers = headersOf(
                            *(challengeHeaders + (HttpHeaders.ContentType to listOf("text/html")))
                                .toList()
                                .toTypedArray(),
                        ),
                    )
                }
            }
        }
    }

    private class RetrieverParser(private val retriever: HtmlRetriever) : ArticleContentParser {
        override suspend fun parse(url: String): String? = (parseResult(url) as? ArticleParseResult.Content)?.content

        override suspend fun parseResult(url: String): ArticleParseResult =
            when (val result = retriever.retrieveResult(url)) {
                is HtmlRetrievalResult.Content -> ArticleParseResult.Content(result.html, url)
                is HtmlRetrievalResult.Challenge -> ArticleParseResult.Challenge(result.challenge)
                HtmlRetrievalResult.Failed -> ArticleParseResult.Failed
            }

        override suspend fun parseHtml(html: String, url: String): String = html

        override suspend fun prepareFeedContent(html: String, baseUrl: String?): String = html
    }

    private companion object {
        const val ARTICLE_ID = "article-1"
        const val ARTICLE_URL = "https://reader.test/article"
        const val ARTICLE = "<html><body>Recovered article</body></html>"
    }
}
