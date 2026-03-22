package com.prof18.feedflow.feedsync.database.data

import kotlinx.coroutines.Dispatchers
import kotlin.test.Test

class SyncedDatabaseHelperTest {

    @Test
    fun `closeScope without initialized database does not require koin scope`() {
        val helper = SyncedDatabaseHelper(backgroundDispatcher = Dispatchers.Default)

        helper.closeScope()
    }
}
