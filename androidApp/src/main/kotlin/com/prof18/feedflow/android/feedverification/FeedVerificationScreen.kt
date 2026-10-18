package com.prof18.feedflow.android.feedverification

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.RecoveryPolicy
import com.prof18.feedflow.shared.domain.recovery.VerificationFailure
import com.prof18.feedflow.shared.domain.recovery.WebsiteProfileCleaner
import com.prof18.feedflow.shared.domain.recovery.WebsiteVerificationRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
internal fun FeedVerificationScreen() {
    FeedRefreshVerificationPrompt()
    val coordinator = koinInject<FeedRecoveryCoordinator>()
    val repository = koinInject<FeedRecoveryRepository>()
    val webViewProfiles = koinInject<WebViewProfiles>()
    val profileCleaner = koinInject<WebsiteProfileCleaner>()
    val request by coordinator.visibleState.collectAsStateWithLifecycle()
    val epoch by repository.accountEpochState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    VerificationScreenEffects(coordinator, epoch)
    val active = request ?: return
    var session by remember(active.sessionId) { mutableStateOf<AndroidFeedVerificationSession?>(null) }
    var rendererStopped by remember(active.sessionId) { mutableStateOf(false) }
    var captureJob by remember(active.sessionId) { mutableStateOf<Job?>(null) }
    var pageLoaded by remember(active.sessionId) { mutableStateOf(false) }
    var pageState by remember(active.sessionId) { mutableStateOf(VerificationPageState.Loading) }

    LaunchedEffect(epoch, session) {
        session?.let { if (!repository.isCurrent(it.metadata)) coordinator.cancelAll() }
    }

    LaunchedEffect(active.sessionId) {
        var owned: AndroidFeedVerificationSession? = null
        var profileId: String? = null
        try {
            if (!webViewProfiles.isSupported()) {
                coordinator.failed(active.id, VerificationFailure.Unsupported)
                return@LaunchedEffect
            }
            profileCleaner.clean()
            val profile = repository.acquireProfile(
                epoch = active.accountEpoch,
                origin = checkNotNull(RecoveryOrigin.of(active.url)),
                userAgent = webViewProfiles.defaultUserAgent(),
                engineVersion = webViewProfiles.engineVersion(),
            )
            profileId = profile.id
            coordinator.attachProfile(active.sessionId, profile)
            owned = AndroidFeedVerificationSession(
                context = context,
                metadata = profile,
                onNavigation = {
                    pageLoaded = false
                    pageState = VerificationPageState.Loading
                    captureJob?.cancel()
                    if (coordinator.visibleState.value?.validating == true) {
                        scope.launch { coordinator.browserStopped(active.id) }
                    }
                },
                onPageReady = { pageLoaded = true },
                onRendererStopped = {
                    rendererStopped = true
                    captureJob?.cancel()
                    scope.launch { coordinator.browserStopped(active.id) }
                },
            )
            session = owned
            awaitCancellation()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            coordinator.failed(active.id, VerificationFailure.Unsupported)
        } finally {
            withContext(NonCancellable) {
                coordinator.cancelSession(active.sessionId)
                captureJob?.cancelAndJoin()
                session = null
                owned?.close()
                profileId?.let { repository.retireOrphan(it) }
                profileCleaner.clean()
            }
        }
    }
    SideEffect(active.id, session) {
        pageLoaded = false
        pageState = VerificationPageState.Loading
        session?.open(active.url)
    }
    LaunchedEffect(active.id, pageLoaded, session) {
        if (active.canCapturePage(pageLoaded, session)) {
            captureJob?.cancelAndJoin()
            pageState = VerificationPageState.Checking
            captureJob = scope.launch {
                pageState = captureVerification(active, session, repository, coordinator)
            }
        }
    }
    // Checks that finish without a page navigation are picked up here; the browser stays visible
    // and a still-challenged or failed attempt leaves the screen as it is.
    LaunchedEffect(active.id, pageState, session) {
        if (pageState != VerificationPageState.Waiting) return@LaunchedEffect
        while (true) {
            delay(RECHECK_INTERVAL_MILLIS)
            if (captureJob?.isActive != true && active.canCapturePage(pageLoaded, session)) {
                captureJob = scope.launch {
                    val result = captureVerification(active, session, repository, coordinator)
                    if (result == VerificationPageState.Checking) pageState = result
                }
            }
        }
    }
    LaunchedEffect(active.sessionId) {
        delay(RecoveryPolicy.VERIFICATION_TIMEOUT_MILLIS)
        coordinator.cancelAll()
    }

    VerificationDialog(
        active = active,
        session = session,
        rendererStopped = rendererStopped,
        pageState = pageState,
        onCancel = { scope.launch { coordinator.cancelAll() } },
        onRead = {
            if (captureJob?.isActive != true) {
                pageState = VerificationPageState.Checking
                captureJob = scope.launch {
                    pageState = captureVerification(active, session, repository, coordinator)
                }
            }
        },
    )
}

@Composable
private fun VerificationScreenEffects(coordinator: FeedRecoveryCoordinator, epoch: String) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val strings = LocalFeedFlowStrings.current
    LaunchedEffect(coordinator) {
        coordinator.failures.collect { reason ->
            val message = reason.message(strings)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(epoch) {
        coordinator.cancelAll()
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) scope.launch { coordinator.cancelAll() }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun VerificationDialog(
    active: WebsiteVerificationRequest,
    session: AndroidFeedVerificationSession?,
    rendererStopped: Boolean,
    pageState: VerificationPageState,
    onCancel: () -> Unit,
    onRead: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        VerificationSystemBars()
        val state = when {
            rendererStopped || active.failure != null -> VerificationPageState.Failed
            active.validating -> VerificationPageState.Checking
            else -> pageState
        }
        WebsiteVerificationContent(
            website = verificationWebsiteName(active.url),
            state = state,
            // Only after a failed check: while the challenge is on screen the check repeats on its own.
            canRetry = session != null && !rendererStopped && !active.validating && active.failure == null &&
                state == VerificationPageState.Failed,
            onCancel = onCancel,
            onRetry = onRead,
            browser = { modifier ->
                session?.let { owned ->
                    AndroidView(
                        // The browser leaves composition while checking, so detach it from the old holder
                        factory = { owned.webView.also { (it.parent as? ViewGroup)?.removeView(it) } },
                        modifier = modifier,
                    )
                }
            },
        )
    }
}

@Composable
private fun VerificationSystemBars() {
    val view = LocalView.current
    val lightBackground = MaterialTheme.colorScheme.surface.luminance() > LIGHT_BACKGROUND_LUMINANCE
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightBackground
            controller.isAppearanceLightNavigationBars = lightBackground
        }
    }
}

private suspend fun captureVerification(
    active: WebsiteVerificationRequest,
    session: AndroidFeedVerificationSession?,
    repository: FeedRecoveryRepository,
    coordinator: FeedRecoveryCoordinator,
): VerificationPageState =
    try {
        val owned = session ?: error("Browser unavailable")
        if (!repository.isCurrent(owned.metadata)) error("Browser profile retired")
        val response = owned.capture(active.url)
        if (!coordinator.startRead(active.id)) return VerificationPageState.Failed
        coordinator.captured(active.id, response, owned.metadata)
        VerificationPageState.Checking
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: BrowserTransferFailure) {
        if (failure.kind == "challenge") VerificationPageState.Waiting else VerificationPageState.Failed
    } catch (_: Exception) {
        VerificationPageState.Failed
    }

/** The browser can stop after a retry replaced the request, so fail whichever one is on screen. */
private suspend fun FeedRecoveryCoordinator.browserStopped(fallbackId: String) {
    failed(visibleState.value?.id ?: fallbackId, VerificationFailure.BrowserStopped)
}

private fun WebsiteVerificationRequest.canCapturePage(
    loaded: Boolean,
    session: AndroidFeedVerificationSession?,
): Boolean =
    loaded && session != null && !validating && failure == null

private fun VerificationFailure.message(strings: FeedFlowStrings): String = when (this) {
    VerificationFailure.SaveFailed -> strings.feedVerificationSaveFailed
    VerificationFailure.BrowserStopped -> strings.feedVerificationBrowserFailed
    VerificationFailure.Unsupported -> strings.feedVerificationUnsupported
    else -> strings.feedVerificationFailed
}

private const val LIGHT_BACKGROUND_LUMINANCE = 0.5f
private const val RECHECK_INTERVAL_MILLIS = 4_000L
