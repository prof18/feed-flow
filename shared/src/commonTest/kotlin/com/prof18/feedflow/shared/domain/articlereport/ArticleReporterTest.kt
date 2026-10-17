package com.prof18.feedflow.shared.domain.articlereport

import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArticleReporterTest : KoinTestBase() {
    @Test
    fun `reporter is shared by Koin callers`() {
        assertSame(getKoin().get<ArticleReporter>(), getKoin().get<ArticleReporter>())
    }

    @Test
    fun `submit sends the provided article and accepts any successful response`() = runTest(testDispatcher) {
        var payload: Map<String, JsonElement>? = null
        var sent: Boolean? = null
        val fixture = fixture { request ->
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            payload = Json.parseToJsonElement(body).jsonObject
            respond("malformed acknowledgement", HttpStatusCode.Accepted)
        }

        fixture.reporter.submit("https://example.com/a?q=1#keep", "web") { sent = it }
        runCurrent()

        val actual = assertNotNull(payload)
        assertEquals(
            setOf(
                "schemaVersion", "reportId", "url", "app", "appVersion", "platform", "osVersion",
                "parserVersion", "contentSource", "reportedAt",
            ),
            actual.keys,
        )
        assertEquals("https://example.com/a?q=1#keep", actual.getValue("url").jsonPrimitive.content)
        assertEquals("feedflow", actual.getValue("app").jsonPrimitive.content)
        assertEquals(TestModules.testAppConfig.appVersion, actual.getValue("appVersion").jsonPrimitive.content)
        assertEquals("android", actual.getValue("platform").jsonPrimitive.content)
        assertEquals(TestModules.testAppConfig.platformVersion, actual.getValue("osVersion").jsonPrimitive.content)
        assertEquals("web", actual.getValue("contentSource").jsonPrimitive.content)
        assertTrue(actual.getValue("reportId").jsonPrimitive.content.isNotBlank())
        assertTrue(actual.getValue("parserVersion").jsonPrimitive.content.isNotBlank())
        assertTrue(actual.getValue("reportedAt").jsonPrimitive.content.isNotBlank())
        assertEquals(true, sent)
        fixture.close()
    }

    @Test
    fun `failed request makes one attempt and returns false`() = runTest(testDispatcher) {
        var requests = 0
        var sent: Boolean? = null
        val fixture = fixture {
            requests++
            respond("", HttpStatusCode.ServiceUnavailable)
        }
        fixture.reporter.submit("https://example.com/article", "feed") { sent = it }
        runCurrent()

        assertEquals(1, requests)
        assertEquals(false, sent)
        fixture.close()
    }

    @Test
    fun `network exception returns false`() = runTest(testDispatcher) {
        var sent: Boolean? = null
        val fixture = fixture { error("Offline") }
        fixture.reporter.submit("https://example.com/article", "web") { sent = it }
        runCurrent()

        assertEquals(false, sent)
        fixture.close()
    }

    @Test
    fun `report completes after its caller is cancelled`() = runTest(testDispatcher) {
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<Unit>()
        var result: Boolean? = null
        val fixture = fixture {
            started.complete(Unit)
            response.await()
            respond("", HttpStatusCode.NoContent)
        }
        val caller = launch {
            fixture.reporter.submit("https://example.com/article", "web") { result = it }
            awaitCancellation()
        }
        runCurrent()
        assertTrue(started.isCompleted)
        caller.cancel()
        runCurrent()

        assertNull(result)
        response.complete(Unit)
        runCurrent()
        assertEquals(true, result)
        fixture.close()
    }

    @Test
    fun `closing reporter cancels its request without calling completion`() = runTest(testDispatcher) {
        val cancelled = CompletableDeferred<Unit>()
        var result: Boolean? = null
        val fixture = fixture {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        fixture.reporter.submit("https://example.com/article", "web") { result = it }
        runCurrent()
        fixture.reporter.close()
        runCurrent()

        assertTrue(cancelled.isCompleted)
        assertNull(result)
        fixture.close()
    }

    private fun fixture(handler: MockRequestHandler): ReporterFixture {
        val engine = MockEngine(
            MockEngineConfig().apply {
                dispatcher = testDispatcher
                addHandler { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    handler(request)
                }
            },
        )
        val client = ArticleReporter.httpClient(engine)
        return ReporterFixture(
            reporter = ArticleReporter(
                client = client,
                appConfig = TestModules.testAppConfig.copy(platformName = "Android"),
                dispatcherProvider = TestDispatcherProvider,
            ),
            client = client,
        )
    }

    private data class ReporterFixture(val reporter: ArticleReporter, private val client: HttpClient) {
        fun close() {
            reporter.close()
            client.close()
        }
    }
}
