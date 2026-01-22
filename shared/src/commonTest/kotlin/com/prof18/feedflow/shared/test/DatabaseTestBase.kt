package com.prof18.feedflow.shared.test

import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.test.koin.TestModules
import org.koin.core.component.inject
import org.koin.core.module.Module

abstract class DatabaseTestBase : KoinTestBase() {

    protected val database: DatabaseHelper by inject()

    override fun getTestModules(): List<Module> = listOf(
        TestModules.createTestLoggingModule(),
        TestModules.createTestDatabaseModule(),
    ) + additionalModules()

    open fun additionalModules(): List<Module> = emptyList()
}
