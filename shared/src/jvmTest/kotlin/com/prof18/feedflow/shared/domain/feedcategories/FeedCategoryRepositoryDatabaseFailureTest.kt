package com.prof18.feedflow.shared.domain.feedcategories

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.feedsync.networkcore.NetworkSettings
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.feed.assertSaveFailed
import com.prof18.feedflow.shared.domain.feed.failAllWritesAsBusy
import com.prof18.feedflow.shared.domain.feed.runUserWrite
import com.prof18.feedflow.shared.test.FailableSqlDriver
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import com.prof18.feedflow.shared.test.createInMemoryDriver
import com.prof18.feedflow.shared.test.koin.TestModules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FeedCategoryRepositoryDatabaseFailureTest : KoinTestBase() {

    private val failableDriver = FailableSqlDriver(createInMemoryDriver())
    private val feedCategoryRepository: FeedCategoryRepository by inject()
    private val feedStateRepository: FeedStateRepository by inject()
    private val databaseHelper: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> =
        TestModules.createTestModules() + module {
            single<SqlDriver> { failableDriver }
        }

    @BeforeTest
    fun setUpAccount() {
        getKoin().get<NetworkSettings>().setSyncAccountType(SyncAccounts.LOCAL)
    }

    @Test
    fun `adding a category on a busy database reports a save failure`() = runTest(testDispatcher) {
        failableDriver.failAllWritesAsBusy()

        feedStateRepository.assertSaveFailed { feedCategoryRepository.addNewCategory(CategoryName("News")) }
    }

    @Test
    fun `renaming a category on a busy database reports a save failure`() = runTest(testDispatcher) {
        databaseHelper.insertCategories(listOf(FeedSourceCategory(id = "cat", title = "Old")))
        failableDriver.failAllWritesAsBusy()

        feedStateRepository.assertSaveFailed {
            feedCategoryRepository.updateCategoryName(CategoryId("cat"), CategoryName("New"))
        }
    }

    @Test
    fun `deleting a category on a busy database reports a save failure`() = runTest(testDispatcher) {
        databaseHelper.insertCategories(listOf(FeedSourceCategory(id = "cat", title = "Old")))
        failableDriver.failAllWritesAsBusy()

        feedStateRepository.assertSaveFailed { feedCategoryRepository.deleteCategory("cat") }
    }

    @Test
    fun `the boundary lets other failures and cancellation through`() = runTest(testDispatcher) {
        assertFailsWith<IllegalStateException> {
            feedStateRepository.runUserWrite(onDatabaseFailure = Unit) { error("not a database failure") }
        }
        assertFailsWith<CancellationException> {
            feedStateRepository.runUserWrite(onDatabaseFailure = Unit) { throw CancellationException("stopped") }
        }
    }
}
