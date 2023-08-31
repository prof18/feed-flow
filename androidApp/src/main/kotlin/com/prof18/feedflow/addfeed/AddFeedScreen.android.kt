@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow.addfeed

import FeedFlowTheme
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.MR
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.ui.addfeed.AddFeedsContent
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddFeedScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<AddFeedViewModel>()
    var feedName by remember { mutableStateOf("") }
    var feedUrl by remember { mutableStateOf("") }

    val context = LocalContext.current
    val isAddDone by viewModel.isAddDoneState.collectAsStateWithLifecycle()
    val isInvalidUrl by viewModel.isInvalidRssFeed.collectAsStateWithLifecycle()

    if (isAddDone) {
        feedName = ""
        feedUrl = ""
        val message = stringResource(resource = MR.strings.feed_added_message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT)
            .show()
    }

    AddFeedScreenContent(
        feedName = feedName,
        feedUrl = feedUrl,
        isInvalidUrl = isInvalidUrl,
        onFeedNameUpdated = { name ->
            feedName = name
            viewModel.updateFeedNameTextFieldValue(name)
        },
        onFeedUrlUpdated = { url ->
            feedUrl = url
            viewModel.updateFeedUrlTextFieldValue(url)
        },
        addFeed = {
            viewModel.addFeed()
        },
        navigateBack = navigateBack,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddFeedScreenContent(
    feedName: String,
    feedUrl: String,
    isInvalidUrl: Boolean,
    onFeedNameUpdated: (String) -> Unit,
    onFeedUrlUpdated: (String) -> Unit,
    addFeed: () -> Unit,
    navigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(resource = MR.strings.add_feed))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigateBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AddFeedsContent(
            paddingValues = paddingValues,
            feedName = feedName,
            onFeedNameUpdated = onFeedNameUpdated,
            feedUrl = feedUrl,
            isInvalidUrl = isInvalidUrl,
            onFeedUrlUpdated = onFeedUrlUpdated,
            addFeed = addFeed,
        )
    }
}

@FeedFlowPreview
@Composable
private fun AddScreenContentPreview() {
    FeedFlowTheme {
        AddFeedScreenContent(
            feedName = "My Feed",
            feedUrl = "https://www.ablog.com/feed",
            onFeedNameUpdated = {},
            onFeedUrlUpdated = {},
            addFeed = { },
            isInvalidUrl = false,
            navigateBack = {},
        )
    }
}

@FeedFlowPreview
@Composable
private fun AddScreenContentInvalidUrlPreview() {
    FeedFlowTheme {
        AddFeedScreenContent(
            feedName = "My Feed",
            feedUrl = "https://www.ablog.com/feed",
            isInvalidUrl = true,
            onFeedNameUpdated = {},
            onFeedUrlUpdated = {},
            addFeed = { },
            navigateBack = {},
        )
    }
}
