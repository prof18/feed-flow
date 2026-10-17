package com.prof18.feedflow.shared.e2e

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArticleReportHttpClientTest : KoinTestBase() {
    @Test
    fun `release environment ignores persisted fake mode and returns live client`() = runTest(testDispatcher) {
        val settings = MapSettings().apply {
            putString(RESPONSE_KEY, "accepted")
        }
        var liveRequests = 0
        val live = liveClient {
            liveRequests++
            respond("", HttpStatusCode.ServiceUnavailable)
        }

        val client = articleReportHttpClient(AppEnvironment.Release, settings, live)
        assertSame(live, client)
        assertEquals(HttpStatusCode.ServiceUnavailable, client.post(TEST_URL).status)
        assertEquals(1, liveRequests)
        client.close()
    }

    @Test
    fun `debug modes return deterministic responses without reaching live client`() = runTest(testDispatcher) {
        val settings = MapSettings()
        var liveRequests = 0
        val live = liveClient {
            liveRequests++
            respond("", HttpStatusCode.OK)
        }
        val client = articleReportHttpClient(AppEnvironment.Debug, settings, live)

        selectArticleReportResponse(settings, "accepted")
        assertEquals(HttpStatusCode.OK, client.post(TEST_URL).status)

        selectArticleReportResponse(settings, "terminal")
        assertEquals(HttpStatusCode.BadRequest, client.post(TEST_URL).status)

        selectArticleReportResponse(settings, "rate-limit")
        assertEquals(HttpStatusCode.TooManyRequests, client.post(TEST_URL).status)

        selectArticleReportResponse(settings, "offline")
        var failed = false
        try {
            client.post(TEST_URL)
        } catch (_: Exception) {
            failed = true
        }
        assertTrue(failed)

        assertEquals(0, liveRequests)
        client.close()
    }

    @Test
    fun `clearing fake mode removes persisted choice and delegates to live client`() = runTest(testDispatcher) {
        val settings = MapSettings()
        var liveRequests = 0
        val live = liveClient {
            liveRequests++
            respond("live", HttpStatusCode.Accepted)
        }
        val client = articleReportHttpClient(AppEnvironment.Debug, settings, live)
        selectArticleReportResponse(settings, "offline")

        selectArticleReportResponse(settings, null)

        assertFalse(settings.hasKey(RESPONSE_KEY))
        assertEquals(HttpStatusCode.Accepted, client.post(TEST_URL).status)
        assertEquals(1, liveRequests)
        client.close()
    }

    private fun liveClient(handler: io.ktor.client.engine.mock.MockRequestHandler): HttpClient = HttpClient(
        MockEngine(
            MockEngineConfig().apply {
                dispatcher = testDispatcher
                addHandler(handler)
            },
        ),
    )

    private companion object {
        const val RESPONSE_KEY = "article-report-debug-transport-v1"
        const val TEST_URL = "https://reports.example.com/v1/article-reports"
    }
}
