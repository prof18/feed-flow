package com.prof18.feedflow.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DatabaseDriverFactoryTest {

    @Test
    fun `validateRequiredSchemaObjects accepts complete schema`() {
        val driver = createCompleteSchemaDriver()

        validateRequiredSchemaObjects(driver)
    }

    @Test
    fun `validateRequiredSchemaObjects rejects missing startup view`() {
        val driver = createCompleteSchemaDriver()
        driver.execute(null, "DROP VIEW feed_source_unread_count;", 0, null)

        assertFailsWith<SQLException> {
            validateRequiredSchemaObjects(driver)
        }
    }

    @Test
    fun `validateRequiredSchemaObjects rejects current version database missing core tables`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE unrelated_table(id TEXT);", 0, null)
        setVersion(driver, FeedFlowDB.Schema.version)

        assertFailsWith<SQLException> {
            validateRequiredSchemaObjects(driver)
        }
    }

    private fun createCompleteSchemaDriver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FeedFlowDB.Schema.create(driver)
        setVersion(driver, FeedFlowDB.Schema.version)
        return driver
    }
}
