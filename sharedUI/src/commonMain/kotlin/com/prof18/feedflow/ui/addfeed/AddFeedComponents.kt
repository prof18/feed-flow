@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow.ui.addfeed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.prof18.feedflow.MR
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AddFeedsContent(
    paddingValues: PaddingValues,
    feedUrl: String,
    showError: Boolean,
    errorMessage: String,
    onFeedUrlUpdated: (String) -> Unit,
    addFeed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = Spacing.regular),
    ) {
        FeedUrlTextField(
            modifier = Modifier
                .padding(top = Spacing.regular)
                .fillMaxWidth(),
            feedUrl = feedUrl,
            showError = showError,
            errorMessage = errorMessage,
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

@Composable
private fun FeedUrlTextField(
    modifier: Modifier = Modifier,
    feedUrl: String,
    showError: Boolean,
    errorMessage: String,
    onFeedUrlUpdated: (String) -> Unit,
) {
    TextField(
        modifier = modifier,
        label = {
            Text(text = stringResource(resource = MR.strings.feed_url))
        },
        isError = showError,
        supportingText = if (showError) {
            {
                Text(errorMessage)
            }
        } else {
            null
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
