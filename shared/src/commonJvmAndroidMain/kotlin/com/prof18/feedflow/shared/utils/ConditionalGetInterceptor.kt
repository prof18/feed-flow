package com.prof18.feedflow.shared.utils

import com.prof18.feedflow.shared.domain.feed.httpcache.FeedHttpCacheStore
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

internal class ConditionalGetInterceptor(
    private val feedHttpCacheStore: FeedHttpCacheStore,
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        val validators = feedHttpCacheStore.validatorsFor(url)
        val request = originalRequest.newBuilder().apply {
            validators?.etag?.let { header("If-None-Match", it) }
            validators?.lastModified?.let { header("If-Modified-Since", it) }
        }.build()

        val response = chain.proceed(request)

        feedHttpCacheStore.recordResponse(
            url = url,
            statusCode = response.code,
            etag = response.header("ETag"),
            lastModified = response.header("Last-Modified"),
            cacheControl = response.header("Cache-Control"),
            expires = response.header("Expires"),
            date = response.header("Date"),
            retryAfter = response.header("Retry-After"),
        )

        return response
    }
}
