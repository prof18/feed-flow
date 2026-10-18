package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feeditem.ArticleContentParser
import com.prof18.feedflow.shared.domain.feeditem.ArticleParseResult
import com.prof18.feedflow.shared.domain.recovery.FeedChallenge
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.fakes.FakeVerificationBrowser
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.koin.RecoveryTestModules
import com.prof18.feedflow.shared.test.toParsedFeedSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ReaderModeRecoveryTest : KoinTestBase() {

    private val challengedUrls = mutableMapOf<String, String?>()
    private val sessionHttp = FakeWebsiteSessionHttp()

    private val viewModel: ReaderModeViewModel by inject()
    private val databaseHelper: DatabaseHelper by inject()
    private val coordinator: FeedRecoveryCoordinator by inject()
    private val recoveryRepository: FeedRecoveryRepository by inject()
    private val browser by lazy {
        FakeVerificationBrowser(coordinator, recoveryRepository).apply {
            bytes = VERIFIED.encodeToByteArray()
        }
    }

    override fun getTestModules(): List<Module> =
        super.getTestModules() +
            RecoveryTestModules.enabled(sessionHttp, null) +
            module { single<ArticleContentParser> { ChallengingParser(challengedUrls) } }

    @Test
    fun `a challenged article shows the feed fallback and offers verification`() = runTest(testDispatcher) {
        val item = seedItem("challenged")
        challengedUrls[item.url] = null

        viewModel.loadReaderContent(item.urlInfo())
        advanceUntilIdle()

        val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(ShownContentSource.FEED, data.shownContentSource)
        assertEquals(item.url, viewModel.verificationState.value.verificationUrl)
    }

    @Test
    fun `verifying replaces the fallback with the article`() = runTest(testDispatcher) {
        browser.start(backgroundScope)
        val item = seedItem("verified")
        challengedUrls[item.url] = null
        viewModel.loadReaderContent(item.urlInfo())
        advanceUntilIdle()

        viewModel.verifyWebsite()
        val data = awaitWebContent()

        assertEquals(VERIFIED, data.content)
        assertNull(viewModel.verificationState.value.verificationUrl)
    }

    @Test
    fun `opening another article clears the previous verification and its backoff`() = runTest(testDispatcher) {
        val first = seedItem("first")
        val second = seedItem("second")
        challengedUrls[first.url] = "3600"
        viewModel.loadReaderContent(first.urlInfo())
        advanceUntilIdle()
        assertEquals(first.id, viewModel.verificationState.value.articleId)

        viewModel.loadReaderContent(second.urlInfo())
        advanceUntilIdle()

        assertEquals(ReaderVerificationState(), viewModel.verificationState.value)
        val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(second.id, data.id.id)
        assertEquals(ShownContentSource.WEB, data.shownContentSource)
    }

    @Test
    fun `a later successful load clears the verification of the same article`() = runTest(testDispatcher) {
        val item = seedItem("lifted")
        challengedUrls[item.url] = null
        viewModel.loadReaderContent(item.urlInfo())
        advanceUntilIdle()
        assertEquals(item.url, viewModel.verificationState.value.verificationUrl)

        challengedUrls.remove(item.url)
        viewModel.toggleContentSource()
        advanceUntilIdle()

        val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(ShownContentSource.WEB, data.shownContentSource)
        assertNull(viewModel.verificationState.value.verificationUrl)
        assertEquals(ReaderVerificationState(articleId = item.id), viewModel.verificationState.value)
    }

    @Test
    fun `cancelling verification keeps the selected article`() = runTest(testDispatcher) {
        browser.respond = false
        browser.start(backgroundScope)
        val item = seedItem("cancel")
        challengedUrls[item.url] = null
        viewModel.loadReaderContent(item.urlInfo())
        advanceUntilIdle()
        viewModel.verifyWebsite()
        coordinator.visibleState.first { it != null }

        viewModel.cancelVerification()
        coordinator.visibleState.first { it == null }

        assertEquals(item.id, viewModel.currentArticleState.value?.id)
        assertIs<ReaderModeState.Success>(viewModel.readerModeState.value)
    }

    @Test
    fun `switching article during verification never shows the old article`() = runTest(testDispatcher) {
        browser.respond = false
        browser.start(backgroundScope)
        val first = seedItem("first")
        val second = seedItem("second")
        challengedUrls[first.url] = null
        viewModel.loadReaderContent(first.urlInfo())
        advanceUntilIdle()
        viewModel.verifyWebsite()
        coordinator.visibleState.first { it != null }

        viewModel.loadReaderContent(second.urlInfo())
        coordinator.visibleState.first { it == null }
        advanceUntilIdle()

        val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(second.id, data.id.id)
    }

    @Test
    fun `a normal article never touches the coordinator`() = runTest(testDispatcher) {
        browser.start(backgroundScope)
        val item = seedItem("normal")

        viewModel.loadReaderContent(item.urlInfo())
        advanceUntilIdle()

        val data = assertIs<ReaderModeState.Success>(viewModel.readerModeState.value).readerModeData
        assertEquals(ShownContentSource.WEB, data.shownContentSource)
        assertEquals(ReaderVerificationState(), viewModel.verificationState.value)
        assertEquals(0, browser.shownRequests.size)
        assertEquals(0, sessionHttp.requests.size)
    }

    private suspend fun TestScope.awaitWebContent() =
        viewModel.readerModeState.first {
            (it as? ReaderModeState.Success)?.readerModeData?.shownContentSource == ShownContentSource.WEB
        }.let { (it as ReaderModeState.Success).readerModeData }

    private suspend fun seedItem(id: String): FeedItem {
        val source = FeedSourceGenerator.feedSource(
            id = "source-$id",
            url = "https://reader.test/$id/feed.xml",
            websiteUrl = "https://reader.test",
        )
        databaseHelper.insertFeedSource(listOf(source.toParsedFeedSource()))
        val item = FeedItem(
            id = id,
            url = "https://reader.test/articles/$id",
            title = "Title $id",
            subtitle = null,
            content = "<p>${"Feed copy of the article. ".repeat(10)}</p>",
            imageUrl = null,
            feedSource = source,
            pubDateMillis = 1000,
            isRead = false,
            dateString = null,
            commentsUrl = null,
            isBookmarked = false,
        )
        databaseHelper.insertFeedItems(listOf(item), lastSyncTimestamp = 0)
        return item
    }

    private fun FeedItem.urlInfo() = FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = false,
        articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
        commentsUrl = null,
    )

    private class ChallengingParser(private val challenged: Map<String, String?>) : ArticleContentParser {
        override suspend fun parse(url: String): String? = (parseResult(url) as? ArticleParseResult.Content)?.content

        override suspend fun parseResult(url: String): ArticleParseResult =
            if (url in challenged) {
                ArticleParseResult.Challenge(FeedChallenge(url, url, 403, challenged[url]))
            } else {
                ArticleParseResult.Content("Content-${url.substringAfterLast('/')}", url)
            }

        override suspend fun parseHtml(html: String, url: String): String = html

        override suspend fun prepareFeedContent(html: String, baseUrl: String?): String = html
    }

    private companion object {
        const val VERIFIED = "<html><body>Verified article</body></html>"
    }
}
