package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for tests that need Koin dependency injection.
 * Provides a default set of modules that can be overridden or extended by subclasses.
 */
abstract class KoinTestBase : KoinTest {

    @BeforeTest
    fun setupKoin() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            allowOverride(true)
            modules(getTestModules())
        }
    }

    @AfterTest
    fun tearDownKoin() {
        testDispatcher.scheduler.advanceUntilIdle()
        stopKoin()
    }

    /**
     * Returns the list of Koin modules to be used in the test.
     * By default, it provides a complete production-like setup.
     * Override this to provide a different set of modules.
     */
    open fun getTestModules(): List<Module> = TestModules.createTestModules()
}
