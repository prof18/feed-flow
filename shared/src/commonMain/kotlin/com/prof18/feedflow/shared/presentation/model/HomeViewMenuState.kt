package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.FeedOrder

data class HomeViewMenuState(
    val feedOrder: FeedOrder,
    val showReadArticlesTimeline: Boolean,
)
