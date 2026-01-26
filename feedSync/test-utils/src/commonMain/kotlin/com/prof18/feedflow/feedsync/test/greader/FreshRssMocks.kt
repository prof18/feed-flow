package com.prof18.feedflow.feedsync.test.greader

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * Configures FreshRSS-specific mock responses for GReader API.
 * These responses are based on actual FreshRSS API responses.
 */
fun GReaderMockEngineBuilder.configureFreshRssMocks() {
    // Login success - FreshRSS format: SID=username/hash, LSID=null, Auth=username/hash
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

    // Reading list contents - first page
    addMockResponse(
        urlPattern = "/reader/api/0/stream/contents/user/-/state/com.google/reading-list",
        method = "GET",
        responseFile = "stream_contents_reading_list_page1.json",
    )

    // Reading list contents - second page (with continuation)
    addMockResponse(
        urlPattern = "/reader/api/0/stream/contents/user/-/state/com.google/reading-list",
        method = "GET",
        responseFile = "stream_contents_reading_list_page2.json",
    )

    // Starred items contents
    addMockResponse(
        urlPattern = "/reader/api/0/stream/contents/user/-/state/com.google/starred",
        method = "GET",
        responseFile = "stream_contents_starred.json",
    )

    // Unread items IDs
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/ids",
        method = "GET",
        responseFile = "stream_items_ids_unread.json",
    )

    // Starred items IDs
    addMockResponse(
        urlPattern = "/reader/api/0/stream/items/ids",
        method = "GET",
        responseFile = "stream_items_ids_starred.json",
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
 * Configures FreshRSS mocks with login failure (401 Unauthorized).
 */
fun GReaderMockEngineBuilder.configureFreshRssMocksWithLoginFailure() {
    addMockResponse(
        urlPattern = "/accounts/ClientLogin",
        method = "POST",
        statusCode = HttpStatusCode.Unauthorized,
        responseContent = "Error=BadAuthentication",
        headers = mapOf(HttpHeaders.ContentType to "text/plain"),
    )
}

/**
 * Configures FreshRSS mocks with successful login but sync failure.
 * Login succeeds, but subscription list returns an error.
 */
fun GReaderMockEngineBuilder.configureFreshRssMocksWithSyncFailure() {
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
