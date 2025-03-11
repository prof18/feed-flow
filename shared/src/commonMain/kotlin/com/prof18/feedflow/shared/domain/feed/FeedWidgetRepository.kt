package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedWidgetRepository internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val dateFormatter: DateFormatter,
) {
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

    internal suspend fun getFeedItemById(id: FeedItemId): FeedItemUrlInfo? {
        return databaseHelper.getFeedItemUrlInfo(id.id)
    }
}
