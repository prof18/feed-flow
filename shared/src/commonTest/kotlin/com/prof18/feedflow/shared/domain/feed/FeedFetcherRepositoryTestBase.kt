package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.koin.TestModules
import com.prof18.feedflow.shared.test.koin.getWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class FeedFetcherRepositoryTestBase : KoinTestBase() {

    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setupTestDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDownTestDispatcher() {
        Dispatchers.resetMain()
    }

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<DispatcherProvider> { StandardDispatcherProvider(testDispatcher) }
            single {
                DatabaseHelper(
                    sqlDriver = get(),
                    backgroundDispatcher = testDispatcher,
                    logger = getWith("DatabaseHelper"),
                )
            }
            single { SyncedDatabaseHelper(backgroundDispatcher = testDispatcher) }
        }

    private class StandardDispatcherProvider(
        private val dispatcher: TestDispatcher,
    ) : DispatcherProvider {
        override val io = dispatcher
        override val main = dispatcher
        override val default = dispatcher
    }
}
