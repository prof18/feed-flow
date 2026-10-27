package com.prof18.feedflow.shared.domain.feed

import co.touchlab.kermit.Logger
import com.prof18.feedflow.database.isDatabaseFailure
import com.prof18.feedflow.shared.logging.reportDatabaseFailure
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import kotlinx.coroutines.CancellationException
import com.prof18.feedflow.core.model.DatabaseError as DatabaseErrorCode

/**
 * Top-level boundary for a user-started write: a database failure (only recognized on desktop) is
 * reported, shown as a save error and turned into [onDatabaseFailure] instead of crashing the app.
 * Anything else, cancellation included, propagates unchanged.
 */
internal suspend inline fun <T> FeedStateRepository.runUserWrite(
    onDatabaseFailure: T,
    block: () -> T,
): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    handleUserWriteFailure(e)
    onDatabaseFailure
}

internal suspend fun FeedStateRepository.handleUserWriteFailure(error: Throwable) {
    if (!error.isDatabaseFailure()) throw error
    reportDatabaseFailure("user_write", error, Logger.withTag("DatabaseWrite"))
    emitErrorState(DatabaseError(DatabaseErrorCode.SaveFailed))
}
