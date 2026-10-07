package com.prof18.feedflow.feedsync.feedbin.data

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbinClientAccountTest {

    @Test
    fun `requests use the current credentials after login and account change`() = runBlocking {
        val requests = mutableListOf<Pair<String, String?>>()
        val engine = MockEngine { request ->
            requests += request.url.encodedPath to request.headers[HttpHeaders.Authorization]
            val content = if (request.url.encodedPath.endsWith("unread_entries.json")) "[]" else ""
            respond(content, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val settings = NetworkSettings(MapSettings()).apply {
            setSyncUsername("user-a")
            setSyncPwd("password-a")
        }
        val client = FeedbinClient(
            logger = Logger.withTag("FeedbinClientAccountTest"),
            networkSettings = settings,
            appEnvironment = AppEnvironment.Release,
            dispatcherProvider = TestDispatchers,
            appVersion = "test",
            httpClientEngine = engine,
        )

        client.getUnreadEntries()
        settings.setSyncUsername("user-b")
        settings.setSyncPwd("password-b")
        client.getUnreadEntries()
        client.login("user-c", "password-c")
        settings.setSyncUsername("user-c")
        settings.setSyncPwd("password-c")
        client.getUnreadEntries()
        settings.setSyncPwd("password-c-updated")
        client.getUnreadEntries()

        assertEquals(
            listOf(
                "/v2/unread_entries.json" to "Basic dXNlci1hOnBhc3N3b3JkLWE=",
                "/v2/unread_entries.json" to "Basic dXNlci1iOnBhc3N3b3JkLWI=",
                "/v2/unread_entries.json" to "Basic dXNlci1jOnBhc3N3b3JkLWM=",
                "/v2/unread_entries.json" to "Basic dXNlci1jOnBhc3N3b3JkLWMtdXBkYXRlZA==",
            ),
            requests.filter { it.first.endsWith("unread_entries.json") },
        )
    }

    private object TestDispatchers : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
    }
}
