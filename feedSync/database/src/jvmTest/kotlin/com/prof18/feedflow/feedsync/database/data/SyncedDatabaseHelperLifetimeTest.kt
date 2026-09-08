package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import com.prof18.feedflow.feedsync.database.di.FEED_SYNC_SCOPE_NAME
import com.prof18.feedflow.feedsync.database.di.SYNC_DB_DRIVER
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncedDatabaseHelperLifetimeTest {

    @Test
    fun `close waits for an in-flight query`() = runTest {
        val fixture = Fixture()
        fixture.helper.isDatabaseEmpty()
        fixture.latestDriver.blockQueries = true

        val query = async(Dispatchers.Default) { fixture.helper.isDatabaseEmpty() }
        fixture.latestDriver.queryStarted.await()
        val close = async(start = CoroutineStart.UNDISPATCHED) { fixture.helper.closeScope() }

        try {
            assertFalse(close.isCompleted)
        } finally {
            fixture.latestDriver.allowQueryToFinish.complete(Unit)
        }
        query.await()
        close.await()
        assertEquals(1, fixture.latestDriver.closeCount)
        fixture.application.close()
    }

    @Test
    fun `closed database block prevents reopen until file work finishes`() = runTest {
        val fixture = Fixture()
        fixture.helper.isDatabaseEmpty()
        val fileWorkStarted = CompletableDeferred<Unit>()
        val allowFileWorkToFinish = CompletableDeferred<Unit>()

        val fileWork = async(start = CoroutineStart.UNDISPATCHED) {
            fixture.helper.withClosedDatabase {
                fileWorkStarted.complete(Unit)
                allowFileWorkToFinish.await()
            }
        }
        fileWorkStarted.await()
        val reopen = async(start = CoroutineStart.UNDISPATCHED) { fixture.helper.isDatabaseEmpty() }

        try {
            assertFalse(reopen.isCompleted)
        } finally {
            allowFileWorkToFinish.complete(Unit)
        }
        fileWork.await()
        assertTrue(reopen.await())
        assertEquals(2, fixture.driverCount)
        fixture.helper.closeScope()
        fixture.application.close()
    }

    @Test
    fun `close is idempotent`() = runTest {
        val fixture = Fixture()
        fixture.helper.isDatabaseEmpty()
        val driver = fixture.latestDriver

        fixture.helper.closeScope()
        fixture.helper.closeScope()

        assertEquals(1, driver.closeCount)
        fixture.application.close()
    }

    @Test
    fun `failed close blocks file work and reopen until close retry succeeds`() = runTest {
        val fixture = Fixture()
        fixture.helper.isDatabaseEmpty()
        fixture.latestDriver.failClose = true
        var fileWorkRan = false

        assertFailsWith<IllegalStateException> { fixture.helper.closeScope() }
        assertFailsWith<IllegalStateException> {
            fixture.helper.withClosedDatabase { fileWorkRan = true }
        }
        assertFalse(fileWorkRan)
        assertFailsWith<IllegalStateException> { fixture.helper.isDatabaseEmpty() }

        fixture.latestDriver.failClose = false
        fixture.helper.withClosedDatabase { fileWorkRan = true }
        assertTrue(fileWorkRan)
        assertTrue(fixture.helper.isDatabaseEmpty())
        assertEquals(2, fixture.driverCount)
        fixture.helper.closeScope()
        fixture.application.close()
    }
}

private class Fixture {
    private val drivers = mutableListOf<ControllableSqlDriver>()
    val driverCount: Int get() = drivers.size
    val latestDriver: ControllableSqlDriver get() = drivers.last()
    val application: KoinApplication = koinApplication {
        modules(
            module {
                scope(named(FEED_SYNC_SCOPE_NAME)) {
                    scoped<SqlDriver>(named(SYNC_DB_DRIVER)) {
                        val delegate = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
                        FeedFlowFeedSyncDB.Schema.create(delegate)
                        ControllableSqlDriver(delegate).also(drivers::add)
                    }
                }
            },
        )
    }
    val helper = SyncedDatabaseHelper(
        backgroundDispatcher = Dispatchers.Unconfined,
        koinContext = application.koin,
    )
}

private class ControllableSqlDriver(private val delegate: SqlDriver) : SqlDriver {
    val queryStarted = CompletableDeferred<Unit>()
    val allowQueryToFinish = CompletableDeferred<Unit>()
    var blockQueries = false
    var failClose = false
    var closeCount = 0

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
        if (blockQueries) {
            queryStarted.complete(Unit)
            runBlocking { allowQueryToFinish.await() }
        }
        return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> = delegate.execute(identifier, sql, parameters, binders)

    override fun newTransaction(): QueryResult<Transacter.Transaction> = delegate.newTransaction()

    override fun currentTransaction(): Transacter.Transaction? = delegate.currentTransaction()

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) =
        delegate.addListener(queryKeys = queryKeys, listener = listener)

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) =
        delegate.removeListener(queryKeys = queryKeys, listener = listener)

    override fun notifyListeners(vararg queryKeys: String) = delegate.notifyListeners(queryKeys = queryKeys)

    override fun close() {
        closeCount += 1
        if (failClose) {
            error("Simulated close failure")
        }
        delegate.close()
    }
}
