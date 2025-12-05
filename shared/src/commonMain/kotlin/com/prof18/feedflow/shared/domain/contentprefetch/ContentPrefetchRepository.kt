package com.prof18.feedflow.shared.domain.contentprefetch

// TODO: rename to repository
interface ContentPrefetchRepository {
    suspend fun prefetchContent()
    fun startBackgroundFetching()

    companion object {
        const val FIRST_PAGE_SIZE = 15L
    }
}
