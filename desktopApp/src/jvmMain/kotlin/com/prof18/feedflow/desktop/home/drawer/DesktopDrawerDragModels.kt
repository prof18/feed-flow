package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.ui.geometry.Rect
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory

internal const val DesktopDrawerPinnedSectionKey = "__feedflow_pinned__"
internal const val DesktopDrawerUncategorizedSectionKey = "__feedflow_uncategorized__"

internal sealed interface DrawerDragPayload {
    data class FeedSources(
        val feedSources: List<FeedSource>,
        val sourceSectionKey: String,
        val sourceIndex: Int,
    ) : DrawerDragPayload

    data class Category(
        val categoryKey: String,
        val title: String,
        val sourceIndex: Int,
    ) : DrawerDragPayload
}

internal sealed interface DrawerDropDecision {
    data class ReorderInSection(
        val sectionKey: String,
        val insertionIndex: Int,
    ) : DrawerDropDecision

    data class MoveToCategory(
        val category: FeedSourceCategory?,
    ) : DrawerDropDecision

    data class ReorderCategories(
        val insertionIndex: Int,
    ) : DrawerDropDecision
}

internal data class ReorderSlot(
    val sectionKey: String,
    val index: Int,
    val rectInWindow: Rect,
    val category: FeedSourceCategory?,
    val isCategoryDropTarget: Boolean,
    val reorderEnabled: Boolean,
)

internal data class CategoryHeaderSlot(
    val sectionKey: String,
    val index: Int,
    val rectInWindow: Rect,
)

internal fun desktopDrawerCategorySectionKey(category: FeedSourceCategory?): String =
    category?.id ?: DesktopDrawerUncategorizedSectionKey
