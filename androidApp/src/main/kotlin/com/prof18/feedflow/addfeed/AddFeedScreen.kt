package com.prof18.feedflow.addfeed

import FeedFlowTheme
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.MR
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.theme.Spacing
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = Spacing.regular),
        ) {
            FeedNameTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                feedName = feedName,
                onFeedNameUpdated = onFeedNameUpdated,
            )

            FeedUrlTextField(
                modifier = Modifier
                    .padding(top = Spacing.regular)
                    .fillMaxWidth(),
                feedUrl = feedUrl,
                onFeedUrlUpdated = onFeedUrlUpdated,
            )

            Button(
                modifier = Modifier
                    .padding(top = Spacing.regular)
                    .align(Alignment.CenterHorizontally),
                onClick = addFeed,
            ) {
                Text(stringResource(resource = MR.strings.add_feed))
            }
        }
    }
}

@Composable
private fun FeedNameTextField(
    modifier: Modifier = Modifier,
    feedName: String,
    onFeedNameUpdated: (String) -> Unit,
) {
    TextField(
        modifier = modifier,
        label = {
            Text(text = stringResource(resource = MR.strings.feed_name))
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        value = feedName,
        onValueChange = {
            onFeedNameUpdated(it)
        },
        placeholder = {
            Text(
                stringResource(resource = MR.strings.feed_name_placeholder),
                maxLines = 1,
            )
        },
        maxLines = 1,
    )
}

@Composable
private fun FeedUrlTextField(
    modifier: Modifier = Modifier,
    feedUrl: String,
    onFeedUrlUpdated: (String) -> Unit,
) {
    TextField(
        modifier = modifier,
        label = {
            Text(text = stringResource(resource = MR.strings.feed_url))
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Uri,
            autoCorrect = false,
        ),
        value = feedUrl,
        onValueChange = {
            onFeedUrlUpdated(it)
        },
        placeholder = {
            Text(
                stringResource(resource = MR.strings.feed_url_placeholder),
                maxLines = 1,
            )
        },
        maxLines = 1,
    )
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
            navigateBack = {},
        )
    }
}
