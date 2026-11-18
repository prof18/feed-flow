package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.presentation.LoadingPhase
import com.prof18.feedflow.shared.presentation.toLoadingPhase
import org.jetbrains.jewel.ui.component.CircularProgressIndicator

/**
 * Branch on the returned phase instead of on [isLoading], so a fast load shows neither the empty state nor a spinner.
 */
@Composable
internal fun rememberLoadingPhase(isLoading: Boolean): LoadingPhase {
    val currentIsLoading by rememberUpdatedState(isLoading)
    val phase by produceState(if (isLoading) LoadingPhase.Pending else LoadingPhase.Ready) {
        snapshotFlow { currentIsLoading }
            .toLoadingPhase()
            .collect { value = it }
    }
    return phase
}

@Composable
internal fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
