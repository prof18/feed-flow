package com.prof18.feedflow.core.domain

import com.prof18.rssparser.model.RssChannel

interface FeedSourceLogoRetriever {
    suspend fun getFeedSourceLogoUrl(rssChannel: RssChannel): String?
    suspend fun getFeedSourceLogoUrl(websiteLink: String?): String?
}
