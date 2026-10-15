package com.prof18.feedflow.desktop.ui.components

import com.prof18.feedflow.core.model.VisibleFeedItem

internal data class ArticleKey(val id: String)
internal data object ArticleFooterKey

internal fun visibleArticles(ids: List<String>, visibleKeys: List<Any>): List<VisibleFeedItem> {
    val indexes = ids.withIndex().associate { it.value to it.index }
    return visibleKeys.mapNotNull { key ->
        val id = (key as? ArticleKey)?.id ?: return@mapNotNull null
        indexes[id]?.let { VisibleFeedItem(id, it) }
    }
}

internal fun articlePageRequest(ids: List<String>, visibleKeys: List<Any>): String? {
    val lastVisible = visibleArticles(ids, visibleKeys).maxOfOrNull { it.index } ?: return null
    return ids.lastOrNull()?.takeIf { lastVisible >= ids.size - PageThreshold }
}

internal fun moveArticleFocus(ids: List<String>, focusedId: String?, delta: Int): String? {
    if (ids.isEmpty()) return null
    val currentIndex = ids.indexOf(focusedId)
    return ids[if (currentIndex < 0) 0 else (currentIndex + delta).coerceIn(ids.indices)]
}

private const val PageThreshold = 15
