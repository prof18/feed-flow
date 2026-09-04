package com.prof18.feedflow.shared.di

import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.utils.ConditionalGetInterceptor
import com.prof18.feedflow.shared.utils.UserAgentInterceptor
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import okhttp3.OkHttpClient

internal fun createRssParser(
    userAgent: String,
    feedHttpCacheStore: FeedHttpCacheStore,
): RssParser = RssParserBuilder(
    callFactory = OkHttpClient
        .Builder()
        .addInterceptor(UserAgentInterceptor(userAgent))
        .addInterceptor(ConditionalGetInterceptor(feedHttpCacheStore))
        .build(),
).build()
