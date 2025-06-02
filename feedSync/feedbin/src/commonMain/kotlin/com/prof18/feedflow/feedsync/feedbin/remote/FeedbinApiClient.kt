package com.prof18.feedflow.feedsync.feedbin.remote

import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinCreateSubscriptionRequest
import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinSubscriptionDTO
import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinUpdateStarredRequest
import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinUpdateUnreadRequest
// Import FeedbinEntryDTO if needed for body() calls, otherwise it's not directly used here
// import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinEntryDTO 
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class FeedbinApiClient(
    private val httpClient: HttpClient,
    private val usernameManager: () -> String?,
    private val passwordManager: () -> String?
) : FeedbinApi {

    private val baseUrl = "https://api.feedbin.com/v2"

    private fun HttpRequestBuilder.applyAuth() {
        val user = usernameManager()
        val pass = passwordManager()
        if (user != null && pass != null) {
            basicAuth(user, pass)
        } else {
            throw IllegalStateException("Feedbin API credentials not available.")
        }
    }

    override suspend fun getSubscriptions(): List<FeedbinSubscriptionDTO> {
        return httpClient.get("$baseUrl/subscriptions.json") {
            applyAuth()
        }.body()
    }

    override suspend fun createSubscription(request: FeedbinCreateSubscriptionRequest): HttpResponse {
        return httpClient.post("$baseUrl/subscriptions.json") {
            applyAuth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun getEntries(
        since: String?,
        page: Int?,
        ids: String?,
        read: Boolean?,
        starred: Boolean?,
        perPage: Int?,
        mode: String
    ): HttpResponse {
        return httpClient.get("$baseUrl/entries.json") {
            applyAuth()
            parameter("mode", mode)
            since?.let { parameter("since", it) }
            page?.let { parameter("page", it) }
            ids?.let { parameter("ids", it) }
            read?.let { parameter("read", it.toString()) } // Convert Boolean to String for query param
            starred?.let { parameter("starred", it.toString()) } // Convert Boolean to String for query param
            perPage?.let { parameter("per_page", it) }
        }
    }

    override suspend fun getFeedEntries(
        feedId: Long,
        page: Int?,
        perPage: Int?,
        mode: String
    ): HttpResponse {
        return httpClient.get("$baseUrl/feeds/$feedId/entries.json") {
            applyAuth()
            parameter("mode", mode)
            page?.let { parameter("page", it) }
            perPage?.let { parameter("per_page", it) }
        }
    }

    override suspend fun getUnreadEntryIds(): List<Long> {
        return httpClient.get("$baseUrl/unread_entries.json") {
            applyAuth()
        }.body()
    }

    override suspend fun markEntriesAsUnread(entryIds: List<Long>): List<Long> {
        return httpClient.post("$baseUrl/unread_entries.json") {
            applyAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbinUpdateUnreadRequest(unreadEntries = entryIds))
        }.body()
    }

    override suspend fun markEntriesAsRead(entryIds: List<Long>): List<Long> {
        return httpClient.post("$baseUrl/unread_entries/delete.json") { // Using POST alternative
            applyAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbinUpdateUnreadRequest(unreadEntries = entryIds))
        }.body()
    }

    override suspend fun getStarredEntryIds(): List<Long> {
        return httpClient.get("$baseUrl/starred_entries.json") {
            applyAuth()
        }.body()
    }

    override suspend fun starEntries(entryIds: List<Long>): List<Long> {
        return httpClient.post("$baseUrl/starred_entries.json") {
            applyAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbinUpdateStarredRequest(starredEntries = entryIds))
        }.body()
    }

    override suspend fun unstarEntries(entryIds: List<Long>): List<Long> {
        return httpClient.post("$baseUrl/starred_entries/delete.json") { // Using POST alternative
            applyAuth()
            contentType(ContentType.Application.Json)
            setBody(FeedbinUpdateStarredRequest(starredEntries = entryIds))
        }.body()
    }
}
