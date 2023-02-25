package com.prof18.feedflow.feedlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen() {

    val viewModel = koinViewModel<FeedSourceListViewModel>()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Feeds") }) }
    ) { paddingValues ->

        val feeds by viewModel.feedsState.collectAsStateWithLifecycle()

        if (feeds.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Text(
                    modifier = Modifier
                        .padding(Spacing.regular),
                    text = "No feeds, please add one",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues),
                contentPadding = PaddingValues(Spacing.regular),
            ) {
                items(
                    items = feeds) { feedSource ->
                    Text(
                        modifier = Modifier
                            .padding(top = Spacing.small),
                        text = feedSource.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = Spacing.xsmall)
                            .padding(bottom = Spacing.small),
                        text = feedSource.url,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Divider(
                        modifier = Modifier,
                        thickness = 0.2.dp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }

}