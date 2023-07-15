package com.prof18.feedflow.addfeed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.MR
import com.prof18.feedflow.koin
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

val viewModel = koin.get<AddFeedViewModel>()


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedScreen(
    onFeedAdded: () -> Unit,
) {
    var feedName by remember { mutableStateOf("") }
    var feedUrl by remember { mutableStateOf("") }

    val isAddDone by viewModel.isAddDoneState.collectAsState()

    if (isAddDone) {
        feedName = ""
        feedUrl = ""
        onFeedAdded()
    }

    Scaffold(
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = Spacing.regular)
        ) {

            TextField(
                modifier = Modifier
                    .fillMaxWidth(),
                label = {
                    Text(
                        text = stringResource(resource = MR.strings.feed_name)
                    )
                },
                value = feedName,
                onValueChange = {
                    feedName = it
                    viewModel.updateFeedNameTextFieldValue(it)
                },
                placeholder = {
                    Text(
                        stringResource(resource = MR.strings.feed_name_placeholder),
                        maxLines = 1,
                    )
                },
                maxLines = 1,
            )



            TextField(
                modifier = Modifier
                    .padding(top = Spacing.regular)
                    .fillMaxWidth(),
                label = {
                    Text(
                        text = stringResource(resource = MR.strings.feed_url)
                    )
                },
                value = feedUrl,
                onValueChange = {
                    feedUrl = it
                    viewModel.updateFeedUrlTextFieldValue(it)
                },
                placeholder = {
                    Text(
                        stringResource(resource = MR.strings.feed_url_placeholder),
                        maxLines = 1,
                    )
                },
                maxLines = 1,
            )

            Button(
                modifier = Modifier
                    .padding(top = Spacing.regular)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    viewModel.addFeed()
                },
            ) {
                Text(
                    stringResource(resource = MR.strings.add_feed)
                )
            }
        }
    }
}