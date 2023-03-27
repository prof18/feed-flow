package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.domain.model.ParsedFeedSource

internal expect class OPMLFeedParser {
    suspend fun parse(opmlInput: OPMLInput): List<ParsedFeedSource>
}