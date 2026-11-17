package com.prof18.feedflow.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class ReadingBehaviorState(
    val isSaveReaderModeContentEnabled: Boolean = false,
    val isPrefetchArticleContentEnabled: Boolean = false,
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val readArticlesVisibility: ReadArticlesVisibility = ReadArticlesVisibility.HIDE_ON_REFRESH,
    val articleOpenMode: ArticleOpenMode = ArticleOpenMode.FULL_ARTICLE,
    val articleBrowserId: String? = null,
)
