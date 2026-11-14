package com.prof18.feedflow.android.notifications

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
internal fun BackgroundRefreshTurnedOnSnackbar(
    events: Flow<*>,
    snackbarHostState: SnackbarHostState,
) {
    val message = LocalFeedFlowStrings.current.settingsNotificationsRefreshTurnedOn
    LaunchedEffect(events, snackbarHostState, message) {
        events.collect {
            // showSnackbar suspends until dismissed; launch so the view model's emit never waits on it.
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
    }
}
