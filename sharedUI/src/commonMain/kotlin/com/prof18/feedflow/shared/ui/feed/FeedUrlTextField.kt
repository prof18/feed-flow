package com.prof18.feedflow.shared.ui.feed

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

// TODO: maybe revert
@Composable
fun FeedUrlTextField(
    feedUrl: String,
    showError: Boolean,
    errorMessage: String,
    onFeedUrlUpdated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        label = {
            Text(text = LocalFeedFlowStrings.current.feedUrl)
        },
        isError = showError,
        supportingText = if (showError) {
            {
                Text(
                    text = errorMessage,
                )
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        value = feedUrl,
        onValueChange = {
            onFeedUrlUpdated(it)
        },
        placeholder = {
            Text(
                LocalFeedFlowStrings.current.feedUrlPlaceholder,
                maxLines = 1,
            )
        },
        maxLines = 1,
    )
}
