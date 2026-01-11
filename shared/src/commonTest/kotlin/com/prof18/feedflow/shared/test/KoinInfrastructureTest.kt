package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinInfrastructureTest : KoinTestBase() {

    private val feedStateRepository: FeedStateRepository by inject()

    @Test
    fun koin_can_inject_repositories() {
        assertNotNull(feedStateRepository)
    }
}
