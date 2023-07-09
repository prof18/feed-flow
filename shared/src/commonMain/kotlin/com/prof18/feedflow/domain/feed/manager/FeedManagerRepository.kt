package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.opml.OPMLInput
import com.prof18.feedflow.domain.opml.OPMLOutput

interface FeedManagerRepository {
    suspend fun addFeedsFromFile(opmlInput: OPMLInput)
    suspend fun getFeeds(): List<FeedSource>
    suspend fun addFeed(url: String, name: String)
    suspend fun exportFeedsAsOpml(opmlOutput: OPMLOutput)
}