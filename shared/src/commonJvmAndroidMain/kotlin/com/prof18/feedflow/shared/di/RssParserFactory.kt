package com.prof18.feedflow.shared.di

import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import com.prof18.feedflow.shared.utils.ConditionalGetInterceptor
import com.prof18.feedflow.shared.utils.StaticHeadersInterceptor
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import okhttp3.OkHttpClient

internal fun createRssParser(
    headers: Map<String, String>,
    feedHttpCacheStore: FeedHttpCacheStore,
): RssParser = RssParserBuilder(
    callFactory = OkHttpClient
        .Builder()
        .addInterceptor(StaticHeadersInterceptor(headers))
        .addInterceptor(ConditionalGetInterceptor(feedHttpCacheStore))
        .build(),
).build()
