package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.domain.mappers.FeedItemMappingSettings
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FeedWidgetRepository internal constructor(
    private val databaseHelper: DatabaseHelper,
    private val dateFormatter: DateFormatter,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
) {
    fun getFeeds(filter: WidgetContentFilter = WidgetContentFilter.Timeline): Flow<ImmutableList<FeedItem>> {
        val settings = FeedItemMappingSettings(
            hideDate = feedAppearanceSettingsRepository.getHideDate(),
            dateFormat = feedAppearanceSettingsRepository.getDateFormat(),
            timeFormat = feedAppearanceSettingsRepository.getTimeFormat(),
        )
        return databaseHelper.getFeedWidgetItems(pageSize = 15, filter = filter)
            .map { items ->
                items.map { item ->
                    item.toFeedItem(dateFormatter, settings = settings)
                }.toImmutableList()
            }
    }

    fun getWidgetRenderState(filter: WidgetContentFilter): Flow<WidgetRenderState> =
        observeResolvedWidgetFilter(filter)
            .distinctUntilChanged()
            .flatMapLatest { resolvedFilter ->
                getFeeds(resolvedFilter.contentFilter).map { feedItems ->
                    WidgetRenderState(
                        feedItems = feedItems,
                        contentFilter = resolvedFilter.contentFilter,
                        title = resolvedFilter.title,
                    )
                }
            }

    suspend fun resolveFilter(filter: WidgetContentFilter): WidgetContentFilter = when (filter) {
        is WidgetContentFilter.Category ->
            if (databaseHelper.getFeedSourceCategory(filter.categoryId) != null) {
                filter
            } else {
                WidgetContentFilter.Timeline
            }
        is WidgetContentFilter.Source ->
            if (databaseHelper.getFeedSource(filter.feedSourceId) != null) {
                filter
            } else {
                WidgetContentFilter.Timeline
            }
        else -> filter
    }

    suspend fun getWidgetTitle(filter: WidgetContentFilter): String? = when (filter) {
        is WidgetContentFilter.Category -> databaseHelper.getFeedSourceCategory(filter.categoryId)?.title
        is WidgetContentFilter.Source -> databaseHelper.getFeedSource(filter.feedSourceId)?.title
        else -> null
    }

    fun observeFeedSourceCategories(): Flow<List<FeedSourceCategory>> =
        databaseHelper.observeFeedSourceCategories()

    fun observeFeedSources(): Flow<List<FeedSource>> =
        databaseHelper.getFeedSourcesFlow()

    suspend fun getFeedSourceCategories(): List<FeedSourceCategory> =
        databaseHelper.getFeedSourceCategories()

    suspend fun getFeedSources(): List<FeedSource> =
        databaseHelper.getFeedSources()

    private fun observeResolvedWidgetFilter(
        filter: WidgetContentFilter,
    ): Flow<ResolvedWidgetFilter> = when (filter) {
        is WidgetContentFilter.Category -> observeFeedSourceCategories().map { categories ->
            categories.firstOrNull { it.id == filter.categoryId }
                ?.let { category -> ResolvedWidgetFilter(filter, category.title) }
                ?: ResolvedWidgetFilter(WidgetContentFilter.Timeline, title = null)
        }
        is WidgetContentFilter.Source -> observeFeedSources().map { feedSources ->
            feedSources.firstOrNull { it.id == filter.feedSourceId }
                ?.let { feedSource -> ResolvedWidgetFilter(filter, feedSource.title) }
                ?: ResolvedWidgetFilter(WidgetContentFilter.Timeline, title = null)
        }
        else -> flowOf(ResolvedWidgetFilter(filter, title = null))
    }

    internal suspend fun getFeedItemById(id: FeedItemId): FeedItemUrlInfo? {
        return databaseHelper.getFeedItemUrlInfo(id.id)
    }
}

data class WidgetRenderState(
    val feedItems: ImmutableList<FeedItem>,
    val contentFilter: WidgetContentFilter,
    val title: String?,
)

private data class ResolvedWidgetFilter(
    val contentFilter: WidgetContentFilter,
    val title: String?,
)
