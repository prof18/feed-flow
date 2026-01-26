package com.prof18.feedflow.feedsync.test.greader

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * Configures Miniflux-specific mock responses for GReader API.
 * These responses are based on actual Miniflux API responses.
 */
fun GReaderMockEngineBuilder.configureMinifluxMocks() {
    // Login success - Miniflux format: SID=username/hash, LSID=username/hash, Auth=username/hash
    addMockResponse(
        urlPattern = "/accounts/ClientLogin",
        method = "POST",
        responseFile = "login_success.txt",
        statusCode = HttpStatusCode.OK,
        headers = mapOf(HttpHeaders.ContentType to "text/plain"),
    )

    // Token
    addMockResponse(
        urlPattern = "/reader/api/0/token",
        method = "GET",
        responseFile = "token.txt",
    )

    // Subscription list
    addMockResponse(
        urlPattern = "/reader/api/0/subscription/list",
        method = "GET",
        responseFile = "subscriptions_list.json",
    )

    // Unread items IDs - first page
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/ids",
        method = "GET",
        responseFile = "stream_items_ids_unread.json",
    )

    // Unread items IDs - second page (with continuation)
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/ids",
        method = "GET",
        responseFile = "stream_items_ids_unread_page2.json",
    )

    // Starred items IDs
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/ids",
        method = "GET",
        responseFile = "stream_items_ids_starred.json",
    )

    // Stream contents - first page (POST method)
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/contents",
        method = "POST",
        responseFile = "stream_contents_page1.json",
    )

    // Stream contents - second page (POST method)
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/contents",
        method = "POST",
        responseFile = "stream_contents_page2.json",
    )

    // Starred items contents (POST method)
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/contents",
        method = "POST",
        responseFile = "stream_contents_starred.json",
    )

    // Edit tag success
    addMockResponse(
        urlPattern = "/reader/api/0/edit-tag",
        method = "POST",
        responseContent = "OK",
    )

    // Subscription edit success
    addMockResponse(
        urlPattern = "/reader/api/0/subscription/edit",
        method = "POST",
        responseContent = "OK",
    )

    // Subscription quickadd success
    addMockResponse(
        urlPattern = "/reader/api/0/subscription/quickadd",
        method = "POST",
        responseContent = """{"numResults":1,"streamId":"feed/http://example.com/feed","query":"http://example.com/feed"}""",
    )

    // Rename tag success
    addMockResponse(
        urlPattern = "/reader/api/0/rename-tag",
        method = "POST",
        responseContent = "OK",
    )
}

/**
 * Configures Miniflux mocks with login failure (401 Unauthorized).
 */
fun GReaderMockEngineBuilder.configureMinifluxMocksWithLoginFailure() {
    addMockResponse(
        urlPattern = "/accounts/ClientLogin",
        method = "POST",
        statusCode = HttpStatusCode.Unauthorized,
        responseContent = "Error=BadAuthentication",
        headers = mapOf(HttpHeaders.ContentType to "text/plain"),
    )
}

/**
 * Configures Miniflux mocks with successful login but sync failure.
 * Login succeeds, but subscription list returns an error.
 */
fun GReaderMockEngineBuilder.configureMinifluxMocksWithSyncFailure() {
    // Login success
    addMockResponse(
        urlPattern = "/accounts/ClientLogin",
        method = "POST",
        responseFile = "login_success.txt",
        statusCode = HttpStatusCode.OK,
        headers = mapOf(HttpHeaders.ContentType to "text/plain"),
    )

    // Token
    addMockResponse(
        urlPattern = "/reader/api/0/token",
        method = "GET",
        responseFile = "token.txt",
    )

    // Subscription list fails with server error
    addMockResponse(
        urlPattern = "/reader/api/0/subscription/list",
        method = "GET",
        statusCode = HttpStatusCode.InternalServerError,
        responseContent = """{"error": "Internal Server Error"}""",
    )
}
