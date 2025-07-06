package com.prof18.feedflow.shared.utils

import kotlinx.datetime.Clock

/**
 * Common interface for cache control management across all platforms
 */
interface CacheControlManager {
    /**
     * Store cache control information for a URL
     */
    fun storeCacheControlInfo(url: String, cacheInfo: CacheControlInfo)
    
    /**
     * Retrieve cache control information for a URL
     */
    fun getCacheControlInfo(url: String): CacheControlInfo?
    
    /**
     * Clear all stored cache control information
     */
    fun clearCacheControlInfo()
}

/**
 * Common data class for cache control information
 */
data class CacheControlInfo(
    val maxAge: Int? = null,
    val expiresTimestamp: Long? = null,
    val lastModifiedTimestamp: Long? = null,
    val etag: String? = null
) {
    /**
     * Determines the next refresh time based on Cache-Control headers
     * @param responseTimestamp The timestamp when the response was received
     * @return The timestamp when the resource should be refreshed, or null if no cache info available
     */
    fun getNextRefreshTime(responseTimestamp: Long): Long? {
        return when {
            maxAge != null -> {
                responseTimestamp + (maxAge * 1000L)
            }
            expiresTimestamp != null -> {
                expiresTimestamp
            }
            else -> null
        }
    }
    
    /**
     * Check if the cached content is still fresh
     * @param responseTimestamp The timestamp when the response was received
     * @param currentTimestamp The current timestamp
     * @return true if content is still fresh, false if it should be refreshed
     */
    fun isFresh(responseTimestamp: Long, currentTimestamp: Long): Boolean {
        val nextRefreshTime = getNextRefreshTime(responseTimestamp)
        return nextRefreshTime?.let { currentTimestamp < it } ?: false
    }
}

/**
 * Common cache control utilities that can be shared across platforms
 */
object CacheControlUtils {
    
    /**
     * Parse Cache-Control header string to extract max-age or s-maxage directive
     */
    fun parseMaxAge(cacheControlHeader: String?): Int? {
        if (cacheControlHeader == null) return null
        
        val directives = cacheControlHeader.split(",").map { it.trim() }
        for (directive in directives) {
            when {
                directive.startsWith("s-maxage=") -> {
                    // s-maxage takes precedence over max-age for shared caches
                    return directive.substringAfter("s-maxage=").toIntOrNull()
                }
                directive.startsWith("max-age=") -> {
                    return directive.substringAfter("max-age=").toIntOrNull()
                }
            }
        }
        return null
    }
    
    /**
     * Parse HTTP date string to timestamp (RFC 7231 format)
     * Expected format: "EEE, dd MMM yyyy HH:mm:ss GMT"
     */
    fun parseHttpDate(dateString: String): Long? {
        return try {
            // This is a simplified parser - platforms can override with more robust implementations
            // Common patterns:
            // "Wed, 21 Oct 2015 07:28:00 GMT"
            // "Wednesday, 21-Oct-15 07:28:00 GMT"
            // "Wed Oct 21 07:28:00 2015"
            
            // For now, return current time + 1 hour as fallback
            // This will be properly implemented in platform-specific code
            Clock.System.now().toEpochMilliseconds() + (60 * 60 * 1000)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract cache control information from response headers
     */
    fun extractCacheControlInfo(
        cacheControlHeader: String?,
        expiresHeader: String?,
        lastModifiedHeader: String?,
        etagHeader: String?
    ): CacheControlInfo {
        val maxAge = parseMaxAge(cacheControlHeader)
        val expiresTimestamp = expiresHeader?.let { parseHttpDate(it) }
        val lastModifiedTimestamp = lastModifiedHeader?.let { parseHttpDate(it) }
        
        return CacheControlInfo(
            maxAge = maxAge,
            expiresTimestamp = expiresTimestamp,
            lastModifiedTimestamp = lastModifiedTimestamp,
            etag = etagHeader
        )
    }
}

/**
 * Basic implementation of CacheControlManager using a simple map
 * Thread safety is handled by platform-specific implementations
 */
class BaseCacheControlManager : CacheControlManager {
    private val cacheInfoMap = mutableMapOf<String, CacheControlInfo>()
    
    override fun storeCacheControlInfo(url: String, cacheInfo: CacheControlInfo) {
        cacheInfoMap[url] = cacheInfo
    }
    
    override fun getCacheControlInfo(url: String): CacheControlInfo? {
        return cacheInfoMap[url]
    }
    
    override fun clearCacheControlInfo() {
        cacheInfoMap.clear()
    }
}