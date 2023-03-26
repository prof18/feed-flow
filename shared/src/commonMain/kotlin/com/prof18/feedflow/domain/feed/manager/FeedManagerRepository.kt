package com.prof18.feedflow.domain.feed.manager

import com.prof18.feedflow.domain.model.FeedSource

interface FeedManagerRepository {
    suspend fun addFeedsFromFile(source: String)
    suspend fun getFeeds(): List<FeedSource>
    suspend fun addFeed(url: String, name: String)
}