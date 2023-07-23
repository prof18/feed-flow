package com.prof18.feedflow.feedsourcelist

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.MR
import com.prof18.feedflow.addfeed.AddFeedScreen
import com.prof18.feedflow.desktopViewModel
import com.prof18.feedflow.di.DI
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.preview.feedSourcesForPreview
import com.prof18.feedflow.ui.style.FeedFlowTheme
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun FeedSourceListScreen(
    navigateBack: () -> Unit,
) {
    FeedFlowTheme {
        var dialogState by remember { mutableStateOf(false) }

        Dialog(
            title = stringResource(MR.strings.add_feed),
            visible = dialogState,
            onCloseRequest = { dialogState = false },
        ) {
            AddFeedScreen(
                onFeedAdded = {
                    dialogState = false
                },
            )
        }
        val viewModel = desktopViewModel { DI.koin.get<FeedSourceListViewModel>() }

        val feedSources by viewModel.feedSourcesState.collectAsState()

        FeedSourceListContent(
            feedSources = feedSources,
            onAddFeedClick = {
                dialogState = true
            },
            onDeleteFeedClick = { feedSource ->
                viewModel.deleteFeedSource(feedSource)
            },
            navigateBack = navigateBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FeedSourceListContent(
    feedSources: List<FeedSource>,
    onAddFeedClick: () -> Unit,
    onDeleteFeedClick: (FeedSource) -> Unit,
    navigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            FeedSourceListNavBar(
                navigateBack = navigateBack,
                onAddFeedClick = onAddFeedClick,
            )
        },
    ) { paddingValues ->
        if (feedSources.isEmpty()) {
            NoFeedSourcesView(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            )
        } else {
            FeedSourcesList(
                modifier = Modifier
                    .padding(paddingValues),
                feedSources = feedSources,
                onDeleteFeedClick = onDeleteFeedClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedSourceListNavBar(
    navigateBack: () -> Unit,
    onAddFeedClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(resource = MR.strings.feeds_title),
            )
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
                    onAddFeedClick()
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
private fun NoFeedSourcesView(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedSourcesList(
    modifier: Modifier = Modifier,
    feedSources: List<FeedSource>,
    onDeleteFeedClick: (FeedSource) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.regular),
    ) {
        items(
            items = feedSources,
        ) { feedSource ->

            var showFeedMenu by remember {
                mutableStateOf(
                    false,
                )
            }

            val interactionSource = remember { MutableInteractionSource() }

            Column(
                modifier = Modifier
                    .onClick(
                        enabled = true,
                        interactionSource = interactionSource,
                        matcher = PointerMatcher.mouse(PointerButton.Secondary), // Right Mouse Button
                        onClick = {
                            showFeedMenu = true
                        },
                    ),
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
                    hideMenu = {
                        showFeedMenu = false
                    },
                    onDeleteFeedClick = onDeleteFeedClick,
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
private fun FeedSourceContextMenu(
    showFeedMenu: Boolean,
    hideMenu: () -> Unit,
    onDeleteFeedClick: (FeedSource) -> Unit,
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
                hideMenu()
                onDeleteFeedClick(feedSource)
            },
        )
    }
}

@Preview
@Composable
private fun FeedSourceListContentPreview() {
    FeedFlowTheme {
        FeedSourceListContent(
            feedSources = feedSourcesForPreview,
            onAddFeedClick = {},
            onDeleteFeedClick = {},
            navigateBack = {},
        )
    }
}

@Preview
@Composable
private fun FeedSourceListContentDarkPreview() {
    FeedFlowTheme(
        darkTheme = true,
    ) {
        FeedSourceListContent(
            feedSources = feedSourcesForPreview,
            onAddFeedClick = {},
            onDeleteFeedClick = {},
            navigateBack = {},
        )
    }
}
