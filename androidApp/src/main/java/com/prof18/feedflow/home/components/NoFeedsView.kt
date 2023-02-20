package com.prof18.feedflow.home.components

import FeedFlowTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.ui.preview.FeedFlowPreview

@Composable
internal fun NoFeedsView(
    modifier: Modifier = Modifier,
    onReloadClick: () -> Unit,
    onAddFeedClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No feeds found. Please add a new feed")
        Button(
            onClick = {
                onAddFeedClick()
            }
        ) {
            Text("Add feed")
        }
        Button(
            onClick = {
                onReloadClick()
            }
        ) {
            Text("Reload")
        }
    }
}

@FeedFlowPreview
@Composable
fun NoFeedsViewPreview() {
    FeedFlowTheme {
        Surface {
            NoFeedsView(onReloadClick = {}, onAddFeedClick = {})
        }
    }
}