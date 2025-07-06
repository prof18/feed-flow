package com.prof18.feedflow.shared.utils

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * OkHttp interceptor that extracts cache control information from HTTP responses
 * for Android and JVM platforms
 */
class CacheControlInterceptor : Interceptor, CacheControlManager {
    
    private val cacheInfoMap = ConcurrentHashMap<String, CacheControlInfo>()
    
    override fun intercept(chain: Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)
        
        // Extract cache control information from response headers
        val url = originalRequest.url.toString()
        val cacheControlInfo = extractCacheControlInfo(response)
        cacheInfoMap[url] = cacheControlInfo
        
        return response
    }
    
    override fun storeCacheControlInfo(url: String, cacheInfo: CacheControlInfo) {
        cacheInfoMap[url] = cacheInfo
    }
    
    override fun getCacheControlInfo(url: String): CacheControlInfo? {
        return cacheInfoMap[url]
    }
    
    override fun clearCacheControlInfo() {
        cacheInfoMap.clear()
    }
    
    private fun extractCacheControlInfo(response: Response): CacheControlInfo {
        val cacheControl = response.header("Cache-Control")
        val expires = response.header("Expires")
        val lastModified = response.header("Last-Modified")
        val etag = response.header("ETag")
        
        return CacheControlUtils.extractCacheControlInfo(
            cacheControlHeader = cacheControl,
            expiresHeader = expires,
            lastModifiedHeader = lastModified,
            etagHeader = etag
        ).copy(
            // Use platform-specific date parsing for better accuracy
            expiresTimestamp = expires?.let { parseHttpDateJvm(it) },
            lastModifiedTimestamp = lastModified?.let { parseHttpDateJvm(it) }
        )
    }
    
    /**
     * Platform-specific HTTP date parsing for JVM/Android using SimpleDateFormat
     */
    private fun parseHttpDateJvm(dateString: String): Long? {
        return try {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.parse(dateString)?.time
        } catch (e: Exception) {
            try {
                // Try alternative format
                SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT")
                }.parse(dateString)?.time
            } catch (e: Exception) {
                null
            }
        }
    }
    
    companion object {
        private var instance: CacheControlInterceptor? = null
        
        fun getInstance(): CacheControlInterceptor {
            return instance ?: synchronized(this) {
                instance ?: CacheControlInterceptor().also { instance = it }
            }
        }
    }
}