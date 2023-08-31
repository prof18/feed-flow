package com.prof18.feedflow.domain.model

import com.prof18.feedflow.core.model.ParsedFeedSource

data class NotValidFeedSources(
    val feedSources: List<ParsedFeedSource>,
)
