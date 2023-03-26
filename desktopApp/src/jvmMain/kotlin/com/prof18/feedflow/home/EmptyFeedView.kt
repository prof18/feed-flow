package com.prof18.feedflow.home

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.ui.style.FeedFlowTheme
import com.prof18.feedflow.ui.style.Spacing

@Composable
internal fun EmptyFeedView(
    modifier: Modifier = Modifier,
    onReloadClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nothing else to read here!",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = {
                onReloadClick()
            }
        ) {
            Text("Refresh feeds")
        }
    }
}

@Preview
@Composable
fun EmptyFeedViewPreview() {
    FeedFlowTheme {
        Surface {
            NoFeedsSourceView {}
        }
    }
}