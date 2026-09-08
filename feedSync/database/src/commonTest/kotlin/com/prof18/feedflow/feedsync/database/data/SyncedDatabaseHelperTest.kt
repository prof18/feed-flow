package com.prof18.feedflow.feedsync.database.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SyncedDatabaseHelperTest {

    @Test
    fun `closeScope without initialized database does not require koin scope`() = runTest {
        val helper = SyncedDatabaseHelper(backgroundDispatcher = Dispatchers.Default)

        helper.closeScope()
    }
}
