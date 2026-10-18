package com.prof18.feedflow.android.feedverification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebsiteVerificationContent(
    website: String,
    state: VerificationPageState,
    canRetry: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    browser: @Composable (Modifier) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Scaffold(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }
            .testTag(FeedVerificationE2eIds.SCREEN),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.feedVerificationScreenTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            website,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag(FeedVerificationE2eIds.CANCEL)) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancelButton)
                    }
                },
            )
        },
        bottomBar = {
            if (state == VerificationPageState.Waiting || canRetry) {
                VerificationFooter(state, canRetry, onRetry)
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state == VerificationPageState.Waiting) {
                Surface(
                    modifier = Modifier.widthIn(max = 960.dp).fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) { browser(Modifier.fillMaxSize().testTag(FeedVerificationE2eIds.WEB_CONTENT)) }
            } else {
                VerificationProgress(state)
            }
        }
    }
}

@Composable
private fun VerificationFooter(state: VerificationPageState, canRetry: Boolean, onRetry: () -> Unit) {
    val strings = LocalFeedFlowStrings.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state == VerificationPageState.Waiting) {
                Text(
                    strings.feedVerificationAutomaticHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (canRetry) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                        .testTag(FeedVerificationE2eIds.CONTINUE),
                ) {
                    Text(strings.feedVerificationCheckAgain)
                }
            }
        }
    }
}

@Composable
private fun VerificationProgress(state: VerificationPageState) {
    val strings = LocalFeedFlowStrings.current
    Column(
        modifier = Modifier.widthIn(max = 400.dp).padding(24.dp).testTag(FeedVerificationE2eIds.CHECKING),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(20.dp).size(32.dp),
            )
        }
        Text(
            strings.feedVerificationCheckingTitle,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            if (state == VerificationPageState.Failed) {
                strings.feedVerificationRetryBody
            } else {
                strings.feedVerificationCheckingBody
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state != VerificationPageState.Failed) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

internal enum class VerificationPageState { Loading, Waiting, Checking, Failed }
