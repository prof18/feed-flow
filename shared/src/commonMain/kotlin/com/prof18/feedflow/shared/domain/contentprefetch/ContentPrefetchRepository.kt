package com.prof18.feedflow.shared.domain.contentprefetch

interface ContentPrefetchRepository {
    suspend fun prefetchContent()
    fun startBackgroundFetching()
    suspend fun cancelFetching()

    companion object {
        const val FIRST_PAGE_SIZE = 15L
    }
}
