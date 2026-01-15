package com.prof18.feedflow.feedsync.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.core.model.SyncedFeedItem
import com.prof18.feedflow.feedsync.database.data.SyncTable
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.test.generators.CategoryGenerator
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.sampleValue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.inject
import kotlin.test.Test

class SyncedDatabaseHelperTest : KoinTestBase() {

    private val database: SyncedDatabaseHelper by inject()

    override fun getTestModules(): List<Module> = listOf(
        module {
            single { SyncedDatabaseHelper(backgroundDispatcher = TestDispatcherProvider.testDispatcher) }
        },
        module {
            scope(named(feedSyncScopeName)) {
                scoped<SqlDriver>(named(syncDbDriver)) { createInMemorySyncDriver() }
            }
        },
    )

    @Test
    fun `should report empty database until all tables have data`() = runTest {
        database.isDatabaseEmpty() shouldBe true

        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(
            category = CategoryGenerator.categoryArb.sampleValue(),
        )
        val items = listOf(
            SyncedFeedItem(id = "sync-item-1", isRead = true, isBookmarked = false),
        )

        database.insertSyncedFeedSource(listOf(source))
        database.isDatabaseEmpty() shouldBe true

        database.insertFeedItems(items)
        database.isDatabaseEmpty() shouldBe false
    }

    @Test
    fun `should update metadata for synced feed sources`() = runTest {
        val source = FeedSourceGenerator.feedSourceArb.sampleValue().copy(category = null)
        database.insertSyncedFeedSource(listOf(source))

        database.getLastChangeTimestamp(SyncTable.SYNCED_FEED_SOURCE).shouldNotBeNull()
    }

    @Test
    fun `should update metadata for synced feed source categories`() = runTest {
        val category = CategoryGenerator.categoryArb.sampleValue()
        database.insertFeedSourceCategories(listOf(category))

        database.getLastChangeTimestamp(SyncTable.SYNCED_FEED_SOURCE_CATEGORY).shouldNotBeNull()
    }

    @Test
    fun `should update metadata for synced feed items`() = runTest {
        val items = listOf(
            SyncedFeedItem(id = "sync-item-2", isRead = false, isBookmarked = true),
        )
        database.insertFeedItems(items)

        database.getLastChangeTimestamp(SyncTable.SYNCED_FEED_ITEM).shouldNotBeNull()
    }

    private fun createInMemorySyncDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FeedFlowFeedSyncDB.Schema.create(driver)
        return driver
    }

    private companion object {
        const val feedSyncScopeName = "FeedSyncScope"
        const val syncDbDriver = "SyncDBDriver"
    }
}
