package com.prof18.feedflow.shared.domain.model

import com.prof18.feedflow.core.model.ParsedFeedSource

internal data class NotValidFeedSources(
    val feedSources: List<ParsedFeedSource>,
    val feedSourcesWithError: List<ParsedFeedSource>,
)
