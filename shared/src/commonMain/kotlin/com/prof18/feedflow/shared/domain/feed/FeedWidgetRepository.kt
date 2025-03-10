package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import co.touchlab.skie.configuration.annotations.SuspendInterop
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedWidgetRepository internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val dateFormatter: DateFormatter,
    private val fetcherRepository: FeedFetcherRepository,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) {

    @SuspendInterop.Enabled
    suspend fun getFeedItems(
        pageSize: Long,
    ): List<FeedItem> =
        databaseHelper.getFeedItems(
            feedFilter = FeedFilter.Timeline,
            pageSize = pageSize,
            offset = 0,
            showReadItems = false,
        ).map { item ->
            item.toFeedItem(
                dateFormatter,
                removeTitleFromDesc = false,
                hideDescription = false,
                hideImages = false,
            )
        }

    fun getFeeds(): Flow<ImmutableList<FeedItem>> =
        databaseHelper.getFeedWidgetItems(pageSize = 15)
            .map { items ->
                items.map { item ->
                    item.toFeedItem(
                        dateFormatter,
                        removeTitleFromDesc = false,
                        hideDescription = false,
                        hideImages = false,
                    )
                }.toImmutableList()
            }

    @SuspendInterop.Enabled
    @Suppress("MagicNumber")
    suspend fun fetchFeeds() {
        val lastForegroundTimestamp = settingsRepository.getLastFeedSyncTimestamp()
        val oneHourInMillis = 60 * 60 * 1000L
        val currentTimestamp = dateFormatter.currentTimeMillis()

        if ((currentTimestamp - lastForegroundTimestamp) >= oneHourInMillis) {
            fetcherRepository.fetchFeeds()
        } else {
            logger.d { "An hour is not passed, skipping automatic refresh" }
        }
    }

    internal suspend fun getFeedItemById(id: FeedItemId): FeedItemUrlInfo? {
        return databaseHelper.getFeedItemUrlInfo(id.id)
    }
}
