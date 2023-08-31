package com.prof18.feedflow.addfeed

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prof18.feedflow.desktopViewModel
import com.prof18.feedflow.di.DI
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.ui.addfeed.AddFeedsContent
import com.prof18.feedflow.ui.style.FeedFlowTheme

@Composable
fun AddFeedScreen(
    onFeedAdded: () -> Unit,
) {
    var feedName by remember { mutableStateOf("") }
    var feedUrl by remember { mutableStateOf("") }

    val viewModel = desktopViewModel { DI.koin.get<AddFeedViewModel>() }

    val isAddDone by viewModel.isAddDoneState.collectAsState()
    val isInvalidUrl by viewModel.isInvalidRssFeed.collectAsState()

    if (isAddDone) {
        feedName = ""
        feedUrl = ""
        onFeedAdded()
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
    )
}

@Composable
private fun AddFeedScreenContent(
    feedName: String,
    feedUrl: String,
    isInvalidUrl: Boolean,
    onFeedNameUpdated: (String) -> Unit,
    onFeedUrlUpdated: (String) -> Unit,
    addFeed: () -> Unit,
) {
    Scaffold { paddingValues ->
        AddFeedsContent(
            paddingValues = paddingValues,
            feedName = feedName,
            isInvalidUrl = isInvalidUrl,
            onFeedNameUpdated = onFeedNameUpdated,
            feedUrl = feedUrl,
            onFeedUrlUpdated = onFeedUrlUpdated,
            addFeed = addFeed,
        )
    }
}

@Preview
@Composable
private fun AddScreenContentPreview() {
    FeedFlowTheme {
        AddFeedScreenContent(
            feedName = "My Feed",
            feedUrl = "https://www.ablog.com/feed",
            isInvalidUrl = false,
            onFeedNameUpdated = {},
            onFeedUrlUpdated = {},
            addFeed = { },
        )
    }
}

@Preview
@Composable
private fun AddScreenContentDarkPreview() {
    FeedFlowTheme(
        darkTheme = true,
    ) {
        AddFeedScreenContent(
            feedName = "My Feed",
            feedUrl = "https://www.ablog.com/feed",
            isInvalidUrl = false,
            onFeedNameUpdated = {},
            onFeedUrlUpdated = {},
            addFeed = { },
        )
    }
}

@Preview
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
        )
    }
}
