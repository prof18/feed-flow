package com.prof18.feedflow.feedsync.greader.data

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.feedsync.greader.domain.Stream
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class GReaderClientAccountTest {

    @Test
    fun `requests and post tokens follow the account after login and settings change`() = runBlocking {
        val requests = mutableListOf<Pair<String, String?>>()
        val postTokens = mutableListOf<String?>()
        val engine = MockEngine { request ->
            requests += request.url.toString() to request.headers[HttpHeaders.Authorization]
            if (request.url.encodedPath.endsWith("/edit-tag")) {
                postTokens += (request.body as FormDataContent).formData["T"]
            }
            val content = when {
                request.url.encodedPath.endsWith("/token") ->
                    "post-${request.headers[HttpHeaders.Authorization]?.substringAfter("auth=")}"
                request.url.encodedPath.endsWith("/ClientLogin") -> "Auth=new-token"
                request.url.encodedPath.endsWith("/list") -> """{"subscriptions":[]}"""
                else -> ""
            }
            respond(content, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val settings = NetworkSettings(MapSettings()).apply {
            setSyncUrl("https://account-a.example/")
            setSyncPwd("token-a")
        }
        val client = GReaderClient(
            logger = Logger.withTag("GReaderClientAccountTest"),
            networkSettings = settings,
            appEnvironment = AppEnvironment.Release,
            dispatcherProvider = TestDispatchers,
            appVersion = "test",
            httpClientEngine = engine,
        )

        client.getFeedSourcesAndCategories()
        client.editTag(itemIds = listOf("item-a"), addTag = Stream.Starred())
        settings.setSyncUrl("https://account-b.example/")
        settings.setSyncPwd("token-b")
        client.editTag(itemIds = listOf("item-b"), addTag = Stream.Starred())
        client.getFeedSourcesAndCategories()
        settings.setSyncPwd("token-b-rotated")
        client.editTag(itemIds = listOf("item-b-rotated"), addTag = Stream.Starred())
        client.getFeedSourcesAndCategories()
        client.login("user-c", "password-c", "https://account-c.example/")
        settings.setSyncUrl("https://account-c.example/")
        settings.setSyncPwd("token-c")
        client.getFeedSourcesAndCategories()

        val subscriptionRequests = requests.filter { it.first.contains("/subscription/list") }
            .map { io.ktor.http.Url(it.first).host to it.second }
        assertEquals(
            listOf(
                "account-a.example" to "GoogleLogin auth=token-a",
                "account-b.example" to "GoogleLogin auth=token-b",
                "account-b.example" to "GoogleLogin auth=token-b-rotated",
                "account-c.example" to "GoogleLogin auth=token-c",
            ),
            subscriptionRequests,
        )
        assertEquals(
            listOf("account-a.example", "account-b.example", "account-b.example"),
            requests.filter { it.first.endsWith("/token") }.map { io.ktor.http.Url(it.first).host },
        )
        assertEquals(
            listOf<String?>("post-token-a", "post-token-b", "post-token-b-rotated"),
            postTokens,
        )
        assertEquals(
            listOf("account-c.example" to null),
            requests.filter { io.ktor.http.Url(it.first).encodedPath.endsWith("/ClientLogin") }
                .map { io.ktor.http.Url(it.first).host to it.second },
        )
    }

    private object TestDispatchers : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
    }
}
