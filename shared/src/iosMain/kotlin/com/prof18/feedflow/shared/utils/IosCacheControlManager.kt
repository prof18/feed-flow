package com.prof18.feedflow.shared.utils

import platform.Foundation.*
import kotlinx.cinterop.*

/**
 * iOS-specific cache control manager using NSURLSession
 * This integrates with the RSS parsing process to extract cache headers
 */
class IosCacheControlManager : CacheControlManager {
    
    private val cacheManager = BaseCacheControlManager()
    
    override fun storeCacheControlInfo(url: String, cacheInfo: CacheControlInfo) {
        cacheManager.storeCacheControlInfo(url, cacheInfo)
    }
    
    override fun getCacheControlInfo(url: String): CacheControlInfo? {
        return cacheManager.getCacheControlInfo(url)
    }
    
    override fun clearCacheControlInfo() {
        cacheManager.clearCacheControlInfo()
    }
    
    /**
     * Extract cache control information from NSURLResponse
     * This should be called after making a request with NSURLSession
     */
    fun extractAndStoreCacheInfo(url: String, response: NSURLResponse?) {
        val httpResponse = response as? NSHTTPURLResponse ?: return
        
        val cacheInfo = extractCacheControlInfo(httpResponse)
        storeCacheControlInfo(url, cacheInfo)
    }
    
    private fun extractCacheControlInfo(response: NSHTTPURLResponse): CacheControlInfo {
        val headers = response.allHeaderFields
        
        val cacheControl = headers["Cache-Control"] as? String
        val expires = headers["Expires"] as? String
        val lastModified = headers["Last-Modified"] as? String  
        val etag = headers["ETag"] as? String
        
        return CacheControlUtils.extractCacheControlInfo(
            cacheControlHeader = cacheControl,
            expiresHeader = expires,
            lastModifiedHeader = lastModified,
            etagHeader = etag
        ).copy(
            // Use platform-specific date parsing for better accuracy
            expiresTimestamp = expires?.let { parseHttpDateIos(it) },
            lastModifiedTimestamp = lastModified?.let { parseHttpDateIos(it) }
        )
    }
    
    /**
     * Platform-specific HTTP date parsing for iOS using NSDateFormatter
     */
    private fun parseHttpDateIos(dateString: String): Long? {
        return try {
            val formatter = NSDateFormatter().apply {
                dateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz"
                timeZone = NSTimeZone.timeZoneWithName("GMT")
                locale = NSLocale(localeIdentifier = "en_US_POSIX")
            }
            
            formatter.dateFromString(dateString)?.timeIntervalSince1970?.times(1000)?.toLong()
        } catch (e: Exception) {
            try {
                // Try alternative format
                val formatter = NSDateFormatter().apply {
                    dateFormat = "EEEE, dd-MMM-yy HH:mm:ss zzz"
                    timeZone = NSTimeZone.timeZoneWithName("GMT")
                    locale = NSLocale(localeIdentifier = "en_US_POSIX")
                }
                
                formatter.dateFromString(dateString)?.timeIntervalSince1970?.times(1000)?.toLong()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    companion object {
        private var instance: IosCacheControlManager? = null
        
        fun getInstance(): IosCacheControlManager {
            return instance ?: synchronized(this) {
                instance ?: IosCacheControlManager().also { instance = it }
            }
        }
    }
}