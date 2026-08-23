package com.prof18.feedflow.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class ReadingBehaviorState(
    val isSaveReaderModeContentEnabled: Boolean = false,
    val isPrefetchArticleContentEnabled: Boolean = false,
    val isKleadParserEnabled: Boolean = false,
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val isShowReadItemsEnabled: Boolean = false,
    val isHideReadItemsEnabled: Boolean = false,
    val articleOpenMode: ArticleOpenMode = ArticleOpenMode.FULL_ARTICLE,
)
