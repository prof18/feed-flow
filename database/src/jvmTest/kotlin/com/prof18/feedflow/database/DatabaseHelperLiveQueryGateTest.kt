package com.prof18.feedflow.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.core.utils.LiveQueryGate
import com.prof18.feedflow.db.FeedFlowDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DatabaseHelperLiveQueryGateTest {

    @Test
    fun `a write while the gate is paused does not re-run the live query`() = withHelper { helper, gate, driver ->
        val emissions = helper.observeSourceIds()
        assertEquals(listOf(SOURCE_A), emissions.awaitItem())
        val liveQuerySql = driver.executedSql.last()
        val liveQueryRuns = driver.executedSql.count { it == liveQuerySql }

        gate.pause()
        helper.insertFeedSource(listOf(parsedSource(SOURCE_B)))

        assertNull(emissions.itemOrNull())
        assertEquals(liveQueryRuns, driver.executedSql.count { it == liveQuerySql })
    }

    @Test
    fun `resuming the gate re-runs the live query with the current data`() = withHelper { helper, gate, _ ->
        val emissions = helper.observeSourceIds()
        assertEquals(listOf(SOURCE_A), emissions.awaitItem())

        gate.pause()
        helper.insertFeedSource(listOf(parsedSource(SOURCE_B)))
        gate.resume()

        assertEquals(listOf(SOURCE_A, SOURCE_B), emissions.awaitItem())
    }

    @Test
    fun `a live query observed while paused waits for the gate to open`() = withHelper { helper, gate, driver ->
        gate.pause()
        val queriesBeforeObserving = driver.executedSql.size
        val emissions = helper.observeSourceIds()

        assertNull(emissions.itemOrNull())
        assertEquals(queriesBeforeObserving, driver.executedSql.size)

        gate.resume()
        assertEquals(listOf(SOURCE_A), emissions.awaitItem())
    }

    private fun withHelper(
        block: suspend CollectorScope.(DatabaseHelper, LiveQueryGate, RecordingDriver) -> Unit,
    ) {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { jdbc ->
            FeedFlowDB.Schema.create(jdbc)
            val driver = RecordingDriver(jdbc)
            val gate = LiveQueryGate()
            val helper = DatabaseHelper(driver, Dispatchers.Default, Logger, gate)
            val collectors = CollectorScope(CoroutineScope(Dispatchers.Default + Job()))
            try {
                runBlocking {
                    helper.insertFeedSource(listOf(parsedSource(SOURCE_A)))
                    collectors.block(helper, gate, driver)
                }
            } finally {
                collectors.scope.cancel()
            }
        }
    }

    private class CollectorScope(val scope: CoroutineScope) {
        fun DatabaseHelper.observeSourceIds(): Channel<List<String>> {
            val channel = Channel<List<String>>(Channel.UNLIMITED)
            scope.launch {
                getFeedSourcesFlow().collect { sources -> channel.send(sources.map(FeedSource::id).sorted()) }
            }
            return channel
        }

        suspend fun <T> Channel<T>.awaitItem(): T = withTimeout(TIMEOUT_MS) { receive() }

        suspend fun <T> Channel<T>.itemOrNull(): T? = withTimeoutOrNull(QUIET_PERIOD_MS) { receive() }
    }

    private class RecordingDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
        val executedSql: MutableList<String> = CopyOnWriteArrayList()

        override fun <R> executeQuery(
            identifier: Int?,
            sql: String,
            mapper: (SqlCursor) -> QueryResult<R>,
            parameters: Int,
            binders: (SqlPreparedStatement.() -> Unit)?,
        ): QueryResult<R> {
            executedSql += sql
            return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
        }
    }

    private fun parsedSource(id: String) = ParsedFeedSource(
        id = id,
        url = "https://fixture.test/$id/rss",
        title = id,
        category = null,
        logoUrl = null,
        websiteUrl = null,
    )

    private companion object {
        const val SOURCE_A = "source-a"
        const val SOURCE_B = "source-b"
        const val TIMEOUT_MS = 5_000L
        const val QUIET_PERIOD_MS = 300L
    }
}
