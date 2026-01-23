package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository

class ContentPrefetchRepositoryFake : ContentPrefetchRepository {
    var cancelFetchingCalled = false
    var startBackgroundFetchingCalled = false
    var prefetchContentCalled = false

    override suspend fun prefetchContent() {
        prefetchContentCalled = true
    }

    override fun startBackgroundFetching() {
        startBackgroundFetchingCalled = true
    }

    override suspend fun cancelFetching() {
        cancelFetchingCalled = true
    }
}
