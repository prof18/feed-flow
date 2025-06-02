package com.prof18.feedflow.feedsync.feedbin.remote

import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinCreateSubscriptionRequest
import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinSubscriptionDTO
// Import FeedbinEntryDTO if it's returned directly by any method, otherwise HttpResponse is fine.
// import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinEntryDTO 
import io.ktor.client.statement.HttpResponse

interface FeedbinApi {
    suspend fun getSubscriptions(): List<FeedbinSubscriptionDTO>

    suspend fun createSubscription(request: FeedbinCreateSubscriptionRequest): HttpResponse

    suspend fun getEntries(
        since: String? = null,
        page: Int? = null,
        ids: String? = null, // Comma-separated string of Long IDs
        read: Boolean? = null,
        starred: Boolean? = null,
        perPage: Int? = null,
        mode: String = "extended"
    ): HttpResponse

    suspend fun getFeedEntries(
        feedId: Long,
        page: Int? = null,
        perPage: Int? = null,
        mode: String = "extended"
    ): HttpResponse

    suspend fun getUnreadEntryIds(): List<Long>
    suspend fun markEntriesAsUnread(entryIds: List<Long>): List<Long>
    suspend fun markEntriesAsRead(entryIds: List<Long>): List<Long>

    suspend fun getStarredEntryIds(): List<Long>
    suspend fun starEntries(entryIds: List<Long>): List<Long>
    suspend fun unstarEntries(entryIds: List<Long>): List<Long>
}
