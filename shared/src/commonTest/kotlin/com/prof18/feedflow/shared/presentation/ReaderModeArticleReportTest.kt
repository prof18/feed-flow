package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ArticleReportMessage
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.shared.domain.articlereport.ArticleReporter
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReaderModeArticleReportTest : KoinTestBase() {
    private val viewModel: ReaderModeViewModel by inject()
    private val contentFiles: FeedItemContentFileHandler by inject()
    private val requests = mutableListOf<Map<String, JsonElement>>()
    private var status = HttpStatusCode.Accepted
    private var response: CompletableDeferred<Unit>? = null
    private val requestFinished = CompletableDeferred<Unit>()

    override fun getTestModules(): List<Module> = super.getTestModules() + module {
        single<HttpClient>(named("article-report")) {
            ArticleReporter.httpClient(
                MockEngine(
                    MockEngineConfig().apply {
                        dispatcher = testDispatcher
                        addHandler { request ->
                            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                            requests.add(Json.parseToJsonElement(body).jsonObject)
                            try {
                                response?.await()
                                respond("", status)
                            } finally {
                                requestFinished.complete(Unit)
                            }
                        }
                    },
                ),
            )
        } onClose { it?.close() }
    }

    @Test
    fun `cancel sends nothing and the next request asks again`() = runTest(testDispatcher) {
        openArticle("a")
        viewModel.requestArticleReport()
        assertTrue(viewModel.articleReportState.value.showDisclosure)
        viewModel.cancelArticleReport()
        runCurrent()

        assertFalse(viewModel.articleReportState.value.showDisclosure)
        assertTrue(requests.isEmpty())
        viewModel.confirmArticleReport()
        runCurrent()
        assertTrue(requests.isEmpty())
        viewModel.requestArticleReport()
        assertTrue(viewModel.articleReportState.value.showDisclosure)
    }

    @Test
    fun `loading reader does not submit an article`() = runTest(testDispatcher) {
        openArticle("a")
        viewModel.setLoading()
        runCurrent()
        viewModel.requestArticleReport()
        viewModel.confirmArticleReport()
        runCurrent()

        assertFalse(viewModel.articleReportState.value.canReport)
        assertFalse(viewModel.articleReportState.value.showDisclosure)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `send submits once and the next report requires confirmation again`() = runTest(testDispatcher) {
        response = CompletableDeferred()
        openArticle("a")
        viewModel.requestArticleReport()
        viewModel.confirmArticleReport()
        viewModel.confirmArticleReport()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals("https://example.com/a", requests.single().getValue("url").jsonPrimitive.content)
        assertEquals("web", requests.single().getValue("contentSource").jsonPrimitive.content)
        assertFalse(viewModel.articleReportState.value.canReport)
        assertEquals(ArticleReportMessage.SENDING, viewModel.articleReportState.value.message)
        response?.complete(Unit)
        runCurrent()

        assertEquals(ArticleReportMessage.SENT, viewModel.articleReportState.value.message)
        assertTrue(viewModel.articleReportState.value.canReport)
        viewModel.requestArticleReport()
        runCurrent()
        assertTrue(viewModel.articleReportState.value.showDisclosure)
        assertEquals(1, requests.size)
        viewModel.confirmArticleReport()
        runCurrent()
        assertEquals(2, requests.size)
    }

    @Test
    fun `failure requires another confirmation before retrying`() = runTest(testDispatcher) {
        status = HttpStatusCode.ServiceUnavailable
        openArticle("a")
        viewModel.requestArticleReport()
        viewModel.confirmArticleReport()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals(ArticleReportMessage.FAILED, viewModel.articleReportState.value.message)
        status = HttpStatusCode.Accepted
        viewModel.requestArticleReport()
        runCurrent()

        assertEquals(1, requests.size)
        assertTrue(viewModel.articleReportState.value.showDisclosure)
        viewModel.confirmArticleReport()
        runCurrent()
        assertEquals(2, requests.size)
        assertFalse(viewModel.articleReportState.value.showDisclosure)
        assertEquals(ArticleReportMessage.SENT, viewModel.articleReportState.value.message)
    }

    @Test
    fun `switching articles dismisses confirmation and sends only the article now open`() = runTest(testDispatcher) {
        openArticle("a")
        viewModel.requestArticleReport()
        openArticle("b")

        assertFalse(viewModel.articleReportState.value.showDisclosure)
        assertTrue(requests.isEmpty())
        viewModel.requestArticleReport()
        viewModel.confirmArticleReport()
        runCurrent()

        assertEquals("https://example.com/b", requests.single().getValue("url").jsonPrimitive.content)
    }

    @Test
    fun `completion does not show feedback on another article or after dismissal`() = runTest(testDispatcher) {
        response = CompletableDeferred()
        openArticle("a")
        viewModel.requestArticleReport()
        viewModel.confirmArticleReport()
        runCurrent()
        openArticle("b")
        response?.complete(Unit)
        runCurrent()

        assertNull(viewModel.articleReportState.value.message)
        response = CompletableDeferred()
        viewModel.requestArticleReport()
        viewModel.confirmArticleReport()
        runCurrent()
        viewModel.dismissArticleReportMessage()
        response?.complete(Unit)
        runCurrent()

        assertEquals(2, requests.size)
        assertNull(viewModel.articleReportState.value.message)
        assertFalse(viewModel.articleReportState.value.sending)
    }

    @Test
    fun `report continues when the reader scope is cancelled`() = runTest(testDispatcher) {
        response = CompletableDeferred()
        openArticle("a")
        viewModel.requestArticleReport()
        viewModel.confirmArticleReport()
        runCurrent()
        assertEquals(1, requests.size)
        assertFalse(requestFinished.isCompleted)

        viewModel.viewModelScope.cancel()
        runCurrent()

        assertFalse(requestFinished.isCompleted)
        response?.complete(Unit)
        runCurrent()

        assertTrue(requestFinished.isCompleted)
    }

    private suspend fun openArticle(id: String) {
        contentFiles.saveFeedItemContentToFile(id, "<p>Cached article</p>")
        viewModel.loadReaderContent(
            FeedItemUrlInfo(
                id = id,
                url = "https://example.com/$id",
                title = id,
                isBookmarked = false,
                articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
                commentsUrl = null,
            ),
        )
    }
}
