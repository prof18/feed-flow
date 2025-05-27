package com.prof18.feedflow.shared.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object InMemoryCacheControlStorage {
    private val cache = mutableMapOf<String, String>() // Key: URL, Value: Cache-Control header
    private val lock = Mutex()

    suspend fun put(url: String, cacheControlHeader: String) {
        lock.withLock {
            cache[url] = cacheControlHeader
        }
    }

    suspend fun get(url: String): String? = lock.withLock {
        cache[url]
    }
}
