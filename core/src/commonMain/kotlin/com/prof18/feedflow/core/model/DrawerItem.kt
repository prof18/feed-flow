package com.prof18.feedflow.core.model

import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper

data class NavDrawerState(
    val timeline: List<DrawerItem> = emptyList(),
    val categories: List<DrawerItem> = emptyList(),
    val feedSourcesWithoutCategory: List<DrawerItem> = emptyList(),
    val feedSourcesByCategory: Map<FeedSourceCategoryWrapper, List<DrawerItem>> = mapOf(),
) {
    fun isEmpty(): Boolean =
        categories.isEmpty() && feedSourcesByCategory.isEmpty()

    fun isNotEmpty(): Boolean =
        categories.isNotEmpty() || feedSourcesByCategory.isNotEmpty()
}

sealed class DrawerItem {
    data object Timeline : DrawerItem()

    data class DrawerCategory(
        val category: FeedSourceCategory,
    ) : DrawerItem()

    data class DrawerFeedSource(
        val feedSource: FeedSource,
    ) : DrawerItem() {

        data class FeedSourceCategoryWrapper(
            val feedSourceCategory: FeedSourceCategory?,
        )
    }
}
