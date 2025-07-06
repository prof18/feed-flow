# iOS Cache-Control Implementation for FeedFlow

## Overview

This document details the implementation of Cache-Control header support for RSS feed parsing on iOS, which complements the existing Android/JVM implementation with minimal platform-specific code.

## Architecture

### Shared Components

The implementation follows a **shared interface pattern** that maximizes code reuse across platforms:

#### 1. Common Interface (`CacheControlManager`)
```kotlin
// shared/src/commonMain/kotlin/com/prof18/feedflow/shared/utils/CacheControlManager.kt
interface CacheControlManager {
    fun storeCacheControlInfo(url: String, cacheInfo: CacheControlInfo)
    fun getCacheControlInfo(url: String): CacheControlInfo?
    fun clearCacheControlInfo()
}
```

#### 2. Shared Data Models (`CacheControlInfo`)
```kotlin
data class CacheControlInfo(
    val maxAge: Int? = null,
    val expiresTimestamp: Long? = null,
    val lastModifiedTimestamp: Long? = null,
    val etag: String? = null
)
```

#### 3. Common Utilities (`CacheControlUtils`)
- **Header Parsing**: Shared logic for parsing `Cache-Control`, `Expires`, `Last-Modified`, and `ETag` headers
- **Max-Age Extraction**: Supports both `max-age` and `s-maxage` directives
- **Freshness Calculation**: Common logic to determine if cached content is still fresh

### Platform-Specific Implementations

#### iOS Implementation

##### 1. iOS Cache Control Manager (`IosCacheControlManager`)
```kotlin
// shared/src/iosMain/kotlin/com/prof18/feedflow/shared/utils/IosCacheControlManager.kt
class IosCacheControlManager : CacheControlManager {
    fun extractAndStoreCacheInfo(url: String, response: NSURLResponse?)
    private fun parseHttpDateIos(dateString: String): Long?
}
```

**Features:**
- Implements the shared `CacheControlManager` interface
- Uses `NSDateFormatter` for accurate HTTP date parsing
- Extracts headers from `NSHTTPURLResponse`
- Thread-safe cache storage

##### 2. NSURLSession Delegate (`IosUrlSessionDelegate`)
```kotlin
class IosUrlSessionDelegate(
    private val cacheControlManager: IosCacheControlManager
) : NSObject(), NSURLSessionDataDelegateProtocol
```

**Responsibilities:**
- Intercepts HTTP responses automatically during RSS parsing
- Extracts cache control headers from each response
- Stores cache information for later use
- Maintains compatibility with existing RSS parsing flow

##### 3. URL Session Factory (`IosUrlSessionFactory`)
```kotlin
object IosUrlSessionFactory {
    fun createSessionWithCacheControl(
        cacheControlManager: IosCacheControlManager
    ): NSURLSession
}
```

**Features:**
- Creates `NSURLSession` configured with cache control interception
- Maintains existing User-Agent and other configurations
- Integrates seamlessly with RSS parser

#### Android/JVM Implementation

##### Enhanced OkHttp Interceptor (`CacheControlInterceptor`)
```kotlin
// shared/src/commonJvmAndroidMain/kotlin/com/prof18/feedflow/shared/utils/CacheControlInterceptor.kt
class CacheControlInterceptor : Interceptor, CacheControlManager
```

**Updated Features:**
- Now implements the shared `CacheControlManager` interface
- Uses `SimpleDateFormat` for accurate HTTP date parsing
- Thread-safe with `ConcurrentHashMap`
- Seamless integration with existing OkHttp setup

## Integration Points

### 1. Dependency Injection

#### iOS (`KoinIOS.kt`)
```kotlin
single<IosCacheControlManager> {
    IosCacheControlManager.getInstance()
}

single<CacheControlManager> {
    get<IosCacheControlManager>()
}

single {
    val cacheControlManager = get<IosCacheControlManager>()
    val nsUrlSession = IosUrlSessionFactory.createSessionWithCacheControl(cacheControlManager)
    
    RssParserBuilder(nsUrlSession = nsUrlSession).build()
}
```

#### Android/JVM (`KoinAndroid.kt`, `KoinDesktop.kt`)
```kotlin
single<CacheControlInterceptor> {
    CacheControlInterceptor.getInstance()
}

single<CacheControlManager> {
    get<CacheControlInterceptor>()
}
```

### 2. Feed Fetcher Repository

The `FeedFetcherRepository` now uses the shared `CacheControlManager` interface:

```kotlin
class FeedFetcherRepository(
    // ...
    private val cacheControlManager: CacheControlManager? = null,
) {
    private suspend fun saveCacheControlInfo(feedSource: FeedSource) {
        cacheControlManager?.getCacheControlInfo(feedSource.url)?.let { cacheInfo ->
            databaseHelper.updateCacheControlInfo(...)
        }
    }
}
```

## Cache Logic Priority

The cache control logic works consistently across all platforms:

1. **Force Refresh**: If `forceRefresh=true` (except openrss.org feeds)
2. **Cache-Control max-age**: Primary cache directive (preferred)
3. **Cache-Control expires**: Fallback cache directive
4. **Time-based logic**: Final fallback (1-hour/6-hour intervals)

## Platform Support Matrix

| Platform | Implementation | Cache Extraction | Date Parsing | Thread Safety |
|----------|----------------|------------------|--------------|---------------|
| **iOS** | `IosCacheControlManager` + `NSURLSessionDelegate` | `NSHTTPURLResponse.allHeaderFields` | `NSDateFormatter` | ✅ |
| **Android** | `CacheControlInterceptor` (OkHttp) | `Response.header()` | `SimpleDateFormat` | ✅ (`ConcurrentHashMap`) |
| **JVM/Desktop** | `CacheControlInterceptor` (OkHttp) | `Response.header()` | `SimpleDateFormat` | ✅ (`ConcurrentHashMap`) |

## Benefits of This Architecture

### 1. **Minimal Platform-Specific Code**
- Only ~150 lines of iOS-specific code
- Shared utilities and data models across all platforms
- Common business logic in `FeedFetcherRepository`

### 2. **Consistent Behavior**
- Same cache control logic on all platforms
- Identical database schema and storage
- Uniform cache header parsing

### 3. **Easy Maintenance**
- Single source of truth for cache control logic
- Platform-specific optimizations where needed
- Clear separation of concerns

### 4. **Performance Benefits**
- Respects server-side caching directives
- Reduces unnecessary network requests
- Improves RSS feed refresh efficiency

## Testing the Implementation

### iOS Testing
1. Build the iOS app with the new implementation
2. Monitor RSS feed requests in network logs
3. Verify cache control headers are being extracted
4. Test cache-based refresh decisions

### Verification Points
- Cache control headers are extracted from responses
- Database is updated with cache information
- Refresh logic uses cache directives appropriately
- No regression in existing functionality

## Future Enhancements

### 1. **Enhanced Date Parsing**
- Support more HTTP date formats
- Better error handling for malformed dates

### 2. **Cache Metrics**
- Track cache hit/miss rates
- Monitor cache effectiveness

### 3. **ETag Support**
- Implement conditional requests using ETags
- Reduce bandwidth for unchanged feeds

### 4. **Last-Modified Support**
- Use `If-Modified-Since` headers
- Additional bandwidth optimization

## Migration Notes

### For Existing iOS Builds
- No breaking changes to existing APIs
- RSS parsing behavior remains the same
- Cache control is additive functionality

### Database Schema
- Uses existing cache control fields added in previous implementation
- No additional migrations required

## Conclusion

This iOS implementation provides full Cache-Control header support while maintaining:
- **Code Sharing**: Maximum reuse of common logic
- **Platform Optimization**: Uses native iOS APIs where appropriate
- **Consistency**: Identical behavior across all platforms
- **Maintainability**: Clean separation of shared vs platform-specific code

The implementation successfully brings iOS to feature parity with Android/JVM for RSS feed caching while keeping platform-specific code to a minimum.