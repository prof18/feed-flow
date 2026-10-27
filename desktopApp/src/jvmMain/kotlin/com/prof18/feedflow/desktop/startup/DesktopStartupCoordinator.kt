package com.prof18.feedflow.desktop.startup

import com.prof18.feedflow.database.DesktopDatabaseFailure
import com.prof18.feedflow.database.DesktopDatabaseStartupException
import com.prof18.feedflow.database.findDesktopDatabaseStartupException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal sealed interface StartupState {
    data object Loading : StartupState
    data object Ready : StartupState
    data class DatabaseFailed(val failure: DesktopDatabaseFailure) : StartupState
}

/** Runs app startup and turns a database that cannot be opened into a state the UI can show. */
internal class DesktopStartupCoordinator(
    private val initDependencies: suspend () -> Unit,
    private val afterDatabaseReady: suspend () -> Unit,
    private val reportStartupFailure: (DesktopDatabaseStartupException) -> Unit,
) {
    private val mutableState = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = mutableState.asStateFlow()

    suspend fun start() {
        try {
            initDependencies()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val startupException = e.findDesktopDatabaseStartupException() ?: throw e
            reportStartupFailure(startupException)
            mutableState.value = StartupState.DatabaseFailed(startupException.failure)
            return
        }
        afterDatabaseReady()
        mutableState.value = StartupState.Ready
    }
}
