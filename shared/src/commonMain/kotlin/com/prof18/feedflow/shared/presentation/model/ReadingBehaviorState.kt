package com.prof18.feedflow.shared.presentation.model

data class ReadingBehaviorState(
    val isReaderModeEnabled: Boolean = false,
    val isSaveReaderModeContentEnabled: Boolean = false,
    val isPrefetchArticleContentEnabled: Boolean = false,
    val isMarkReadWhenScrollingEnabled: Boolean = true,
    val isShowReadItemsEnabled: Boolean = false,
)
