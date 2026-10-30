package com.prof18.feedflow.android.feedverification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.presentation.FeedRefreshVerificationViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FeedRefreshVerificationPrompt() {
    val viewModel = koinViewModel<FeedRefreshVerificationViewModel>()
    val coordinator = koinInject<FeedRecoveryCoordinator>()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val busy by viewModel.isRecovering.collectAsStateWithLifecycle()
    val browser by coordinator.visibleState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    foreground = true
                    viewModel.onForeground()
                }
                Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    viewModel.cancel()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.cancel()
        }
    }
    if (foreground && viewModel.verificationAvailable && !busy && browser == null) {
        pending.firstOrNull()?.let { next ->
            WebsiteVerificationExplanationDialog(
                websiteUrl = next.record.effectiveUrl,
                onDismiss = { viewModel.dismiss(next) },
                onConfirm = {
                    viewModel.dismiss(next)
                    viewModel.verify(next)
                },
            )
        }
    }
}
