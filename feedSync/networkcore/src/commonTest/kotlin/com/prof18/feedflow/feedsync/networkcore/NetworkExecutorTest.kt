package com.prof18.feedflow.feedsync.networkcore

import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.core.model.Unhandled
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkExecutorTest {

    @Test
    fun `executeNetwork returns success when response is ok`() = runTest {
        val client = buildClient(
            status = HttpStatusCode.OK,
            body = "payload",
        )

        val result = executeNetwork<String> { client.get("https://example.com") }

        assertEquals(DataResult.Success("payload"), result)
        client.close()
    }

    @Test
    fun `executeNetwork returns bad token when header is set`() = runTest {
        val client = buildClient(
            status = HttpStatusCode.OK,
            body = "payload",
            headers = headersOf("X-Reader-Google-Bad-Token", "true"),
        )

        val result = executeNetwork<String> { client.get("https://example.com") }

        assertEquals(DataResult.Error(NetworkFailure.BadToken), result)
        client.close()
    }

    @Test
    fun `executeNetwork maps unauthorized to Unauthorised failure`() = runTest {
        val client = buildClient(status = HttpStatusCode.Unauthorized, body = "")

        val result = executeNetwork<String> { client.get("https://example.com") }

        assertEquals(DataResult.Error(NetworkFailure.Unauthorised), result)
        client.close()
    }

    @Test
    fun `executeNetwork maps not found to DataNotFound`() = runTest {
        val client = buildClient(status = HttpStatusCode.NotFound, body = "")

        val result = executeNetwork<String> { client.get("https://example.com") }

        assertEquals(DataResult.Error(DataNotFound), result)
        client.close()
    }

    @Test
    fun `executeNetwork maps server errors to ServerFailure`() = runTest {
        val client = buildClient(status = HttpStatusCode.ServiceUnavailable, body = "")

        val result = executeNetwork<String> { client.get("https://example.com") }

        assertEquals(DataResult.Error(NetworkFailure.ServerFailure), result)
        client.close()
    }

    @Test
    fun `executeNetwork returns success for empty body`() = runTest {
        val client = buildClient(
            status = HttpStatusCode.NoContent,
            body = "",
        )

        val result = executeNetwork<String> { client.get("https://example.com") }

        assertEquals(DataResult.Success(""), result)
        client.close()
    }

    @Test
    fun `executeNetwork wraps unexpected exceptions as Unhandled`() = runTest {
        val failure = IllegalStateException("boom")

        val result = executeNetwork<String> { throw failure }

        val error = result as DataResult.Error
        assertEquals(Unhandled(failure), error.failure)
    }

    private fun buildClient(
        status: HttpStatusCode,
        body: String,
        headers: io.ktor.http.Headers = headersOf(HttpHeaders.ContentType, "text/plain"),
    ): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(
                    content = ByteReadChannel(body),
                    status = status,
                    headers = headers,
                )
            }
        }
    }
}
