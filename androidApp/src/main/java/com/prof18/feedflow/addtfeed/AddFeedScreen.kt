package com.prof18.feedflow.addtfeed

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedScreen() {

    val viewModel = koinViewModel<AddFeedViewModel>()

    val context = LocalContext.current
    val isAddDone by viewModel.isAddDoneState.collectAsStateWithLifecycle()

    if (isAddDone) {
        Toast.makeText(context, "Feed Added", Toast.LENGTH_SHORT)
            .show()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Feed") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = Spacing.regular)
        ) {

            TextField(
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text(text = "Feed name") },
                value = viewModel.feedNameState.value,
                onValueChange = { viewModel.updateFeedNameTextFieldValue(it) },
                placeholder = {
                    Text(
                        "My Favourite Blog",
                        maxLines = 1,
                    )
                },
                maxLines = 1,
            )

            TextField(
                modifier = Modifier
                    .padding(top = Spacing.regular)
                    .fillMaxWidth(),
                label = { Text(text = "Feed url") },
                value = viewModel.feedUrlState.value,
                onValueChange = { viewModel.updateFeedUrlTextFieldValue(it) },
                placeholder = {
                    Text(
                        "https://myfavouriteblog.com/feed",
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
                Text("Add Feed")
            }

        }
    }

}