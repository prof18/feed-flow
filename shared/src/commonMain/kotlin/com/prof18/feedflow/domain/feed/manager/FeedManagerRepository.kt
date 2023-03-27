package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.domain.opml.OPMLInput

interface FeedManagerRepository {
    suspend fun addFeedsFromFile(opmlInput: OPMLInput)
    suspend fun getFeeds(): List<FeedSource>
    suspend fun addFeed(url: String, name: String)
}