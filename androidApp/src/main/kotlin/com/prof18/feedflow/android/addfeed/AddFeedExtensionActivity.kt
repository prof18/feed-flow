package com.prof18.feedflow.android.addfeed

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.time.Duration.Companion.seconds

class AddFeedExtensionActivity : BaseThemeActivity() {
    val viewModel by viewModel<AddFeedViewModel>()

    private var sharedUrl: String? = null

    /** Starts adding the shared feed; false when the share holds no link to add. */
    private fun startSharedFeedRequest(): Boolean {
        if (intent?.action != Intent.ACTION_SEND) return false
        val url = sharedFeedUrl(
            text = intent.getStringExtra(Intent.EXTRA_TEXT),
            html = intent.getStringExtra(Intent.EXTRA_HTML_TEXT),
        ) ?: return false
        sharedUrl = url
        addFeed(url)
        return true
    }

    @Composable
    override fun Content() {
        var result by remember { mutableStateOf<ShareResult?>(null) }
        val strings = LocalFeedFlowStrings.current

        LaunchedEffect(Unit) {
            observeShareFeedResults(
                results = viewModel.feedAddedState,
                startRequest = {
                    if (!startSharedFeedRequest()) {
                        result = ShareResult(strings.shareNoFeedLinkFound, isError = true)
                    }
                },
            ).collect { feedAddedState ->
                when (feedAddedState) {
                    is FeedAddedState.Error -> {
                        val message = when (feedAddedState) {
                            is FeedAddedState.Error.InvalidUrl -> strings.invalidRssUrl
                            is FeedAddedState.Error.InvalidTitleLink -> strings.missingTitleAndLink
                            is FeedAddedState.Error.GenericError -> strings.addFeedGenericError
                            is FeedAddedState.Error.FetchFailed ->
                                strings.feedFetchRefusedError
                        }
                        result = ShareResult(message, isError = true)
                    }

                    is FeedAddedState.FeedAdded, is FeedAddedState.FeedAlreadyExists -> {
                        val feedName = (feedAddedState as? FeedAddedState.FeedAdded)?.feedName
                        val message = if (feedAddedState is FeedAddedState.FeedAlreadyExists) {
                            strings.feedAlreadyExistsMessage(feedAddedState.feedName)
                        } else if (feedName != null) {
                            strings.feedAddedMessage(feedName)
                        } else {
                            strings.feedAddedMessageWithoutName
                        }
                        result = ShareResult(message, isError = false)
                        delay(SHARE_SUCCESS_DELAY)
                        finish()
                    }

                    // AddFeedViewModel emits FeedNotAdded every time the URL changes, right before each add.
                    FeedAddedState.FeedNotAdded -> {
                    }

                    FeedAddedState.Loading -> {
                        result = null
                    }
                }
            }
        }

        // The floating window spans the screen: center the card and dismiss on taps around it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = ::finish),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    // Taps on the card must not reach the dismissing scrim behind it.
                    .pointerInput(Unit) { detectTapGestures { } },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    Modifier.padding(
                        start = Spacing.medium,
                        top = Spacing.medium,
                        end = Spacing.medium,
                        // TextButtons carry their own touch padding, so the action row needs less below it.
                        bottom = if (result?.isError == true) Spacing.small else Spacing.medium,
                    ),
                ) {
                    val currentResult = result
                    if (currentResult == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(32.dp))
                            Spacer(Modifier.width(Spacing.regular))
                            Text(
                                text = strings.addingFeed,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        ShareResultContent(
                            result = currentResult,
                            openInFeedFlowLink = sharedUrl?.let(::openInFeedFlowLink),
                            onDone = ::finish,
                            onOpenInFeedFlow = ::openInFeedFlow,
                        )
                    }
                }
            }
        }
    }

    private fun addFeed(url: String) {
        viewModel.updateFeedUrlTextFieldValue(url)
        viewModel.addFeed()
    }

    private fun openInFeedFlow(link: String) {
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
            .setPackage(packageName)
            // Opens MainActivity in FeedFlow's own task, not in the task of the app that shared.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}

@Composable
private fun ShareResultContent(
    result: ShareResult,
    openInFeedFlowLink: String?,
    onDone: () -> Unit,
    onOpenInFeedFlow: (String) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Column {
        Text(
            text = result.message,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (result.isError) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.End),
            ) {
                TextButton(onClick = onDone) {
                    Text(strings.actionDone)
                }
                if (openInFeedFlowLink != null) {
                    TextButton(onClick = { onOpenInFeedFlow(openInFeedFlowLink) }) {
                        Text(strings.shareOpenInFeedflow)
                    }
                }
            }
        }
    }
}

private val SHARE_SUCCESS_DELAY = 4.seconds

private data class ShareResult(val message: String, val isError: Boolean)
