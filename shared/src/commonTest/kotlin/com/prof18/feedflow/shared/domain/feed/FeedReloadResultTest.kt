package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.core.model.DatabaseError
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.test.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.prof18.feedflow.shared.presentation.model.DatabaseError as DatabaseErrorState

class FeedReloadResultTest : KoinTestBase() {
    @Test
    fun `empty successful reload returns true without reporting an error`() = runTest(testDispatcher) {
        val repository = get<FeedStateRepository>()
        repository.errorState.test {
            assertTrue(repository.reloadFeeds())
            assertTrue(repository.feedState.value.isEmpty())
            expectNoEvents()
        }
    }

    @Test
    fun `failed database reload returns false and retains existing error reporting`() = runTest(testDispatcher) {
        val repository = get<FeedStateRepository>()
        get<DatabaseHelper>().close()
        repository.errorState.test {
            assertFalse(repository.reloadFeeds())
            assertEquals(DatabaseErrorState(DatabaseError.FeedRetrievalFailed), awaitItem())
        }
    }
}
