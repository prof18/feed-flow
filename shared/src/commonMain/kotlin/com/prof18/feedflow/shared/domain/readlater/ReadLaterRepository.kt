package com.prof18.feedflow.shared.domain.readlater

import com.prof18.feedflow.core.model.ReadLaterMarker
import com.prof18.feedflow.core.model.ReadLaterMarkerWithDetails
import com.prof18.feedflow.database.DatabaseHelper
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random
import kotlin.time.Clock

class ReadLaterRepository(
    private val databaseHelper: DatabaseHelper,
    private val clock: Clock = Clock.System,
) {
    fun observeReadLaterMarkers(): Flow<List<ReadLaterMarkerWithDetails>> =
        databaseHelper.observeReadLaterMarkers()

    suspend fun getReadLaterMarkers(): List<ReadLaterMarkerWithDetails> =
        databaseHelper.getReadLaterMarkers()

    suspend fun saveReadLaterMarker(feedItemId: String, scrollPosition: Int): ReadLaterMarker {
        val marker = ReadLaterMarker(
            id = generateId(feedItemId),
            feedItemId = feedItemId,
            scrollPosition = scrollPosition,
            createdAt = clock.now().toEpochMilliseconds(),
        )
        databaseHelper.insertReadLaterMarker(marker)
        return marker
    }

    suspend fun deleteMarker(id: String) {
        databaseHelper.deleteReadLaterMarker(id)
    }

    suspend fun deleteMarkersForFeedItem(feedItemId: String) {
        databaseHelper.deleteReadLaterMarkersForFeedItem(feedItemId)
    }

    private fun generateId(feedItemId: String): String {
        val timestamp = clock.now().toEpochMilliseconds()
        val random = Random.nextInt(Int.MAX_VALUE)
        return "${feedItemId}_${timestamp}_$random"
    }
}
