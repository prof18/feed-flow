package com.prof18.feedflow.core.model

sealed interface DrawerItem {
    data object Timeline : DrawerItem

    data object CategorySectionTitle : DrawerItem

    data class DrawerCategory(
        val category: FeedSourceCategory,
    ) : DrawerItem

    data object CategorySourcesTitle : DrawerItem

    data class DrawerCategoryWrapper(
        val category: FeedSourceCategory?,
        val feedSources: List<FeedSourceWrapper>,
        val isExpanded: Boolean = false,
        val onExpandClick: (DrawerCategoryWrapper) -> Unit,
    ) : DrawerItem {
        data class FeedSourceWrapper(
            val feedSource: FeedSource,
        )
    }
}
