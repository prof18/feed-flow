package com.prof18.feedflow.shared.utils

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

class CacheControlInterceptor(
    private val cacheControlCallback: CacheControlCallback,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        val url = originalRequest.url.toString()
        val cacheControl = response.header("Cache-Control")
        val maxAge = parseCacheControlMaxAge(cacheControl)

        if (maxAge != null) {
            cacheControlCallback.onCacheControlReceived(url, maxAge)
        }

        return response
    }

    private fun parseCacheControlMaxAge(cacheControl: String?): Long? {
        if (cacheControl == null) return null

        val maxAgeRegex = """max-age=(\d+)""".toRegex()
        val matchResult = maxAgeRegex.find(cacheControl)
        return matchResult?.groupValues?.get(1)?.toLongOrNull()
    }
}

interface CacheControlCallback {
    fun onCacheControlReceived(url: String, maxAgeSeconds: Long)
}

class CacheControlStore : CacheControlCallback {
    private val cacheControlData = ConcurrentHashMap<String, Long>()

    override fun onCacheControlReceived(url: String, maxAgeSeconds: Long) {
        cacheControlData[url] = maxAgeSeconds
    }

    fun getMaxAge(url: String): Long? {
        return cacheControlData[url]
    }

    fun clear() {
        cacheControlData.clear()
    }
}
