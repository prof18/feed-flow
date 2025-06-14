package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.DateFormatter
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
    private val settingsRepository: SettingsRepository,
) {
    fun getFeeds(): Flow<ImmutableList<FeedItem>> {
        val dateFormat = settingsRepository.getDateFormat()
        return databaseHelper.getFeedWidgetItems(pageSize = 15)
            .map { items ->
                items.map { item ->
                    item.toFeedItem(
                        dateFormatter,
                        removeTitleFromDesc = false,
                        hideDescription = false,
                        hideImages = false,
                        dateFormat = dateFormat,
                    )
                }.toImmutableList()
            }
    }

    fun getFeedLayout() = settingsRepository.feedLayout

    internal suspend fun getFeedItemById(id: FeedItemId): FeedItemUrlInfo? {
        return databaseHelper.getFeedItemUrlInfo(id.id)
    }
}
