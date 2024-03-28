package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import kotlinx.collections.immutable.ImmutableList

internal expect fun Modifier.feedSourceMenuClickModifier(
    onClick: () -> Unit = {},
    onLongClick: () -> Unit,
): Modifier

@Composable
internal fun FeedSourcesWithCategoryList(
    feedSourceState: FeedSourceListState,
    feedSourceImage: @Composable (String) -> Unit,
    onExpandClicked: (CategoryId?) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.regular),
    ) {
        item {
            FeedSourcesList(
                feedSources = feedSourceState.feedSourcesWithoutCategory,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                feedSourceImage = feedSourceImage,
            )
        }

        items(feedSourceState.feedSourcesWithCategory) { feedSourceState ->
            Column(
                modifier = Modifier
                    .tagForTesting(TestingTag.FEED_SOURCE_SELECTOR),
            ) {
                @Suppress("MagicNumber")
                val degrees by animateFloatAsState(
                    if (feedSourceState.isExpanded) {
                        -90f
                    } else {
                        90f
                    },
                )
                Row(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            onExpandClicked(feedSourceState.categoryId)
                        }
                        .fillMaxWidth()
                        .padding(vertical = Spacing.regular),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val headerText = if (feedSourceState.categoryName != null) {
                        requireNotNull(feedSourceState.categoryName)
                    } else {
                        LocalFeedFlowStrings.current.noCategory
                    }

                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.rotate(degrees),
                    )
                }

                FeedSourcesListWithCategorySelector(
                    feedSourceState = feedSourceState,
                    feedSourceImage = feedSourceImage,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                )
            }
        }
    }
}

@Composable
private fun FeedSourcesListWithCategorySelector(
    feedSourceState: FeedSourceState,
    feedSourceImage: @Composable (String) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
) {
    AnimatedVisibility(
        visible = feedSourceState.isExpanded,
        enter = expandVertically(
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
        ),
        exit = shrinkVertically(),
    ) {
        FeedSourcesList(
            feedSources = feedSourceState.feedSources,
            onDeleteFeedSourceClick = onDeleteFeedSourceClick,
            feedSourceImage = feedSourceImage,
        )
    }
}

@Composable
private fun FeedSourcesList(
    feedSources: ImmutableList<FeedSource>,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    feedSourceImage: @Composable (String) -> Unit,
) {
    Column {
        feedSources.forEachIndexed { index, feedSource ->
            FeedSourceItem(
                feedSource = feedSource,
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                feedSourceImage = feedSourceImage,
            )

            if (index < feedSources.size - 1) {
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
private fun FeedSourceItem(
    feedSource: FeedSource,
    feedSourceImage: @Composable (String) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
) {
    var showFeedMenu by remember {
        mutableStateOf(
            false,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .feedSourceMenuClickModifier(
                onLongClick = {
                    showFeedMenu = true
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val imageUrl = feedSource.logoUrl
        if (imageUrl != null) {
            feedSourceImage(imageUrl)
        } else {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .padding(start = Spacing.regular),
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
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                feedSource = feedSource,
            )
        }
    }
}

@Composable
private fun FeedSourceContextMenu(
    showFeedMenu: Boolean,
    hideMenu: () -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    feedSource: FeedSource,
) {
    DropdownMenu(
        expanded = showFeedMenu,
        onDismissRequest = hideMenu,
        properties = PopupProperties(),
    ) {
        DropdownMenuItem(
            modifier = Modifier
                .tagForTesting(TestingTag.FEED_SOURCE_DELETE_BUTTON),
            text = {
                Text(
                    LocalFeedFlowStrings.current.deleteFeed,
                )
            },
            onClick = {
                onDeleteFeedSourceClick(feedSource)
                hideMenu()
            },
        )
    }
}

@Composable
internal fun FeedSourceNavBar(
    navigateBack: () -> Unit,
    onAddFeedSourceClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(LocalFeedFlowStrings.current.feedsTitle)
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .tagForTesting(TestingTag.BACK_BUTTON_FEED_SOURCES),
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
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
internal fun NoFeedSourcesView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier
                .padding(Spacing.regular)
                .tagForTesting(TestingTag.NO_FEED_SOURCE_MESSAGE),
            text = LocalFeedFlowStrings.current.noFeedsAddOneMessage,
        )
    }
}
