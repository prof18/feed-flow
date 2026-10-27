package com.prof18.feedflow.shared.domain.feed

import app.cash.turbine.test
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.test.FailableSqlDriver
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import kotlin.test.assertEquals
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

internal fun FailableSqlDriver.failAllWritesAsBusy() {
    writeFailure = { SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY) }
}

/** Runs [action] and asserts it reported a save failure instead of throwing. */
internal suspend fun FeedStateRepository.assertSaveFailed(action: suspend () -> Unit) {
    errorState.test {
        action()
        assertEquals(DatabaseError(DatabaseErrorCode.SaveFailed), awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
