package com.prof18.feedflow.shared.presentation.model

fun isFullyVisibleInViewport(
    itemStart: Int,
    itemSize: Int,
    viewportEndOffset: Int,
    afterContentPadding: Int,
): Boolean = itemStart >= 0 && itemStart + itemSize <= viewportEndOffset - afterContentPadding

data class ArticleRevealRequest(
    val articleId: String,
    val source: ArticleListSource,
)

enum class ArticleListSource {
    TIMELINE,
    SEARCH,
}
