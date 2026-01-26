package com.prof18.feedflow.feedsync.test.feedbin

import io.ktor.http.HttpStatusCode

/**
 * Configures Feedbin-specific mock responses based on actual API responses.
 * These responses are based on captured Feedbin API responses from a real sync operation.
 */
fun FeedbinMockEngineBuilder.configureFeedbinMocks() {
    // Authentication success - returns empty JSON object
    addMockResponse(
        urlPattern = "/v2/authentication.json",
        method = "GET",
        responseFile = "auth_success.json",
        statusCode = HttpStatusCode.OK,
    )

    // Icons list
    addMockResponse(
        urlPattern = "/v2/icons.json",
        method = "GET",
        responseFile = "icons_list.json",
    )

    // Taggings list
    addMockResponse(
        urlPattern = "/v2/taggings.json",
        method = "GET",
        responseFile = "taggings_list.json",
    )

    // Subscriptions list
    addMockResponse(
        urlPattern = "/v2/subscriptions.json",
        method = "GET",
        responseFile = "subscriptions_list.json",
    )

    // Starred entries (list of entry IDs)
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "GET",
        responseFile = "starred_entries.json",
    )

    // Unread entries (list of entry IDs)
    addMockResponse(
        urlPattern = "/v2/unread_entries.json",
        method = "GET",
        responseFile = "unread_entries.json",
    )

    // Entries with IDs (first page)
    addMockResponse(
        urlPattern = "/v2/entries.json",
        method = "GET",
        responseFile = "entries_page1.json",
    )

    // Create subscription success
    addMockResponse(
        urlPattern = "/v2/subscriptions.json",
        method = "POST",
        responseContent = """{"id":9115993,"created_at":"2025-12-31T11:38:59.810691Z",""" +
            """"feed_id":1240842,"title":"Test Feed",""" +
            """"feed_url":"https://example.com/feed","site_url":"https://example.com"}""",
    )

    // Delete subscription success (returns empty)
    addMockResponse(
        urlPattern = "/v2/subscriptions/",
        method = "DELETE",
        responseContent = "",
        statusCode = HttpStatusCode.NoContent,
    )

    // Update subscription success
    addMockResponse(
        urlPattern = "/v2/subscriptions/",
        method = "PATCH",
        responseContent = """{"id":9115993,"created_at":"2025-12-31T11:38:59.810691Z",""" +
            """"feed_id":1240842,"title":"Updated Feed",""" +
            """"feed_url":"https://example.com/feed","site_url":"https://example.com"}""",
    )

    // Mark as unread success
    addMockResponse(
        urlPattern = "/v2/unread_entries.json",
        method = "POST",
        responseContent = """[5031084432,5050623384]""",
    )

    // Mark as read success (returns empty)
    addMockResponse(
        urlPattern = "/v2/unread_entries.json",
        method = "DELETE",
        responseContent = "",
        statusCode = HttpStatusCode.OK,
    )

    // Star entries success
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "POST",
        responseContent = """[5031084432,5050623384]""",
    )

    // Unstar entries success
    addMockResponse(
        urlPattern = "/v2/starred_entries.json",
        method = "DELETE",
        responseContent = "",
        statusCode = HttpStatusCode.OK,
    )

    // Create tagging success
    addMockResponse(
        urlPattern = "/v2/taggings.json",
        method = "POST",
        responseContent = """{"id":8467006,"feed_id":1820404,"name":"Test Tag"}""",
    )

    // Delete tagging success (returns empty)
    addMockResponse(
        urlPattern = "/v2/taggings/",
        method = "DELETE",
        responseContent = "",
        statusCode = HttpStatusCode.NoContent,
    )

    // Rename tag success
    addMockResponse(
        urlPattern = "/v2/tags.json",
        method = "POST",
        responseContent = """[{"id":8467006,"feed_id":1820404,"name":"Renamed Tag"}]""",
    )

    // Delete tag success
    addMockResponse(
        urlPattern = "/v2/tags.json",
        method = "DELETE",
        responseContent = """[]""",
    )
}

/**
 * Configures Feedbin mocks with login failure (401 Unauthorized).
 */
fun FeedbinMockEngineBuilder.configureFeedbinMocksWithLoginFailure() {
    addMockResponse(
        urlPattern = "/v2/authentication.json",
        method = "GET",
        statusCode = HttpStatusCode.Unauthorized,
        responseContent = """{}""",
    )
}

/**
 * Configures Feedbin mocks with successful login but sync failure.
 * Login succeeds, but subscription list returns an error.
 */
fun FeedbinMockEngineBuilder.configureFeedbinMocksWithSyncFailure() {
    // Auth succeeds
    addMockResponse(
        urlPattern = "/v2/authentication.json",
        method = "GET",
        responseFile = "auth_success.json",
        statusCode = HttpStatusCode.OK,
    )
    // But subscriptions fail
    addMockResponse(
        urlPattern = "/v2/subscriptions.json",
        method = "GET",
        statusCode = HttpStatusCode.InternalServerError,
        responseContent = """{"error": "Internal Server Error"}""",
    )
}
