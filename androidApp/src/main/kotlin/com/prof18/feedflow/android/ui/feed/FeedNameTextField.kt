package com.prof18.feedflow.android.ui.feed

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedNameTextField(
    feedName: String,
    onFeedNameUpdated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        label = {
            Text(text = LocalFeedFlowStrings.current.feedName)
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
                LocalFeedFlowStrings.current.feedName,
                maxLines = 1,
            )
        },
        maxLines = 1,
    )
}
