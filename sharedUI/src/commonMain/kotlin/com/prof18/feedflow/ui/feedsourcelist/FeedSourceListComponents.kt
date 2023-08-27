package com.prof18.feedflow.ui.feedsourcelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun FeedSourcesList(
    modifier: Modifier = Modifier,
    feedSourceItemClickModifier: Modifier,
    feedSources: List<FeedSource>,
    showFeedMenu: Boolean,
    hideFeedMenu: () -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.regular),
    ) {
        items(
            items = feedSources,
        ) { feedSource ->

            Column(
                modifier = Modifier
                    .then(feedSourceItemClickModifier),
            ) {
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
                    style = MaterialTheme.typography.labelLarge,
                )

                FeedSourceContextMenu(
                    showFeedMenu = showFeedMenu,
                    hideMenu = hideFeedMenu,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                    feedSource = feedSource,
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

@Composable
fun FeedSourceContextMenu(
    showFeedMenu: Boolean,
    hideMenu: () -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    feedSource: FeedSource,
) {
    DropdownMenu(
        expanded = showFeedMenu,
        onDismissRequest = hideMenu,
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(resource = MR.strings.delete_feed),
                )
            },
            onClick = {
                onDeleteFeedSourceClick(feedSource)
                hideMenu()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSourceNavBar(
    navigateBack: () -> Unit,
    onAddFeedSourceClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(stringResource(resource = MR.strings.feeds_title))
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
        },
        actions = {
            IconButton(
                onClick = {
                    onAddFeedSourceClick()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
fun NoFeedSourcesView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier
                .padding(Spacing.regular),
            text = stringResource(resource = MR.strings.no_feeds_add_one_message),
        )
    }
}
