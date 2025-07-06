# Cache-Control Header Implementation for RSS Feed Parsing

This document describes the implementation of Cache-Control header checking for RSS feeds in FeedFlow.

## Overview

The RSS parser now checks Cache-Control headers from RSS feed responses to determine when to refresh feeds instead of relying solely on fixed time intervals. This improves efficiency by respecting server-side caching directives.

## Components Added/Modified

### 1. Database Schema (`FeedSource.sq`)

Added cache control related fields to the `feed_source` table:
- `cache_control_max_age`: INTEGER - max-age directive in seconds
- `cache_control_expires`: INTEGER - expires header timestamp
- `cache_control_last_modified`: INTEGER - last-modified header timestamp  
- `cache_control_etag`: TEXT - etag value

Added `updateCacheControlInfo` query to update cache control information.

### 2. FeedSource Model (`FeedSource.kt`)

Added corresponding fields to the FeedSource data class:
- `cacheControlMaxAge: Int?`
- `cacheControlExpires: Long?`
- `cacheControlLastModified: Long?`
- `cacheControlEtag: String?`

### 3. CacheControlInterceptor (`CacheControlInterceptor.kt`)

New OkHttp interceptor that:
- Intercepts RSS feed HTTP responses
- Extracts Cache-Control, Expires, Last-Modified, and ETag headers
- Stores cache information for later retrieval
- Provides utility methods to determine if content is fresh

Supported Cache-Control directives:
- `max-age`: Preferred over expires header
- `s-maxage`: Takes precedence over max-age for shared caches

### 4. DatabaseHelper (`DatabaseHelper.kt`)

Added:
- `updateCacheControlInfo()` method to save cache control data
- Updated all `transformToFeedSource()` methods to include cache control fields
- Updated database mappings to handle new fields

### 5. FeedFetcherRepository (`FeedFetcherRepository.kt`)

Modified:
- Added `cacheControlInterceptor` parameter to constructor
- Enhanced `shouldRefreshFeed()` to check cache control headers before falling back to time-based logic
- Added `shouldRefreshBasedOnCacheControl()` helper method
- Added `saveCacheControlInfo()` to persist cache data after fetching
- Updated `parseFeeds()` to save cache control information

### 6. Dependency Injection

Updated platform-specific DI modules:
- **Android** (`KoinAndroid.kt`): Added CacheControlInterceptor singleton and injected into RSS parser
- **JVM/Desktop** (`KoinDesktop.kt`): Added CacheControlInterceptor singleton and injected into RSS parser  
- **Common** (`Koin.kt`): Updated FeedFetcherRepository to optionally receive CacheControlInterceptor
- **iOS**: Uses `getOrNull()` for CacheControlInterceptor (not available on iOS platform)

## Cache Logic Priority

1. **Force refresh**: Always refreshes if `forceRefresh=true` (except for openrss.org feeds)
2. **Cache-Control max-age**: If present, uses this duration from last sync
3. **Cache-Control expires**: If present and no max-age, uses absolute expiration time
4. **Fallback to time-based**: Uses existing 1-hour/6-hour logic if no cache headers

## Platform Support

- ✅ **Android**: Full support with OkHttp interceptor
- ✅ **JVM/Desktop**: Full support with OkHttp interceptor  
- ⚠️ **iOS**: Basic support (cache info saved if available, but iOS NSURLSession doesn't use the interceptor)

## Benefits

1. **Efficiency**: Reduces unnecessary network requests by respecting server cache directives
2. **Server-friendly**: Honors publisher preferences for update frequency
3. **Backward compatible**: Falls back to existing time-based logic when cache headers unavailable
4. **Flexible**: Supports multiple cache control mechanisms (max-age, expires, etc.)

## Future Enhancements

1. **iOS Support**: Implement NSURLSession-based cache header extraction
2. **ETag Support**: Use ETags for conditional requests (If-None-Match)
3. **Last-Modified Support**: Use Last-Modified for conditional requests (If-Modified-Since)
4. **Cache Statistics**: Track cache hit/miss rates for analytics
5. **User Override**: Allow users to override cache settings per feed

## Testing

The implementation handles:
- Missing cache headers (falls back to time-based logic)
- Invalid cache values (gracefully ignored)
- Multiple cache directives (priority to s-maxage > max-age > expires)
- Platform differences (iOS graceful degradation)