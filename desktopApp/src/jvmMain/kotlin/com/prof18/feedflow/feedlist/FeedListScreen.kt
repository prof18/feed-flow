package com.prof18.feedflow.feedlist

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun FeedListScreen(
    navigateBack: () -> Unit,
) {
    Column {
        Text("FeedList")
        Button(
            onClick = { navigateBack() }
        ) {
            Text("Back")
        }
    }

}