package com.prof18.feedflow.core.model

import com.prof18.feedflow.core.model.DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper

data class NavDrawerState(
    val timeline: List<DrawerItem> = listOf(),
    val categories: List<DrawerItem> = listOf(),
    val feedSourcesByCategory: Map<FeedSourceCategoryWrapper, List<DrawerItem>> = mapOf(),
)

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
