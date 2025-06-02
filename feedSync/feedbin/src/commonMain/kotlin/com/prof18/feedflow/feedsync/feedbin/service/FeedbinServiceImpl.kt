package com.prof18.feedflow.feedsync.feedbin.service

import com.prof18.feedflow.core.model.FeedSource // Using FeedSource as per mapper
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.SyncResult // Assuming a common SyncResult type, may need to define or use DataResult
import com.prof18.feedflow.core.model.DataResult // Using DataResult as seen in GReader
import com.prof18.feedflow.core.model.Success
import com.prof18.feedflow.core.model.Error
import com.prof18.feedflow.core.model.NetworkErrorKind
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedSourceUrl // Wrapper for URL string
import com.prof18.feedflow.core.model.FeedSourceName // Wrapper for name string


// TODO: This service should ideally implement a common FeedSyncService interface if one exists.
// For now, structuring it based on the issue description and GReaderRepository's functionality.
// import com.prof18.feedflow.domain.feed.retriever.FeedSyncService 

import com.prof18.feedflow.feedsync.feedbin.remote.FeedbinApi
import com.prof18.feedflow.feedsync.feedbin.mapper.toDomainModel
import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinCreateSubscriptionRequest
import com.prof18.feedflow.feedsync.feedbin.remote.model.FeedbinEntryDTO
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import java.io.IOException // For general network issues, common on JVM/Android
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant // Ensure this is imported

// Temporary logger import, replace with Kermit or project's logger
// import co.touchlab.kermit.Logger


// Placeholder for FeedAccount if needed for multi-account, using feedAccountId: String for now
// import com.prof18.feedflow.core.model.FeedAccount


internal class FeedbinServiceImpl(
    private val feedbinApi: FeedbinApi
    // TODO: Inject database helper or settings repository if needed for 'since' token or other persistence
    // private val databaseHelper: DatabaseHelper, 
    // private val networkSettings: NetworkSettings, // For last sync date
    // private val logger: Logger // Project's logger
) { // Potential: : FeedSyncService 

    private val feedbinPerPageLimit = 100 // Default per Feedbin API

    // Mimicking GReaderRepository's login; actual auth happens in FeedbinApi via applyAuth()
    // This method could be used to make a test call to verify credentials.
    suspend fun authenticate(username: String, password: String): DataResult<Unit> {
        // In this design, FeedbinApi is already configured with credential providers.
        // A successful call to getSubscriptions would mean credentials are valid.
        return try {
            feedbinApi.getSubscriptions() // Test call
            // If successful, credentials are valid.
            // Username/password are not directly stored here but by platform-specific secure storage,
            // then provided to FeedbinApiClient via lambdas.
            DataResult.Success(Unit)
        } catch (e: Exception) {
            // TODO: Map Ktor exceptions (e.g., ResponseException) to specific NetworkFailure types
            // logger.e(e) { "Feedbin authentication failed" }
            DataResult.Error(NetworkErrorKind.Authentication) // Example error
        }
    }

    suspend fun getFeeds(feedAccountId: String): DataResult<List<FeedSource>> {
        return try {
            val dtos = feedbinApi.getSubscriptions()
            DataResult.Success(dtos.map { it.toDomainModel() })
        } catch (e: ClientRequestException) {
            // logger.e(e) { "Client error fetching Feedbin feeds: ${e.response.status}" }
            val errorKind = when (e.response.status) {
                HttpStatusCode.Unauthorized -> NetworkErrorKind.Authentication
                HttpStatusCode.Forbidden -> NetworkErrorKind.Forbidden
                HttpStatusCode.NotFound -> NetworkErrorKind.NotFound
                HttpStatusCode.TooManyRequests -> NetworkErrorKind.RateLimit
                else -> NetworkErrorKind.Client // Generic client error
            }
            DataResult.Error(errorKind, message = e.message)
        } catch (e: ServerResponseException) {
            // logger.e(e) { "Server error fetching Feedbin feeds: ${e.response.status}" }
            DataResult.Error(NetworkErrorKind.Server, message = e.message)
        } catch (e: IOException) { // General network issue (e.g. no connectivity)
            // logger.e(e) { "Network error fetching Feedbin feeds" }
            DataResult.Error(NetworkErrorKind.NetworkUnavailable, message = e.message)
        } catch (e: Exception) { // Catch-all for other unexpected errors
            // logger.e(e) { "Unexpected error fetching Feedbin feeds" }
            DataResult.Error(NetworkErrorKind.Unknown, message = e.message)
        }
    }

    suspend fun getFeedItems(
        feedAccountId: String, // Or use a FeedAccount object
        lastRefreshTimestamp: Long? // Corresponds to 'since'
    ): DataResult<List<FeedItem>> = coroutineScope { // Ensure coroutineScope is imported
        try {
            // Fetch unread and starred IDs concurrently
            val unreadIdsDeferred = async { feedbinApi.getUnreadEntryIds().toSet() }
            val starredIdsDeferred = async { feedbinApi.getStarredEntryIds().toSet() }

            val unreadEntryIds = unreadIdsDeferred.await()
            val starredEntryIds = starredIdsDeferred.await()

            val allFeedItems = mutableListOf<FeedItem>()
            var currentPage = 1
            val sinceIso: String? = lastRefreshTimestamp?.let { Instant.fromEpochMilliseconds(it).toString() }
            var morePages = true

            while (morePages) {
                val response = feedbinApi.getEntries(
                    since = sinceIso,
                    page = currentPage,
                    perPage = feedbinPerPageLimit
                )

                if (response.status != HttpStatusCode.OK) {
                    // logger.e { "Failed to fetch entries. Status: ${response.status}, Body: ${response.bodyAsText()}" }
                    // TODO: Handle specific errors (401, 429 etc.)
                    morePages = false // Stop pagination on error
                    if (allFeedItems.isEmpty()) { // If this is the first page and it failed
                         return@coroutineScope DataResult.Error(NetworkErrorKind.Unknown) // Or more specific error
                    }
                    break // Break from while loop, return what's been fetched so far or error
                }

                val entryDTOs: List<FeedbinEntryDTO> = response.body()
                if (entryDTOs.isEmpty()) {
                    morePages = false // No more entries
                    break
                }

                // TODO: This mapping of feedId to feedUrl is a placeholder.
                // Need a reliable way to get the feedUrl for each entry.
                // One option: fetch all subscriptions first and create a map of feedId -> feedUrl.
                // Or, if getFeedItems is called per-feed, feedUrl would be known.
                // For now, using a placeholder.
                val feedIdToUrlMap = mutableMapOf<Long, String>() // Populate this from getFeeds() or pass as param

                val domainItems = entryDTOs.map { dto ->
                    val parentFeedUrl = feedIdToUrlMap[dto.feedId] ?: "feedbin_feed_id_${dto.feedId}" // Placeholder
                    // The FeedSource ID here should be the one stored locally for the feed this item belongs to.
                    // This requires mapping Feedbin's feed_id to our local FeedSource.id.
                    // This is a complex part if fetching all items globally.
                    // For now, using dto.feedId.toString() as a stand-in for feedSourceId.
                    dto.toDomainModel(
                        feedSourceId = dto.feedId.toString(), // Needs proper mapping to local FeedSource.id
                        isRead = !unreadEntryIds.contains(dto.id),
                        isBookmarked = starredEntryIds.contains(dto.id)
                    )
                }
                allFeedItems.addAll(domainItems)

                // Pagination logic
                val linkHeader = response.headers[HttpHeaders.Link]
                if (linkHeader != null && linkHeader.contains("rel=\"next\"")) { // Escaped quote for string literal
                    currentPage++
                } else if (entryDTOs.size < feedbinPerPageLimit) {
                    morePages = false // Likely the last page
                } else {
                    // If Link header is not present but we received a full page, assume there might be more.
                    // This is a fallback and less reliable than Link header.
                    currentPage++ 
                }

                // Safety break for very large number of pages if Link header parsing is complex/missing
                if (currentPage > 200) { // Example limit to prevent infinite loops
                    // logger.w { "Reached page limit (200) for fetching Feedbin entries." }
                    morePages = false
                }
            }
            // TODO: Persist the new 'since' timestamp. This should be the 'created_at' of the latest entry,
            // or Clock.System.now().toEpochMilliseconds() if API doesn't guarantee order.
            // networkSettings.setLastSyncDate(newSinceTimestamp)
            DataResult.Success(allFeedItems)
        } catch (e: Exception) {
            // logger.e(e) { "Failed to get Feedbin feed items" }
            DataResult.Error(NetworkErrorKind.Unknown) // Example error
        }
    }

    suspend fun markAsRead(feedAccountId: String, itemIdsOnServer: List<String>): DataResult<Unit> {
        return try {
            if (itemIdsOnServer.isNotEmpty()) {
                val entryIds = itemIdsOnServer.mapNotNull { it.toLongOrNull() }
                if (entryIds.isNotEmpty()) {
                    feedbinApi.markEntriesAsRead(entryIds)
                    // TODO: Handle response if needed (e.g., confirm which IDs were successfully marked)
                }
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            // logger.e(e) { "Failed to mark Feedbin items as read" }
            DataResult.Error(NetworkErrorKind.Unknown)
        }
    }

    suspend fun markAsUnread(feedAccountId: String, itemIdsOnServer: List<String>): DataResult<Unit> {
         return try {
            if (itemIdsOnServer.isNotEmpty()) {
                val entryIds = itemIdsOnServer.mapNotNull { it.toLongOrNull() }
                 if (entryIds.isNotEmpty()) {
                    feedbinApi.markEntriesAsUnread(entryIds)
                }
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            // logger.e(e) { "Failed to mark Feedbin items as unread" }
            DataResult.Error(NetworkErrorKind.Unknown)
        }
    }

    suspend fun markAsBookmarked(feedAccountId: String, itemIdOnServer: String): DataResult<Unit> {
        return try {
            itemIdOnServer.toLongOrNull()?.let {
                feedbinApi.starEntries(listOf(it))
            } ?: return DataResult.Error(NetworkErrorKind.Unknown, "Invalid item ID format") // Or some other error
            DataResult.Success(Unit)
        } catch (e: Exception) {
            // logger.e(e) { "Failed to bookmark Feedbin item" }
            DataResult.Error(NetworkErrorKind.Unknown)
        }
    }

    suspend fun unmarkAsBookmarked(feedAccountId: String, itemIdOnServer: String): DataResult<Unit> {
        return try {
            itemIdOnServer.toLongOrNull()?.let {
                feedbinApi.unstarEntries(listOf(it))
            } ?: return DataResult.Error(NetworkErrorKind.Unknown, "Invalid item ID format")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            // logger.e(e) { "Failed to unbookmark Feedbin item" }
            DataResult.Error(NetworkErrorKind.Unknown)
        }
    }

    suspend fun addFeed(feedAccountId: String, url: String, title: String?): DataResult<FeedSource> {
        return try {
            val response = feedbinApi.createSubscription(FeedbinCreateSubscriptionRequest(feedUrl = url))
            // Existing status code handling for 201, 302, 300, 404, 422 is good.
            // This try-catch will handle exceptions from the API call itself (e.g. network errors before response)
            // or if response.body() fails.
            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.Found -> {
                    val createdSubscription: FeedbinSubscriptionDTO = response.body()
                    DataResult.Success(createdSubscription.toDomainModel())
                }
                HttpStatusCode.MultipleChoices -> {
                    DataResult.Error(NetworkErrorKind.FeedAdditionError, "Multiple feed choices found. Disambiguation needed.")
                }
                HttpStatusCode.NotFound -> { // Should be caught by ClientRequestException if Ktor throws for it
                    DataResult.Error(NetworkErrorKind.FeedAdditionError, "No feed found at the provided URL.")
                }
                HttpStatusCode.UnprocessableEntity -> { // Should be caught by ClientRequestException
                    DataResult.Error(NetworkErrorKind.FeedAdditionError, "The provided URL is not a valid feed.")
                }
                // Ensure other non-2xx codes that don't throw exceptions are handled or throw one to be caught below.
                // For example, if Ktor is configured not to throw for non-2xx. Default is to throw.
                else -> {
                     if (response.status.value >= 400) { // General error if not already a specific Client/Server exception
                        // This case might be hit if Ktor's expectSuccess is not used or globally turned off for some reason.
                        // logger.e { "Failed to add Feedbin feed. URL: $url, Status: ${response.status}, Body: ${response.bodyAsText()}" }
                        val errorKind = if (response.status.value >= 500) NetworkErrorKind.Server else NetworkErrorKind.Client
                        DataResult.Error(errorKind, "Failed to add feed. Status: ${response.status.value}")
                     } else {
                        // logger.w { "Unexpected success status in addFeed: ${response.status}" }
                        // This case should ideally not be hit if all success cases (201, 302) are handled.
                        DataResult.Error(NetworkErrorKind.Unknown, "Unexpected status: ${response.status.value}")
                     }
                }
            }
        } catch (e: ClientRequestException) {
            // logger.e(e) { "Client error adding Feedbin feed: $url, ${e.response.status}" }
             val errorKind = when (e.response.status) {
                HttpStatusCode.Unauthorized -> NetworkErrorKind.Authentication
                HttpStatusCode.Forbidden -> NetworkErrorKind.Forbidden
                HttpStatusCode.NotFound -> NetworkErrorKind.FeedAdditionError // Specific to this context
                HttpStatusCode.UnprocessableEntity -> NetworkErrorKind.FeedAdditionError
                HttpStatusCode.TooManyRequests -> NetworkErrorKind.RateLimit
                else -> NetworkErrorKind.Client
            }
            DataResult.Error(errorKind, message = "Failed to add feed: ${e.response.status.description}")
        } catch (e: ServerResponseException) {
            // logger.e(e) { "Server error adding Feedbin feed: $url, ${e.response.status}" }
            DataResult.Error(NetworkErrorKind.Server, message = "Server error when adding feed: ${e.response.status.description}")
        } catch (e: IOException) {
            // logger.e(e) { "Network error adding Feedbin feed: $url" }
            DataResult.Error(NetworkErrorKind.NetworkUnavailable, message = "Network error when adding feed.")
        } catch (e: Exception) {
            // logger.e(e) { "Unexpected error adding Feedbin feed: $url" }
            DataResult.Error(NetworkErrorKind.Unknown, message = "An unexpected error occurred.")
        }
    }
}
