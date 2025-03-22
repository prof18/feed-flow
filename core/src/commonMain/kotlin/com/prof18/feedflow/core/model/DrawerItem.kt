package com.prof18.feedflow.core.model

import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class NavDrawerState(
    val timeline: ImmutableList<DrawerItem> = persistentListOf(),
    val read: ImmutableList<DrawerItem> = persistentListOf(),
    val bookmarks: ImmutableList<DrawerItem> = persistentListOf(),
    val categories: ImmutableList<DrawerItem> = persistentListOf(),
    val pinnedFeedSources: ImmutableList<DrawerItem> = persistentListOf(),
    val feedSourcesWithoutCategory: ImmutableList<DrawerItem> = persistentListOf(),
    val feedSourcesByCategory: ImmutableMap<FeedSourceCategoryWrapper, List<DrawerItem>> = persistentMapOf(),
) {
    fun isEmpty(): Boolean =
        categories.isEmpty() && feedSourcesByCategory.isEmpty()

    fun isNotEmpty(): Boolean =
        categories.isNotEmpty() || feedSourcesByCategory.isNotEmpty()
}

sealed class DrawerItem {
    data class Timeline(
        val unreadCount: Long,
    ) : DrawerItem()

    data object Read : DrawerItem()

    data class Bookmarks(
        val unreadCount: Long,
    ) : DrawerItem()

    data class DrawerCategory(
        val category: FeedSourceCategory,
        val unreadCount: Long,
    ) : DrawerItem()

    data class DrawerFeedSource(
        val feedSource: FeedSource,
        val unreadCount: Long,
    ) : DrawerItem() {

        data class FeedSourceCategoryWrapper(
            val feedSourceCategory: FeedSourceCategory?,
        )
    }
}
