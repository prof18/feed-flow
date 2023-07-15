package com.prof18.feedflow.addtfeed

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.MR
import com.prof18.feedflow.presentation.AddFeedViewModel
import com.prof18.feedflow.ui.theme.Spacing
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
                }
            )
        }
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
                    Text(text = stringResource(resource = MR.strings.feed_name))
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
                    Text(text = stringResource(resource = MR.strings.feed_url))
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
                Text(stringResource(resource = MR.strings.add_feed))
            }
        }
    }
}