package com.prof18.feedflow.android.addfeed

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.shared.domain.model.FeedAddedState
import com.prof18.feedflow.shared.presentation.AddFeedViewModel
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.time.Duration.Companion.seconds

class AddFeedExtensionActivity : BaseThemeActivity() {
    val viewModel by viewModel<AddFeedViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                when {
                    intent.type?.startsWith("text/plain") == true -> {
                        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { url ->
                            addFeed(url)
                        }
                    }

                    intent.type?.startsWith("text/html") == true -> {
                        intent.getStringExtra(Intent.EXTRA_HTML_TEXT)?.let { url ->
                            addFeed(url)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() {
        var showLoading by remember { mutableStateOf(true) }
        var message by remember { mutableStateOf("") }
        val strings = LocalFeedFlowStrings.current

        LaunchedEffect(Unit) {
            viewModel.feedAddedState.collect { feedAddedState ->
                when (feedAddedState) {
                    is FeedAddedState.Error -> {
                        showLoading = false
                        message = when (feedAddedState) {
                            FeedAddedState.Error.InvalidUrl -> strings.invalidRssUrl
                            FeedAddedState.Error.InvalidTitleLink -> strings.missingTitleAndLink
                            FeedAddedState.Error.GenericError -> strings.addFeedGenericError
                        }
                        delay(2.seconds)
                        finish()
                    }

                    is FeedAddedState.FeedAdded -> {
                        showLoading = false
                        val feedName = feedAddedState.feedName
                        message = if (feedName != null) {
                            strings.feedAddedMessage(feedName)
                        } else {
                            strings.feedAddedMessageWithoutName
                        }
                        delay(2.seconds)
                        finish()
                    }

                    FeedAddedState.FeedNotAdded -> {
                    }

                    FeedAddedState.Loading -> {
                        showLoading = true
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .clickable { finish() },
        ) {
            Row(
                Modifier
                    .align(Center)
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxWidth()
                    .padding(start = Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.regular),
                            text = LocalFeedFlowStrings.current.addingFeed,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.regular),
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    private fun addFeed(url: String) {
        viewModel.updateFeedUrlTextFieldValue(url)
        viewModel.addFeed()
    }
}
