package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting

@Composable
fun NoFeedsSourceView(
    modifier: Modifier = Modifier,
    onAddFeedClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = LocalFeedFlowStrings.current.noFeedsFoundMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular)
                .tagForTesting(TestingTag.HOME_SCREEN_ADD_FEED_BUTTON),
            onClick = {
                onAddFeedClick()
            },
        ) {
            Text(LocalFeedFlowStrings.current.addFeed)
        }
    }
}
